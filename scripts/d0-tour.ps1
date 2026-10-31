<#
.SYNOPSIS
  D0 (slide 3): tur TokoKu versi final. Place order -> event Kafka -> reservasi stok + notifikasi -> SATU trace di Grafana.
.DESCRIPTION
  Prasyarat: tokoku-lab di tag "final", mode perf menyala:
    git -C ..\tokoku-lab checkout final
    (di tokoku-lab) .\mvnw.cmd package -DskipTests
    docker compose -f infra/compose.yaml --profile db --profile kafka --profile obs --profile security --profile apps up -d --build --wait
#>
$ErrorActionPreference = 'Continue'
Import-Module (Join-Path $PSScriptRoot 'tokoku.psm1') -Force

$alice = Get-Token @{ User = 'alice' }
$traceId = -join ((1..32) | ForEach-Object { '{0:x}' -f (Get-Random -Maximum 16) })
$spanId = -join ((1..16) | ForEach-Object { '{0:x}' -f (Get-Random -Maximum 16) })

Write-Host '==> alice membuat order (product 3)' -ForegroundColor Cyan
$order = Invoke-RestMethod -Method Post http://localhost:8082/api/orders -ContentType 'application/json' `
    -Headers @{ Authorization = "Bearer $alice"; traceparent = "00-$traceId-$spanId-01" } `
    -Body '{"lines":[{"productId":3,"quantity":1}]}'
Write-Host "    order $($order.id) status $($order.status)"
Start-Sleep 3
$after = Invoke-RestMethod "http://localhost:8082/api/orders/$($order.id)" -Headers @{ Authorization = "Bearer $alice" }
Write-Host "    setelah saga: status $($after.status)"
Write-Host '==> notification-service:' -ForegroundColor Cyan
docker logs --since 30s tokoku-notification-service-1 2>&1 | Select-String "order $($order.id) " | ForEach-Object { '    ' + ($_.Line -replace '^.*OrderPlacedListener\s+: ', '') }
Write-Host '==> Satu trace untuk seluruh alur (HTTP order -> HTTP inventory -> Kafka -> inventory + notification -> Kafka -> order):' -ForegroundColor Green
Write-Host "    Grafana > Explore > Tempo > TraceQL: $traceId"
Write-Host "    http://localhost:3000/explore (pilih Tempo, tempel trace ID)"
