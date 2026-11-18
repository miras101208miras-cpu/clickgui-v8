$ErrorActionPreference = 'Stop'
[Net.ServicePointManager]::SecurityProtocol = [Net.SecurityProtocolType]::Tls12
$root  = Split-Path -Parent (Split-Path -Parent $MyInvocation.MyCommand.Path)
$props = Join-Path $root 'gradle.properties'
$text  = Get-Content $props -Raw
$mc    = '1.21.11'

if ($text -match 'yarn_mappings=AUTO') {
    Write-Host 'Fetching Yarn version...'
    $yarn = (Invoke-RestMethod "https://meta.fabricmc.net/v2/versions/yarn/$mc")[0].version
    $text = $text -replace 'yarn_mappings=AUTO', "yarn_mappings=$yarn"
}
if ($text -match 'loader_version=AUTO') {
    Write-Host 'Fetching Fabric Loader version...'
    $loader = ((Invoke-RestMethod 'https://meta.fabricmc.net/v2/versions/loader') | Where-Object { $_.stable })[0].version
    $text = $text -replace 'loader_version=AUTO', "loader_version=$loader"
}
if ($text -match 'fabric_version=AUTO') {
    Write-Host 'Fetching Fabric API version...'
    [xml]$meta = (Invoke-WebRequest 'https://maven.fabricmc.net/net/fabricmc/fabric-api/fabric-api/maven-metadata.xml' -UseBasicParsing).Content
    $fapi = $meta.metadata.versioning.versions.version | Where-Object { $_ -like "*+$mc" } | Select-Object -Last 1
    if (-not $fapi) { throw "Fabric API for $mc not found" }
    $text = $text -replace 'fabric_version=AUTO', "fabric_version=$fapi"
}
Set-Content -Path $props -Value $text -Encoding ASCII

$gv   = '9.2.1'
$dist = Join-Path $root '.gradle-dist'
$gh   = Join-Path $dist "gradle-$gv"
if (-not (Test-Path (Join-Path $gh 'bin\gradle.bat'))) {
    New-Item -ItemType Directory -Force $dist | Out-Null
    $zip = Join-Path $dist 'gradle.zip'
    Write-Host "Downloading Gradle $gv (one time, ~130 MB)..."
    Invoke-WebRequest "https://services.gradle.org/distributions/gradle-$gv-bin.zip" -OutFile $zip -UseBasicParsing
    Expand-Archive $zip $dist -Force
    Remove-Item $zip
}
$fontDir = Join-Path $root 'src\main\resources\assets\clickgui\font'
$marker  = Join-Path $fontDir '.inter'
if (-not (Test-Path $marker)) {
    try {
        Write-Host 'Downloading Inter font...'
        $tmp = Join-Path $env:TEMP 'clickgui-inter.ttf'
        Invoke-WebRequest 'https://raw.githubusercontent.com/google/fonts/main/ofl/inter/Inter%5Bopsz,wght%5D.ttf' -OutFile $tmp -UseBasicParsing
        Move-Item $tmp (Join-Path $fontDir 'inter.ttf') -Force
        Set-Content $marker 'ok'
    } catch { Write-Host 'Inter download failed, using fallback font' }
}
Write-Host 'Setup OK' 
