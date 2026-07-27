#!/usr/bin/env bash

set -euo pipefail

temp_dir="$(mktemp -d)"
case "$temp_dir" in
  /tmp/*) ;;
  *)
    echo "Unexpected temporary directory: $temp_dir" >&2
    exit 1
    ;;
esac
trap 'rm -rf -- "$temp_dir"' EXIT

app_dir="${temp_dir}/app"
fake_bin="${temp_dir}/bin"
mkdir -p "$app_dir" "$fake_bin"

cat > "${fake_bin}/docker" <<'EOF'
#!/usr/bin/env bash
set -euo pipefail
printf '%s|%s|%s\n' "${1:-}" "${2:-}" "${MOYEO_IMAGE:-}" >> "${DOCKER_LOG:?}"
if [ "${ASSERT_ENV_UNCHANGED:-0}" = "1" ] \
    && [ "${1:-}" = "compose" ] \
    && ! grep -qx "MOYEO_IMAGE=example.test/moyeo:current-good" "${FAKE_ENV_FILE:?}"; then
  exit 2
fi
if [ "${FAKE_DEPLOY_FAIL:-}" = "up" ] \
    && [ "${1:-}" = "compose" ] \
    && [ "${2:-}" = "up" ] \
    && [ "${MOYEO_IMAGE:-}" = "example.test/moyeo:new-bad" ]; then
  exit 1
fi
EOF
chmod +x "${fake_bin}/docker"

cat > "${app_dir}/retain-app-images.sh" <<'EOF'
#!/usr/bin/env bash
set -euo pipefail
printf '%s\n' "$@" > "${RETAIN_ARGS_FILE:?}"
EOF
chmod +x "${app_dir}/retain-app-images.sh"

write_initial_env() {
  cat > "${app_dir}/.env" <<'EOF'
MOYEO_IMAGE=example.test/moyeo:current-good
MOYEO_PREVIOUS_IMAGE=example.test/moyeo:previous-good
EOF
}

assert_env_value() {
  local expected="$1"
  grep -qx "$expected" "${app_dir}/.env"
}

write_initial_env
PATH="${fake_bin}:${PATH}" \
RETAIN_ARGS_FILE="${temp_dir}/retain-args" \
DOCKER_LOG="${temp_dir}/docker-success.log" \
FAKE_ENV_FILE="${app_dir}/.env" \
ASSERT_ENV_UNCHANGED=1 \
  bash scripts/deploy-app-image.sh \
  "$app_dir" \
  "example.test/moyeo" \
  "example.test/moyeo:new-good"

assert_env_value "MOYEO_IMAGE=example.test/moyeo:new-good"
assert_env_value "MOYEO_PREVIOUS_IMAGE=example.test/moyeo:current-good"
grep -qx "example.test/moyeo:new-good" "${temp_dir}/retain-args"
grep -qx "example.test/moyeo:current-good" "${temp_dir}/retain-args"

write_initial_env
if PATH="${fake_bin}:${PATH}" \
    FAKE_DEPLOY_FAIL=up \
    RETAIN_ARGS_FILE="${temp_dir}/retain-args-failed" \
    DOCKER_LOG="${temp_dir}/docker-failed.log" \
    FAKE_ENV_FILE="${app_dir}/.env" \
    ASSERT_ENV_UNCHANGED=1 \
    bash scripts/deploy-app-image.sh \
    "$app_dir" \
    "example.test/moyeo" \
    "example.test/moyeo:new-bad"; then
  echo "Failed candidate deployment unexpectedly succeeded." >&2
  exit 1
fi

assert_env_value "MOYEO_IMAGE=example.test/moyeo:current-good"
assert_env_value "MOYEO_PREVIOUS_IMAGE=example.test/moyeo:previous-good"
grep -qx "compose|up|" "${temp_dir}/docker-failed.log"
grep -qx "example.test/moyeo:current-good" "${temp_dir}/retain-args-failed"
grep -qx "example.test/moyeo:previous-good" "${temp_dir}/retain-args-failed"
if grep -q "new-bad" "${temp_dir}/retain-args-failed"; then
  echo "Failed candidate image was retained." >&2
  exit 1
fi

echo "Deployment script tests passed."
