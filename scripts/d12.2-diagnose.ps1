<#
.SYNOPSIS
  D12.2 (slide 109): JFR (hot method, alokasi), thread dump JSON, heap dump leak.
.DESCRIPTION
  Menjalankan demo-web di container (base -jdk), memberi beban CPU + leak, lalu mengambil:
    out\d122.jfr        -> buka di JDK Mission Control: Method Profiling (DiagnosticsController.hot), Allocation
    out\threads.json    -> thread dump format JSON (termasuk virtual thread)
    out\heap.hprof      -> buka di Eclipse MAT: Dominator Tree -> DiagnosticsController.SESSION_SNAPSHOTS
#>
$ErrorActionPreference = 'Continue'
$root = Resolve-Path (Join-Path $PSScriptRoot '..')
$out = Join-Path $root 'out'
New-Item -ItemType Directory -Force $out | Out-Null
docker build -q -t tokoku-demos/demo-web:1.0 (Join-Path $root 'demo-web') | Out-Null

docker rm -f d122 *> $null
docker run -d --name d122 -m 1g -p 8093:8090 tokoku-demos/demo-web:1.0 | Out-Null
for ($i = 0; $i -lt 60; $i++) {
    try { Invoke-RestMethod http://localhost:8093/actuator/health -TimeoutSec 2 | Out-Null; break } catch { Start-Sleep 1 }
}

Write-Host '==> JFR 30 s sambil memberi beban /d122/hot dan leak 200 MiB' -ForegroundColor Cyan
docker exec d122 jcmd 1 JFR.start name=d122 duration=30s filename=/tmp/d122.jfr settings=profile | Out-Null
1..20 | ForEach-Object { Invoke-RestMethod -Method Post "http://localhost:8093/d122/leak?mb=10" | Out-Null }
$deadline = (Get-Date).AddSeconds(30)
while ((Get-Date) -lt $deadline) { Invoke-RestMethod "http://localhost:8093/d122/hot" | Out-Null }
Start-Sleep 3

Write-Host '==> Thread dump JSON dan heap dump' -ForegroundColor Cyan
docker exec d122 jcmd 1 Thread.dump_to_file -format=json /tmp/threads.json | Out-Null
docker exec d122 jcmd 1 GC.heap_dump /tmp/heap.hprof | Out-Null
foreach ($f in 'd122.jfr', 'threads.json', 'heap.hprof') { docker cp "d122:/tmp/$f" (Join-Path $out $f) | Out-Null }
docker rm -f d122 *> $null

Get-ChildItem $out | ForEach-Object { Write-Host ('{0,-14} {1,8:N1} MiB' -f $_.Name, ($_.Length / 1MB)) }
Write-Host 'Cepat tanpa GUI:  jfr print --events jdk.ExecutionSample out\d122.jfr | Select-String DiagnosticsController | Select-Object -First 3'
