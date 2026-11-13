# Install the matching release drop-in; preserve native bundles and configuration.
[CmdletBinding()]
param([string]$CapellaDir = $env:CAPELLA_HOME)
$ErrorActionPreference = 'Stop'
if (-not $CapellaDir) { $CapellaDir = 'C:\Program Files\Capella\eclipse' }
$project = Split-Path $PSScriptRoot -Parent
$version = (Select-String -Path "$project\META-INF\MANIFEST.MF" -Pattern '^Bundle-Version: (.+)$').Matches.Groups[1].Value.Trim()
$analysis = @(Get-ChildItem -LiteralPath "$CapellaDir\plugins" -Filter 'org.polarsys.capella.core.sirius.analysis_*.jar')
if ($analysis.Count -ne 1) { throw 'Expected one Capella analysis bundle.' }
if ($analysis[0].Name -notmatch '_([0-9]+\.[0-9]+\.[0-9]+)') { throw 'Cannot identify Capella version.' }
$capellaVersion = $Matches[1]
$archiveRoot = "capella-beautifier-v$version-capella-$capellaVersion-full"
$package = Join-Path $project "dist\$archiveRoot.zip"
if (-not (Test-Path -LiteralPath $package)) { throw "Matching release package not found: $package" }
$staging = Join-Path ([IO.Path]::GetTempPath()) ([guid]::NewGuid().ToString())
try {
    Expand-Archive -LiteralPath $package -DestinationPath $staging
    $payload = Join-Path $staging "$archiveRoot\dropins\capella-beautifier"
    if (-not (Test-Path -LiteralPath "$payload\eclipse\plugins\capella-beautify-$version.jar")) { throw 'Incomplete release package.' }
    $backup = Join-Path $CapellaDir ('backup\beautifier-' + [guid]::NewGuid().ToString())
    New-Item -ItemType Directory -Path $backup -Force | Out-Null
    New-Item -ItemType Directory -Path "$CapellaDir\dropins" -Force | Out-Null
    if (Test-Path -LiteralPath "$CapellaDir\dropins\capella-beautifier") {
        Move-Item -LiteralPath "$CapellaDir\dropins\capella-beautifier" -Destination $backup
    }
    foreach ($folder in @('dropins', 'plugins')) {
        Get-ChildItem -LiteralPath "$CapellaDir\$folder" -Recurse -File -Filter 'capella-beautify-*.jar' | ForEach-Object {
            $relative = $_.FullName.Substring($CapellaDir.TrimEnd('\').Length + 1)
            $destination = Join-Path $backup $relative
            New-Item -ItemType Directory -Path (Split-Path $destination -Parent) -Force | Out-Null
            Move-Item -LiteralPath $_.FullName -Destination $destination
        }
    }
    Copy-Item -LiteralPath $payload -Destination "$CapellaDir\dropins" -Recurse
    Write-Output "Installed Beautifier $version for Capella $capellaVersion. Backup: $backup"
    Write-Output 'Restart Capella. Use -clean before -vmargs on the first launch if necessary.'
} finally {
    if (Test-Path -LiteralPath $staging) { Remove-Item -LiteralPath $staging -Recurse -Force }
}
