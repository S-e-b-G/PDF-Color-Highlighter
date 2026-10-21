# SPDX-License-Identifier: AGPL-3.0-or-later
param([string]$ProjectRoot=(Split-Path -Parent $PSScriptRoot),[string]$AdbPath='')
$ErrorActionPreference='Stop'
$root=[IO.Path]::GetFullPath($ProjectRoot)
if (-not $AdbPath) {
    $local=Join-Path $root 'local.properties'
    if (Test-Path -LiteralPath $local) {
        $line=[IO.File]::ReadAllLines($local) | Where-Object { $_ -match '^sdk\.dir=' } | Select-Object -First 1
        if ($line) {
            $sdk=$line.Substring('sdk.dir='.Length).Replace('\\','\').Replace('\:',':')
            $AdbPath=Join-Path $sdk 'platform-tools/adb.exe'
        }
    }
    if (-not $AdbPath -or -not (Test-Path -LiteralPath $AdbPath)) {
        $AdbPath=Join-Path $env:LOCALAPPDATA 'Android/Sdk/platform-tools/adb.exe'
    }
}
if (-not (Test-Path -LiteralPath $AdbPath)) { throw 'adb.exe not found; provide -AdbPath.' }
$devices=@(& $AdbPath devices)
if ($LASTEXITCODE -ne 0) { throw 'ADB device listing failed.' }
$authorized=@($devices | Where-Object { $_ -match '^(\S+)\s+device\s*$' })
if ($authorized.Count -ne 1) { throw 'Connect exactly one authorized tablet.' }
$serial=($authorized[0] -split '\s+')[0]
$directory=Join-Path $root '.validation'
New-Item -ItemType Directory -Path $directory -Force | Out-Null
$file=Join-Path $directory ('viewport-'+(Get-Date -Format 'yyyyMMdd-HHmmss')+'-'+[Guid]::NewGuid().ToString('N').Substring(0,8)+'.txt')
& $AdbPath -s $serial logcat -d -v threadtime -s 'PdfViewport:D' '*:S' |
    Out-File -LiteralPath $file -Encoding utf8
if ($LASTEXITCODE -ne 0) { throw 'Could not collect the viewport log.' }
Get-Item -LiteralPath $file | Select-Object FullName,Length
Write-Host 'Attach this TXT file. It contains only the PdfViewport log tag; no log buffer was cleared.'
