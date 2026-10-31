# Fungsi bantu untuk skrip demo yang menjalankan demo-jpa (D7.1, D7.2).
$script:Jar = (Resolve-Path (Join-Path $PSScriptRoot '..\demo-jpa\target\demo-jpa-1.0.0-SNAPSHOT.jar')).Path

function Start-DemoJpa([int]$Port = 8095, [string[]]$AppArgs = @()) {
    $log = Join-Path $env:TEMP "demo-jpa-$Port.log"
    $args = @('-jar', "`"$script:Jar`"", "--server.port=$Port") + $AppArgs
    $p = Start-Process java -ArgumentList $args -RedirectStandardOutput $log -RedirectStandardError "$log.err" -PassThru -WindowStyle Hidden
    for ($i = 0; $i -lt 90; $i++) {
        try { Invoke-RestMethod "http://localhost:$Port/actuator/health" -TimeoutSec 2 | Out-Null; return $p } catch { Start-Sleep 1 }
    }
    throw "demo-jpa :$Port tidak start (lihat $log)"
}

function Stop-DemoJpa {
    Get-CimInstance Win32_Process -Filter "Name='java.exe'" | Where-Object { $_.CommandLine -match 'demo-jpa' } |
        ForEach-Object { Stop-Process -Id $_.ProcessId -Force }
}

Export-ModuleMember -Function Start-DemoJpa, Stop-DemoJpa
