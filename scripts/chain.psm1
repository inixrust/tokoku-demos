# Fungsi bantu rantai A -> B -> C (D8.2, D10.1).
$script:Jar = (Resolve-Path (Join-Path $PSScriptRoot '..\demo-chain\target\demo-chain-1.0.0-SNAPSHOT.jar')).Path

function Start-Hop([string]$Name, [int]$Port, [string]$Downstream, [string]$Delay = '0ms', [int]$Attempts = 1) {
    $args = @('-jar', "`"$script:Jar`"", "--server.port=$Port", "--hop.name=$Name", "--hop.downstream=$Downstream",
        "--hop.delay=$Delay", "--hop.attempts=$Attempts")
    Start-Process java -ArgumentList $args -RedirectStandardOutput (Join-Path $env:TEMP "hop-$Name.log") `
        -RedirectStandardError (Join-Path $env:TEMP "hop-$Name.err") -WindowStyle Hidden | Out-Null
}

function Wait-Hops([int[]]$Ports) {
    foreach ($port in $Ports) {
        for ($i = 0; $i -lt 90; $i++) {
            try { Invoke-RestMethod "http://localhost:$port/actuator/health" -TimeoutSec 2 | Out-Null; break } catch { Start-Sleep 1 }
        }
    }
}

function Stop-Hops {
    Get-CimInstance Win32_Process -Filter "Name='java.exe'" | Where-Object { $_.CommandLine -match 'demo-chain' } |
        ForEach-Object { Stop-Process -Id $_.ProcessId -Force }
}

function Invoke-Timed([string]$Url) {
    $sw = [Diagnostics.Stopwatch]::StartNew()
    $status = curl.exe -s -o NUL -w '%{http_code}' $Url
    $sw.Stop()
    return '{0} dalam {1:N0} ms' -f $status, $sw.Elapsed.TotalMilliseconds
}

Export-ModuleMember -Function Start-Hop, Wait-Hops, Stop-Hops, Invoke-Timed
