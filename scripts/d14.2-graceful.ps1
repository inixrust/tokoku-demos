<#
.SYNOPSIS
  D14.2 (slide 125-126): graceful shutdown dengan request 20 s yang sedang berjalan.
.DESCRIPTION
  a. docker stop -t 40, timeout shutdown Spring 30 s (default)  -> request selesai bersih
  b. docker stop -t 40, timeout-per-shutdown-phase=5s           -> request terpotong setelah 5 s
  c. docker stop tanpa -t, timeout 30 s                          -> SIGKILL sebelum request selesai (exit 137)
     Dokumentasi Docker: default 10 s. Docker Engine 29.9 di laptop instruktur: terukur 3,3 s. Selalu tulis -t.
  Pelajaran: grace period platform > timeout shutdown Spring > durasi request terlama yang ingin diselesaikan.
#>
$ErrorActionPreference = 'Continue'
$root = Resolve-Path (Join-Path $PSScriptRoot '..')
docker build -q -t tokoku-demos/demo-web:1.0 (Join-Path $root 'demo-web') | Out-Null

function Run-Case([string]$title, [string[]]$appArgs, [string[]]$stopArgs) {
    docker rm -f d142 *> $null
    docker run -d --name d142 -p 8092:8090 tokoku-demos/demo-web:1.0 @appArgs | Out-Null
    for ($i = 0; $i -lt 60; $i++) {
        try { Invoke-RestMethod http://localhost:8092/actuator/health -TimeoutSec 2 | Out-Null; break } catch { Start-Sleep 1 }
    }
    $job = Start-Job { curl.exe -s -m 60 -w " [HTTP %{http_code}, %{time_total} s]" http://localhost:8092/d142/long?seconds=20 }
    Start-Sleep 2
    $sw = [Diagnostics.Stopwatch]::StartNew()
    docker stop @stopArgs d142 | Out-Null
    $sw.Stop()
    $result = Receive-Job $job -Wait -AutoRemoveJob
    $exit = docker inspect d142 --format '{{.State.ExitCode}}'
    if (-not $result) { $result = '(koneksi terputus, tidak ada response)' }
    Write-Host ("{0}`n    request: {1}`n    docker stop {2:N1} s, exit code {3}" -f $title, $result, $sw.Elapsed.TotalSeconds, $exit) -ForegroundColor Cyan
    docker rm -f d142 *> $null
}

Run-Case 'a. stop -t 40, timeout Spring 30 s' @() @('-t', '40')
Run-Case 'b. stop -t 40, timeout Spring 5 s' @('--spring.lifecycle.timeout-per-shutdown-phase=5s') @('-t', '40')
Run-Case 'c. stop tanpa -t, timeout Spring 30 s' @() @()
