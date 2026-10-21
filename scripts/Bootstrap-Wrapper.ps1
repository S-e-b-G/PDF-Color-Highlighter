# SPDX-License-Identifier: AGPL-3.0-or-later
$ErrorActionPreference = 'Stop'
$root = Split-Path -Parent $PSScriptRoot
$version = '9.4.1'
$paths = @('gradlew', 'gradlew.bat', 'gradle/wrapper/gradle-wrapper.jar',
           'gradle/wrapper/gradle-wrapper.properties')
foreach ($path in $paths) {
    if (Test-Path (Join-Path $root $path)) { throw "Refusing to overwrite existing wrapper file: $path" }
}
if (-not (Get-Command java -ErrorAction SilentlyContinue)) { throw 'Install JDK 17 and add java to PATH first.' }
$temp = Join-Path ([IO.Path]::GetTempPath()) ('pdf-wrapper-' + [Guid]::NewGuid())
New-Item $temp -ItemType Directory | Out-Null
try {
    $url = "https://services.gradle.org/distributions/gradle-$version-bin.zip"
    $expected = (Invoke-RestMethod "$url.sha256").Trim().ToLowerInvariant()
    if ($expected -notmatch '^[0-9a-f]{64}$') { throw 'Invalid official distribution checksum.' }
    $zip = Join-Path $temp 'gradle.zip'
    Invoke-WebRequest $url -OutFile $zip -UseBasicParsing
    $actual = (Get-FileHash $zip -Algorithm SHA256).Hash.ToLowerInvariant()
    if ($actual -ne $expected) { throw 'Gradle distribution checksum mismatch.' }
    Expand-Archive $zip -DestinationPath $temp
    $bootstrap = Join-Path $temp 'project'
    New-Item $bootstrap -ItemType Directory | Out-Null
    'rootProject.name = "wrapper-bootstrap"' | Set-Content (Join-Path $bootstrap 'settings.gradle.kts') -Encoding ASCII
    '' | Set-Content (Join-Path $bootstrap 'build.gradle.kts') -Encoding ASCII
    $gradle = Join-Path $temp "gradle-$version/bin/gradle.bat"
    & $gradle --no-daemon -p $bootstrap :wrapper --gradle-version $version --distribution-type bin --gradle-distribution-sha256-sum $expected
    if ($LASTEXITCODE -ne 0) { throw 'Wrapper generation failed.' }
    $jar = Join-Path $bootstrap 'gradle/wrapper/gradle-wrapper.jar'
    $jarExpected = (Invoke-RestMethod "https://services.gradle.org/distributions/gradle-$version-wrapper.jar.sha256").Trim().ToLowerInvariant()
    if ((Get-FileHash $jar -Algorithm SHA256).Hash.ToLowerInvariant() -ne $jarExpected) { throw 'Wrapper JAR checksum mismatch.' }
    New-Item (Join-Path $root 'gradle/wrapper') -ItemType Directory -Force | Out-Null
    foreach ($path in $paths) { Copy-Item (Join-Path $bootstrap $path) (Join-Path $root $path) }
    Write-Host 'Official Gradle wrapper generated and checksums verified.'
} finally { Remove-Item $temp -Recurse -Force }
