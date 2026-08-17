#!/usr/bin/env bash
set -euo pipefail

interval_seconds="${1:-1}"
output_file="${2:-route-benchmark-server-stats.csv}"
if ! [[ "$interval_seconds" =~ ^[1-9][0-9]*$ ]]; then echo "Usage: $0 [interval-seconds] [output-file]" >&2; exit 2; fi

echo 'timestamp_utc,mem_total_kib,mem_available_kib,mem_used_percent,container,cpu_percent,mem_usage,mem_percent' > "$output_file"
while true; do
  timestamp="$(date -u +%Y-%m-%dT%H:%M:%SZ)"
  mem_total="$(awk '/MemTotal:/ {print $2}' /proc/meminfo)"
  mem_available="$(awk '/MemAvailable:/ {print $2}' /proc/meminfo)"
  mem_used_percent="$(awk -v total="$mem_total" -v available="$mem_available" 'BEGIN { printf "%.2f", ((total - available) / total) * 100 }')"
  docker stats --no-stream --format '{{.Name}},{{.CPUPerc}},{{.MemUsage}},{{.MemPerc}}' | while IFS= read -r container_stats; do
    printf '%s,%s,%s,%s,%s\n' "$timestamp" "$mem_total" "$mem_available" "$mem_used_percent" "$container_stats" >> "$output_file"
  done
  sleep "$interval_seconds"
done
