[CmdletBinding()]
param(
    [Parameter(Mandatory)][string] $ResultsPath,
    [string] $ServerStatsPath,
    [string] $OutputPath
)

$ErrorActionPreference = 'Stop'
if (-not (Test-Path -LiteralPath $ResultsPath)) { throw "Results not found: $ResultsPath" }
if ([string]::IsNullOrWhiteSpace($OutputPath)) {
    $OutputPath = Join-Path (Split-Path -Parent (Resolve-Path -LiteralPath $ResultsPath)) 'report.md'
}

function Get-Percentile([double[]] $Values, [double] $Percentile) {
    $sorted = @($Values | Sort-Object)
    if ($sorted.Count -eq 0) { return $null }
    $index = [Math]::Ceiling($Percentile * $sorted.Count) - 1
    return $sorted[[Math]::Max(0, $index)]
}

$results = @(Import-Csv -LiteralPath $ResultsPath)
if ($results.Count -eq 0) { throw 'results.csv has no rows.' }
$lines = [System.Collections.Generic.List[string]]::new()
$lines.Add('# Route API benchmark report')
$lines.Add('')
$lines.Add("Generated at: $((Get-Date).ToUniversalTime().ToString('u'))")
$lines.Add('')
$lines.Add('All benchmark participants used `CAR`; `expected_kakao_car_calls` is therefore the exact Kakao automobile-directions request count for each meeting.')
$lines.Add('')
$lines.Add('| Implementation | Scenario | Participants | Candidates | Concurrent meetings | Requests | Success | Avg ms | p50 ms | p95 ms | Max ms | Expected Kakao calls |')
$lines.Add('|---|---|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|')

$results | Group-Object implementation, scenario, participants, candidate_count, concurrent_meetings | ForEach-Object {
    $group = @($_.Group)
    $durations = @($group | ForEach-Object { [double]$_.elapsed_ms })
    $success = @($group | Where-Object { $_.http_status -eq '200' }).Count
    $first = $group[0]
    $lines.Add('| {0} | {1} | {2} | {3} | {4} | {5} | {6}/{5} | {7:N2} | {8:N2} | {9:N2} | {10:N2} | {11} |' -f $first.implementation, $first.scenario, $first.participants, $first.candidate_count, $first.concurrent_meetings, $group.Count, $success, (($durations | Measure-Object -Average).Average), (Get-Percentile $durations 0.50), (Get-Percentile $durations 0.95), (($durations | Measure-Object -Maximum).Maximum), (($group | ForEach-Object { [int]$_.expected_kakao_car_calls } | Measure-Object -Sum).Sum))
}

if (-not [string]::IsNullOrWhiteSpace($ServerStatsPath) -and (Test-Path -LiteralPath $ServerStatsPath)) {
    $stats = @(Import-Csv -LiteralPath $ServerStatsPath)
    if ($stats.Count -gt 0) {
        $hostSnapshots = $stats | Group-Object timestamp_utc | ForEach-Object { $_.Group[0] }
        $minimumAvailableMiB = ($hostSnapshots | ForEach-Object { [double]$_.mem_available_kib / 1024 } | Measure-Object -Minimum).Minimum
        $maximumHostMemoryPercent = ($hostSnapshots | ForEach-Object { [double]$_.mem_used_percent } | Measure-Object -Maximum).Maximum
        $lines.Add('')
        $lines.Add('## Development-server resource summary')
        $lines.Add('')
        $lines.Add("- Minimum available host memory: {0:N2} MiB" -f $minimumAvailableMiB)
        $lines.Add("- Maximum host memory used: {0:N2}%" -f $maximumHostMemoryPercent)
        $lines.Add('')
        $lines.Add('| Container | Maximum CPU | Maximum memory |')
        $lines.Add('|---|---:|---:|')
        $stats | Group-Object container | ForEach-Object {
            $containerRows = @($_.Group)
            $maxCpu = ($containerRows | ForEach-Object { [double]($_.cpu_percent -replace '%', '') } | Measure-Object -Maximum).Maximum
            $maxMemory = ($containerRows | ForEach-Object { [double]($_.mem_percent -replace '%', '') } | Measure-Object -Maximum).Maximum
            $lines.Add('| {0} | {1:N2}% | {2:N2}% |' -f $_.Name, $maxCpu, $maxMemory)
        }
    }
}

$lines | Set-Content -Encoding utf8 -Path $OutputPath
Write-Host "Generated portfolio summary: $OutputPath"
