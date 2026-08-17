[CmdletBinding()]
param(
    [Parameter(Mandatory)][string] $ManifestPath,
    [Parameter(Mandatory)][ValidateSet('serial', 'parallel-2')][string] $Implementation,
    [Parameter(Mandatory)][ValidateSet('candidate-count', 'concurrent-meetings', 'single-meeting')][string] $Scenario,
    [Parameter(Mandatory)][ValidateSet(3)][int] $CandidateCount,
    [ValidateRange(1, 4)][int] $Concurrency = 1,
    [string] $BaseUrl = $env:MOYEO_BENCHMARK_BASE_URL
)

$ErrorActionPreference = 'Stop'
if ([string]::IsNullOrWhiteSpace($BaseUrl)) { throw 'Set MOYEO_BENCHMARK_BASE_URL or pass -BaseUrl.' }
if (-not (Test-Path -LiteralPath $ManifestPath)) { throw "Manifest not found: $ManifestPath" }
$BaseUrl = $BaseUrl.TrimEnd('/')
$meetings = @(Import-Csv -LiteralPath $ManifestPath)
if ($meetings.Count -lt $Concurrency) { throw "The manifest has $($meetings.Count) meetings, but -Concurrency is $Concurrency." }

$runDirectory = Split-Path -Parent (Resolve-Path -LiteralPath $ManifestPath)
$resultPath = Join-Path $runDirectory 'results.csv'
$selected = $meetings | Select-Object -First $Concurrency
$jobs = foreach ($meeting in $selected) {
    Start-Job -ScriptBlock {
        param($RequestUri, $RunId, $ImplementationLabel, $ScenarioLabel, $CandidateTotal, $ConcurrentMeetings, $Meeting)
        $startedAt = [DateTime]::UtcNow
        $responseFile = [System.IO.Path]::GetTempFileName()
        try {
            $curlResult = & curl.exe --silent --show-error --output $responseFile --write-out '%{http_code},%{time_total}' $RequestUri
            $parts = $curlResult.Trim().Split(',')
            [PSCustomObject]@{ run_id = $RunId; implementation = $ImplementationLabel; scenario = $ScenarioLabel; participants = [int]$Meeting.participants; candidate_count = $CandidateTotal; concurrent_meetings = $ConcurrentMeetings; meeting_id = $Meeting.meeting_id; started_at_utc = $startedAt.ToString('o'); http_status = $parts[0]; elapsed_ms = [math]::Round(([double]$parts[1]) * 1000, 2); expected_kakao_car_calls = ([int]$Meeting.participants) * $CandidateTotal }
        } finally { Remove-Item -LiteralPath $responseFile -Force -ErrorAction SilentlyContinue }
    } -ArgumentList "$BaseUrl/api/meetings/invitations/$($meeting.invite_code)/view/places", $meeting.run_id, $Implementation, $Scenario, $CandidateCount, $Concurrency, $meeting
}
$results = $jobs | Wait-Job | Receive-Job
$jobs | Remove-Job
$results | Export-Csv -NoTypeInformation -Encoding utf8 -Append -Path $resultPath
$results | Format-Table implementation, scenario, participants, candidate_count, concurrent_meetings, http_status, elapsed_ms, expected_kakao_car_calls -AutoSize
Write-Host "Saved request results: $resultPath"
