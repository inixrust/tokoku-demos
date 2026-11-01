<#
.SYNOPSIS
  D11.1 dan D11.3 (slide 87-89): token user (PKCE) vs token service (client credentials), decode lokal;
  token untuk API lain (aud bukan order-service) ditolak 401.
.DESCRIPTION
  Prasyarat: profil security (Keycloak) dan order-service L11.1-solution atau sesudahnya.
#>
$ErrorActionPreference = 'Continue'
Import-Module (Join-Path $PSScriptRoot 'tokoku.psm1') -Force
$getToken = Get-LabPath 'scripts\get-token.ps1'

Write-Host '==> D11.1 token alice (authorization code + PKCE, client tokoku-web)' -ForegroundColor Cyan
& $getToken -User alice -Decode | Out-Null
Write-Host '==> D11.1 token tokoku-loadtest (client credentials)' -ForegroundColor Cyan
& $getToken -ClientCredentials -Decode | Out-Null
Write-Host '    Bandingkan sub, aud, scope, exp. Audience mapper: Keycloak > realm tokoku > Client scopes > orders.read > Mappers.'

Write-Host '==> D11.3 token untuk API lain (aud inventory-service)' -ForegroundColor Cyan
$other = (& $getToken -OtherApi -Decode | Select-Object -Last 1).Trim()
Write-Host "    GET /api/orders dengan token itu: $(curl.exe -s -o NUL -w 'HTTP %{http_code}' -H "Authorization: Bearer $other" http://localhost:8082/api/orders)  (harapan 401)" -ForegroundColor Yellow
