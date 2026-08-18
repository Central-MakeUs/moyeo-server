# Route benchmark scripts

These scripts benchmark the existing place-view API against the deployed dev server. They do not add an application API and do not send the Kakao REST API key from the client.

## Safety rules

- Run only against the development server, never production.
- Set `MOYEO_BENCHMARK_BASE_URL` and `MOYEO_BENCHMARK_HOST_TOKEN` in the local shell. Do not put values in a script, CSV, or commit.
- The preparation script sets every participant to `CAR`. Kakao automobile directions therefore have no walking fallback.
- A prepared meeting must be used for exactly one place-view request because a successful result is cached as a snapshot.
- The current application permits up to five preliminary candidates. Seven-candidate runs need a separately reviewed benchmark-only server configuration.

## Baseline workflow

1. Start server monitoring on the dev instance and stop it with `Ctrl+C` after the client run.

   ```bash
   ./collect-route-benchmark-stats.sh 1 server-stats.csv
   ```

2. Set local-only client configuration.

   ```powershell
   $env:MOYEO_BENCHMARK_BASE_URL = 'https://api.moyeo.app'
   $env:MOYEO_BENCHMARK_HOST_TOKEN = 'dev access token'
   ```

3. Prepare fresh meetings, then issue exactly one request per prepared meeting.

   ```powershell
   ./prepare-route-benchmark.ps1 -Scenario single-p20 -Participants 20 -MeetingCount 1
   ./run-route-benchmark.ps1 -ManifestPath <generated meetings.csv> -Implementation serial -Scenario single-meeting -CandidateCount 3
   ```

4. Generate the portfolio summary after copying the server statistics file into the same local result directory.

   ```powershell
   ./summarize-route-benchmark.ps1 -ResultsPath <generated results.csv> -ServerStatsPath <server-stats.csv>
   ```

`results.csv` contains client-observed HTTP status and elapsed time. `server-stats.csv` contains host memory availability plus per-container CPU/memory snapshots. The summary script writes `report.md`. Generated data is deliberately ignored under `artifacts/route-benchmarks/`; copy only reviewed aggregates into a portfolio report.
