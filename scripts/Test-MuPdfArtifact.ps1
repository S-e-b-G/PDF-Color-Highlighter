# SPDX-License-Identifier: AGPL-3.0-or-later
$ErrorActionPreference = 'Stop'
Add-Type -AssemblyName System.IO.Compression.FileSystem
$root = Split-Path -Parent $PSScriptRoot
$dir = Join-Path $root '.validation'
New-Item $dir -ItemType Directory -Force | Out-Null
$version = '1.28.5'
$url = "https://maven.ghostscript.com/com/artifex/mupdf/fitz/$version/fitz-$version.aar"
$temp = Join-Path ([IO.Path]::GetTempPath()) ('mupdf-aar-' + [Guid]::NewGuid())
New-Item $temp -ItemType Directory | Out-Null
try {
    $aar = Join-Path $temp 'fitz.aar'
    Invoke-WebRequest $url -OutFile $aar -UseBasicParsing
    $hash = (Get-FileHash $aar -Algorithm SHA256).Hash.ToLowerInvariant()
    $archive = [IO.Compression.ZipFile]::OpenRead($aar)
    try {
        $native = @($archive.Entries | Where-Object { $_.FullName -match '^jni/[^/]+/.+\.so$' } | ForEach-Object { $_.FullName })
        $classes = $archive.GetEntry('classes.jar')
        if (-not $classes) { throw 'AAR has no classes.jar.' }
        $jar = Join-Path $temp 'classes.jar'
        [IO.Compression.ZipFileExtensions]::ExtractToFile($classes, $jar, $false)
    } finally { $archive.Dispose() }
    $jarArchive = [IO.Compression.ZipFile]::OpenRead($jar)
    try {
        foreach ($class in @('com/artifex/mupdf/fitz/Document.class',
                'com/artifex/mupdf/fitz/android/AndroidDrawDevice.class')) {
            if (-not $jarArchive.GetEntry($class)) { throw "Required class missing: $class" }
        }
    } finally { $jarArchive.Dispose() }
    if ($native.Count -eq 0) { throw 'No JNI libraries found.' }
    $report = [ordered]@{ version=$version; url=$url; sha256=$hash;
        nativeLibraries=$native; checkedAt=(Get-Date).ToString('o');
        checksumTrust='Computed locally; not compared with an independently authenticated vendor checksum.' }
    $report | ConvertTo-Json -Depth 5 | Set-Content (Join-Path $dir 'mupdf-artifact.json') -Encoding UTF8
    $report | ConvertTo-Json -Depth 5
} catch {
    Write-Error "MuPDF prebuilt integration remains blocked: $($_.Exception.GetType().Name). Do not substitute another PDF engine or enable HTTP."
    exit 1
} finally { Remove-Item $temp -Recurse -Force }
