<#
.SYNOPSIS
  D10.3 (slide 81): POST /api/payments dengan Idempotency-Key.
.DESCRIPTION
  Prasyarat: tokoku-lab L10.1-solution atau sesudahnya. Agar request paralel benar-benar tumpang tindih, jalankan
  order-service dengan --tokoku.payments.gateway-delay=500ms (IDE: program argument).
  Sejak L11.1 endpoint butuh token tokoku-loadtest (scope payments.write); sebelum L11.1 header Authorization diabaikan.
#>
$ErrorActionPreference = 'Continue'
Import-Module (Join-Path $PSScriptRoot 'tokoku.psm1') -Force
$token = try { Get-Token @{ ClientCredentials = $true } } catch { 'none' }
$key = "demo-$(Get-Random)"

function Pay([string]$k, [string]$body) {
    curl.exe -s -w ' [HTTP %{http_code}]' -H "Authorization: Bearer $token" -H "Idempotency-Key: $k" `
        -H 'Content-Type: application/json' -d $body http://localhost:8082/api/payments
}

Write-Host "1. pertama        : $(Pay $key '{\"orderId\":1,\"amount\":150000,\"method\":\"CARD\"}')"
Write-Host "2. key sama       : $(Pay $key '{\"orderId\":1,\"amount\":150000,\"method\":\"CARD\"}')"
Write-Host "3. payload beda   : $(Pay $key '{\"orderId\":1,\"amount\":999,\"method\":\"CARD\"}')"
$parallelKey = "par-$(Get-Random)"
$jobs = 1..5 | ForEach-Object {
    Start-Job -ArgumentList $token, $parallelKey {
        param($t, $k)
        curl.exe -s -o NUL -w '%{http_code}' -H "Authorization: Bearer $t" -H "Idempotency-Key: $k" -H 'Content-Type: application/json' `
            -d '{\"orderId\":2,\"amount\":5000,\"method\":\"VA\"}' http://localhost:8082/api/payments
    }
}
Write-Host "4. 5x paralel     : $((Receive-Job $jobs -Wait -AutoRemoveJob) -join ' ')   (harapan: 201 + 409; satu payment)"
