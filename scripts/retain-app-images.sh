#!/usr/bin/env bash

set -euo pipefail

if [ "$#" -lt 2 ] || [ "$#" -gt 3 ]; then
  echo "Usage: $0 <image-repository> <current-image> [previous-image]" >&2
  exit 2
fi

image_repository="$1"
current_image="$2"
previous_image="${3:-}"

case "$current_image" in
  "$image_repository":*) ;;
  *)
    echo "Current image is outside the expected repository." >&2
    exit 2
    ;;
esac

if [ -n "$previous_image" ]; then
  case "$previous_image" in
    "$image_repository":*) ;;
    *)
      echo "Previous image is outside the expected repository." >&2
      exit 2
      ;;
  esac
fi

docker image inspect "$current_image" >/dev/null

while IFS= read -r image_ref; do
  if [ -z "$image_ref" ] \
      || [ "$image_ref" = "$current_image" ] \
      || [ "$image_ref" = "$previous_image" ]; then
    continue
  fi

  if ! docker image rm "$image_ref"; then
    echo "Warning: could not remove unused app image: $image_ref" >&2
  fi
done < <(
  docker images \
    --filter "reference=${image_repository}:*" \
    --format '{{.Repository}}:{{.Tag}}' \
    | sort -u
)

docker image prune -f
