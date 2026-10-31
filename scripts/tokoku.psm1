# Fungsi bantu demo yang memakai sistem TokoKu (repo tokoku-lab di folder sebelah).
$script:Lab = (Resolve-Path (Join-Path $PSScriptRoot '..\..\tokoku-lab')).Path

function Get-LabPath([string]$relative) { Join-Path $script:Lab $relative }

function Get-Token([string[]]$TokenArgs) {
    $script = Get-LabPath 'scripts\get-token.ps1'
    (& $script @TokenArgs | Select-Object -Last 1).Trim()
}

function Invoke-Prometheus([string]$Query) {
    $url = 'http://localhost:3000/api/datasources/proxy/uid/prometheus/api/v1/query?query=' + [uri]::EscapeDataString($Query)
    $auth = [Convert]::ToBase64String([Text.Encoding]::ASCII.GetBytes('admin:admin'))
    (Invoke-RestMethod $url -Headers @{ Authorization = "Basic $auth" }).data.result
}

Export-ModuleMember -Function Get-LabPath, Get-Token, Invoke-Prometheus
