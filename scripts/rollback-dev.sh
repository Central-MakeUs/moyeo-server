#!/usr/bin/env bash

set -euo pipefail

app_dir="${1:-$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)}"
env_file="${app_dir}/.env"

if [ ! -f "$env_file" ]; then
  echo "Runtime env file not found: $env_file" >&2
  exit 2
fi

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

if [ -z "$current_image" ] || [ -z "$previous_image" ]; then
  echo "Both MOYEO_IMAGE and MOYEO_PREVIOUS_IMAGE are required." >&2
  exit 2
fi

docker image inspect "$previous_image" >/dev/null

write_env_value MOYEO_IMAGE "$previous_image"
write_env_value MOYEO_PREVIOUS_IMAGE "$current_image"

cd "$app_dir"
if ! docker compose up -d --wait --wait-timeout 150; then
  echo "Rollback failed; restoring the former image selection." >&2
  write_env_value MOYEO_IMAGE "$current_image"
  write_env_value MOYEO_PREVIOUS_IMAGE "$previous_image"
  docker compose up -d --wait --wait-timeout 150 || true
  exit 1
fi

echo "Rollback completed: $previous_image"
