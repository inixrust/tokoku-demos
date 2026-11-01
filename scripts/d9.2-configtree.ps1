<#
.SYNOPSIS
  D9.2 (slide 72): secret dibaca dari folder yang di-mount (config tree), tidak ada di repo maupun di image.
.DESCRIPTION
  1. Tanpa mount: /d92/secret -> terbaca=false.
  2. Dengan -v <folder>:/etc/config (file db-password): terbaca=true, nilai tidak pernah dicetak utuh.
#>
$ErrorActionPreference = 'Continue'
$root = Resolve-Path (Join-Path $PSScriptRoot '..')
docker build -q -t tokoku-demos/demo-web:1.0 (Join-Path $root 'demo-web') | Out-Null

$secrets = Join-Path $env:TEMP 'd92-config'
New-Item -ItemType Directory -Force $secrets | Out-Null
Set-Content -Path (Join-Path $secrets 'db-password') -Value 's3cr3t-dari-vault' -NoNewline -Encoding ascii

function Show([string]$title, [string[]]$extra) {
    docker rm -f d92 *> $null
    docker run -d --name d92 -p 8091:8090 @extra tokoku-demos/demo-web:1.0 | Out-Null
    for ($i = 0; $i -lt 60; $i++) {
        try { $r = Invoke-RestMethod http://localhost:8091/d92/secret -TimeoutSec 2; break } catch { Start-Sleep 1 }
    }
    Write-Host ("{0,-28} {1}" -f $title, ($r | ConvertTo-Json -Compress)) -ForegroundColor Cyan
    docker rm -f d92 *> $null
}

Show 'tanpa mount:' @()
Show 'dengan /etc/config di-mount:' @('-v', "${secrets}:/etc/config:ro")
Write-Host "Isi folder yang di-mount: $(Get-ChildItem $secrets -Name)"
