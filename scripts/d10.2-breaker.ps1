<#
.SYNOPSIS
  D10.2 (slide 78, 81): Toxiproxy mematikan inventory -> breaker OPEN (fail fast) -> HALF_OPEN -> CLOSED.
.DESCRIPTION
  Prasyarat: tokoku-lab L10.1-solution atau sesudahnya, mode dev (order-service di IDE, base-url localhost:18083),
  profil chaos menyala. Buka dashboard Grafana "TokoKu - Resilience" di layar.
#>
$ErrorActionPreference = 'Continue'
Import-Module (Join-Path $PSScriptRoot 'tokoku.psm1') -Force
$chaos = Get-LabPath 'scripts\chaos.ps1'
$alice = Get-Token @('-User', 'alice')

function State {
    $line = (curl.exe -s http://localhost:9082/actuator/prometheus) -split "`n" |
        Where-Object { $_ -match '^resilience4j_circuitbreaker_state\{.*name="inventory".*\} 1' } | Select-Object -First 1
    if ($line -match 'state="([a-z_]+)"') { $Matches[1].ToUpper() } else { '?' }
}

function Burst([string]$title, [int]$n) {
    $codes = 1..$n | ForEach-Object {
        curl.exe -s -o NUL -w '%{http_code}/%{time_total}s ' -H "Authorization: Bearer $alice" -H 'Content-Type: application/json' `
            -d '{\"lines\":[{\"productId\":2,\"quantity\":1}]}' http://localhost:8082/api/orders
    }
    Write-Host ("{0,-28} breaker={1,-10} {2}" -f $title, (State), (($codes | Select-Object -Last 4) -join ''))
}

Burst 'normal' 5
& $chaos -Down | Out-Null
Burst 'inventory mati (12 order)' 12
Burst 'breaker terbuka: fail fast' 5
& $chaos -Reset | Out-Null
Write-Host '    tunggu waitDurationInOpenState 10 s...'
Start-Sleep 11
Burst 'trial HALF_OPEN -> CLOSED' 5
