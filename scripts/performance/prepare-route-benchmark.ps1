[CmdletBinding()]
param(
    [Parameter(Mandatory)][ValidateRange(2, 20)][int] $Participants,
    [Parameter(Mandatory)][ValidateRange(1, 20)][int] $MeetingCount,
    [Parameter(Mandatory)][ValidatePattern('^[A-Za-z0-9-]{1,40}$')][string] $Scenario,
    [string] $BaseUrl = $env:MOYEO_BENCHMARK_BASE_URL,
    [string] $HostToken = $env:MOYEO_BENCHMARK_HOST_TOKEN,
    [string] $OutputDirectory = (Join-Path $PSScriptRoot '..\..\artifacts\route-benchmarks')
)

$ErrorActionPreference = 'Stop'
if ([string]::IsNullOrWhiteSpace($BaseUrl)) { throw 'Set MOYEO_BENCHMARK_BASE_URL or pass -BaseUrl.' }
if ([string]::IsNullOrWhiteSpace($HostToken)) { throw 'Set MOYEO_BENCHMARK_HOST_TOKEN or pass -HostToken.' }
$BaseUrl = $BaseUrl.TrimEnd('/')
$runId = "{0}-{1}" -f $Scenario, (Get-Date -Format 'yyyyMMdd-HHmmss')
$runDirectory = Join-Path $OutputDirectory $runId
New-Item -ItemType Directory -Force -Path $runDirectory | Out-Null
$supportedBenchmarkAddress = -join @(
    [char]0xACBD, [char]0xAE30, [char]0xB3C4, ' ',
    [char]0xC131, [char]0xB0A8, [char]0xC2DC, ' ',
    [char]0xBD84, [char]0xB2F9, [char]0xAD6C
)
$benchmarkLatitude = 37.359571
$benchmarkLongitude = 127.105399

function ConvertTo-AsciiJson([object] $Body) {
    $json = $Body | ConvertTo-Json -Depth 6 -Compress
    return -join ($json.ToCharArray() | ForEach-Object {
        if ([int][char]$_ -gt 127) { '\u{0:x4}' -f [int][char]$_ } else { $_ }
    })
}

function Invoke-JsonPost([string] $Path, [object] $Body, [bool] $Authenticated) {
    # Passing a JSON string as a native Windows curl argument can strip its quotes.
    # A UTF-8 request file preserves the JSON body for Windows PowerShell and PowerShell 7.
    $requestFile = [System.IO.Path]::GetTempFileName()
    $responseFile = [System.IO.Path]::GetTempFileName()
    try {
        $utf8NoBom = New-Object System.Text.UTF8Encoding($false)
        [System.IO.File]::WriteAllText($requestFile, (ConvertTo-AsciiJson $Body), $utf8NoBom)
        $arguments = @('--silent', '--show-error', '--request', 'POST', '--header', 'Content-Type: application/json')
        if ($Authenticated) { $arguments += @('--header', "Authorization: Bearer $HostToken") }
        $arguments += @('--data-binary', "@$requestFile", '--output', $responseFile, '--write-out', '%{http_code}', "$BaseUrl$Path")
        $status = & curl.exe @arguments
        if ($LASTEXITCODE -ne 0) { throw "curl failed for POST $Path (exit $LASTEXITCODE)." }
        $response = [System.IO.File]::ReadAllText($responseFile, $utf8NoBom)
        if ($status -notmatch '^2\d\d$') { throw "POST $Path returned HTTP ${status}: $response" }
        return $response | ConvertFrom-Json
    } finally {
        Remove-Item -LiteralPath $requestFile -Force -ErrorAction SilentlyContinue
        Remove-Item -LiteralPath $responseFile -Force -ErrorAction SilentlyContinue
    }
}

$meetings = [System.Collections.Generic.List[object]]::new()
for ($meetingIndex = 1; $meetingIndex -le $MeetingCount; $meetingIndex++) {
    $meetingName = "PERF$meetingIndex$runId"
    $createBody = @{
        name = $meetingName.Substring(0, [Math]::Min(15, $meetingName.Length))
        maxParticipants = $Participants; planningType = 'PLACE_ONLY'; deadlineMinutes = 60
        departure = @{ address = $supportedBenchmarkAddress; latitude = $benchmarkLatitude; longitude = $benchmarkLongitude; transportationMode = 'CAR' }
    }
    $created = Invoke-JsonPost '/api/meetings' $createBody $true
    if ([string]::IsNullOrWhiteSpace($created.inviteCode)) { throw 'Meeting creation did not return an inviteCode.' }
    for ($guestIndex = 1; $guestIndex -lt $Participants; $guestIndex++) {
        $guestBody = @{ nickname = ('guest' + [char](96 + $guestIndex)); password = '1234'; departure = @{ address = $supportedBenchmarkAddress; latitude = $benchmarkLatitude; longitude = $benchmarkLongitude; transportationMode = 'CAR' } }
        $null = Invoke-JsonPost "/api/meetings/invitations/$($created.inviteCode)/guests" $guestBody $false
    }
    $meetings.Add([PSCustomObject]@{ run_id = $runId; scenario = $Scenario; meeting_id = $created.meetingId; invite_code = $created.inviteCode; participants = $Participants; transportation_mode = 'CAR'; created_at_utc = (Get-Date).ToUniversalTime().ToString('o') })
}
$manifestPath = Join-Path $runDirectory 'meetings.csv'
$meetings | Export-Csv -NoTypeInformation -Encoding utf8 -Path $manifestPath
Write-Host "Prepared $MeetingCount fresh meetings. Manifest: $manifestPath"
