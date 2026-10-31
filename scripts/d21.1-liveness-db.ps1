<#
.SYNOPSIS
  D21.1 (slide 154): liveness yang memeriksa DB -> DB mati -> restart massal (CrashLoopBackOff).
  Bandingkan dengan konfigurasi benar (L21.1-solution): readiness turun, 0 restart.
.DESCRIPTION
  Prasyarat: tokoku-lab L21.1-solution, image product-service:1.0, kubectl context docker-desktop,
  profil db menyala, dan profil apps DIMATIKAN (slot connection PostgreSQL).
#>
$ErrorActionPreference = 'Continue'
Import-Module (Join-Path $PSScriptRoot 'tokoku.psm1') -Force
$compose = Get-LabPath 'infra\compose.yaml'
kubectl config use-context docker-desktop | Out-Null
kubectl apply -f (Get-LabPath 'k8s') | Out-Null

function Watch-Pods([string]$title, [int]$seconds) {
    $end = (Get-Date).AddSeconds($seconds)
    while ((Get-Date) -lt $end) {
        Start-Sleep 15
        $pods = kubectl get pods -l app=product-service --no-headers | ForEach-Object { $c = -split $_; "$($c[1]) $($c[2]) restarts=$($c[3])" }
        Write-Host ("    {0} {1:HH:mm:ss}  {2}" -f $title, (Get-Date), ($pods -join ' | '))
    }
}

Write-Host '==> SALAH: liveness = livenessState,db' -ForegroundColor Cyan
kubectl set env deploy/product-service MANAGEMENT_ENDPOINT_HEALTH_GROUP_LIVENESS_INCLUDE=livenessState,db | Out-Null
kubectl rollout status deploy/product-service --timeout=180s | Out-Null
docker compose -f $compose stop postgres *> $null
Watch-Pods 'DB mati' 75
docker compose -f $compose start postgres *> $null

Write-Host '==> BENAR: liveness tanpa db, readiness = readinessState,db' -ForegroundColor Cyan
kubectl set env deploy/product-service MANAGEMENT_ENDPOINT_HEALTH_GROUP_LIVENESS_INCLUDE- | Out-Null
kubectl rollout status deploy/product-service --timeout=180s | Out-Null
docker compose -f $compose stop postgres *> $null
Watch-Pods 'DB mati' 45
docker compose -f $compose start postgres *> $null
Watch-Pods 'DB hidup' 30
