#!/usr/bin/env bash

set -euo pipefail

if [ "$#" -ne 3 ]; then
  echo "Usage: $0 <app-dir> <image-repository> <new-image>" >&2
  exit 2
fi

app_dir="$1"
image_repository="$2"
new_image="$3"
env_file="${app_dir}/.env"

if [ ! -f "$env_file" ]; then
  echo "Runtime env file not found: $env_file" >&2
  exit 2
fi

case "$new_image" in
  "$image_repository":*) ;;
  *)
    echo "New image is outside the expected repository." >&2
    exit 2
    ;;
esac

read_env_value() {
  local key="$1"
  sed -n "s/^${key}=//p" "$env_file" | tail -n 1
}

write_env_value() {
  local key="$1"
  local value="$2"

  if grep -q "^${key}=" "$env_file"; then
    sed -i "s|^${key}=.*|${key}=${value}|" "$env_file"
  else
    printf '%s=%s\n' "$key" "$value" >> "$env_file"
  fi
}

current_image="$(read_env_value MOYEO_IMAGE)"
previous_image="$(read_env_value MOYEO_PREVIOUS_IMAGE)"

if [ -z "$current_image" ]; then
  echo "MOYEO_IMAGE is required in $env_file." >&2
  exit 2
fi

restore_previous_deployment() {
  echo "Deployment failed; restoring the former image selection." >&2
  docker compose up -d --wait --wait-timeout 150
}

cd "$app_dir"

if ! MOYEO_IMAGE="$new_image" docker compose pull \
    || ! MOYEO_IMAGE="$new_image" docker compose up -d --wait --wait-timeout 150; then
  if restore_previous_deployment; then
    "${app_dir}/retain-app-images.sh" \
      "$image_repository" \
      "$current_image" \
      "$previous_image"
  else
    echo "Failed to restore the former healthy application." >&2
  fi
  exit 1
fi

if [ "$new_image" != "$current_image" ]; then
  write_env_value MOYEO_PREVIOUS_IMAGE "$current_image"
fi
write_env_value MOYEO_IMAGE "$new_image"

rollback_image="$(read_env_value MOYEO_PREVIOUS_IMAGE)"
"${app_dir}/retain-app-images.sh" \
  "$image_repository" \
  "$new_image" \
  "$rollback_image"

echo "Deployment completed: $new_image"
