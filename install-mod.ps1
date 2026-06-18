param(
    [string]$ModsDir,
    [switch]$SkipBuild,
    [switch]$NoVersionBump
)

$ErrorActionPreference = "Stop"

function Read-Properties([string]$Path) {
    $result = [ordered]@{}
    Get-Content -LiteralPath $Path | ForEach-Object {
        $line = $_.Trim()
        if ($line.Length -eq 0 -or $line.StartsWith("#") -or -not $line.Contains("=")) {
            return
        }
        $parts = $line.Split("=", 2)
        $result[$parts[0].Trim()] = $parts[1].Trim()
    }
    return $result
}

function Write-PropertyValue([string]$Path, [string]$Name, [string]$Value) {
    $lines = Get-Content -LiteralPath $Path
    $updated = $false
    $newLines = foreach ($line in $lines) {
        if ($line -match "^\s*$([regex]::Escape($Name))\s*=") {
            $updated = $true
            "$Name=$Value"
        }
        else {
            $line
        }
    }
    if (-not $updated) {
        $newLines += "$Name=$Value"
    }
    Set-Content -LiteralPath $Path -Value $newLines -Encoding UTF8
}

function Get-BumpedVersion([string]$Version) {
    $match = [regex]::Match($Version, "^(.*?)(\d+)([^\d]*)$")
    if (-not $match.Success) {
        return "$Version.1"
    }
    $number = [int]$match.Groups[2].Value
    return "$($match.Groups[1].Value)$($number + 1)$($match.Groups[3].Value)"
}

function Read-LocalEnv([string]$Path) {
    $envValues = @{}
    if (-not (Test-Path -LiteralPath $Path)) {
        return $envValues
    }
    Get-Content -LiteralPath $Path | ForEach-Object {
        $line = $_.Trim()
        if ($line.Length -eq 0 -or $line.StartsWith("#") -or -not $line.Contains("=")) {
            return
        }
        $parts = $line.Split("=", 2)
        $envValues[$parts[0].Trim()] = $parts[1].Trim()
    }
    return $envValues
}

function Get-JarMetadata([string]$JarPath) {
    Add-Type -AssemblyName System.IO.Compression.FileSystem
    $zip = [System.IO.Compression.ZipFile]::OpenRead($JarPath)
    try {
        $entry = $zip.GetEntry("META-INF/neoforge.mods.toml")
        if ($null -eq $entry) {
            return ""
        }
        $reader = New-Object System.IO.StreamReader($entry.Open())
        try {
            return $reader.ReadToEnd()
        }
        finally {
            $reader.Dispose()
        }
    }
    finally {
        $zip.Dispose()
    }
}

$ProjectRoot = Split-Path -Parent $MyInvocation.MyCommand.Path
$GradleProperties = Join-Path $ProjectRoot "gradle.properties"
$LocalEnv = Read-LocalEnv (Join-Path $ProjectRoot ".codex\local.env")
$Props = Read-Properties $GradleProperties

$ModId = $Props["mod_id"]
$PreviousVersion = $Props["mod_version"]
if ([string]::IsNullOrWhiteSpace($ModId) -or [string]::IsNullOrWhiteSpace($PreviousVersion)) {
    throw "gradle.properties must define mod_id and mod_version."
}

if (-not $NoVersionBump -and -not $SkipBuild) {
    $NewVersion = Get-BumpedVersion $PreviousVersion
    Write-PropertyValue $GradleProperties "mod_version" $NewVersion
}
else {
    $NewVersion = $PreviousVersion
}

if ([string]::IsNullOrWhiteSpace($ModsDir)) {
    if ($env:CODEX_MINECRAFT_MODS_DIR) {
        $ModsDir = $env:CODEX_MINECRAFT_MODS_DIR
    }
    elseif ($LocalEnv.ContainsKey("CODEX_MINECRAFT_MODS_DIR")) {
        $ModsDir = $LocalEnv["CODEX_MINECRAFT_MODS_DIR"]
    }
    else {
        $ModsDir = "C:\Users\Emmanuel Tremblay\AppData\Roaming\PrismLauncher\instances\1.21.1 TesT LaB\minecraft\mods"
    }
}

$ResolvedModsDir = (Resolve-Path -LiteralPath $ModsDir -ErrorAction SilentlyContinue)
if ($null -eq $ResolvedModsDir) {
    throw "Mods directory does not exist: $ModsDir"
}
$ModsDir = $ResolvedModsDir.Path

if (-not $SkipBuild) {
    Push-Location $ProjectRoot
    try {
        & .\gradlew.bat clean build
        if ($LASTEXITCODE -ne 0) {
            throw "Gradle build failed with exit code $LASTEXITCODE"
        }
    }
    finally {
        Pop-Location
    }
}

$BuildLibs = Join-Path $ProjectRoot "build\libs"
if (-not (Test-Path -LiteralPath $BuildLibs)) {
    throw "Missing build libs directory: $BuildLibs"
}

$Excluded = "sources|javadoc|dev|plain|test|tests|api"
$Candidates = @(Get-ChildItem -LiteralPath $BuildLibs -Filter "*.jar" |
    Where-Object { $_.Name -like "$ModId*.jar" -and $_.BaseName -notmatch $Excluded } |
    Sort-Object LastWriteTime -Descending)

if ($Candidates.Count -eq 0) {
    throw "No runtime jar candidate found in $BuildLibs"
}

$BuiltJar = $Candidates[0].FullName
$Metadata = Get-JarMetadata $BuiltJar
if ($Metadata -notmatch "modId\s*=\s*`"$([regex]::Escape($ModId))`"" -or $Metadata -notmatch "version\s*=\s*`"$([regex]::Escape($NewVersion))`"") {
    throw "Built jar metadata does not contain expected mod id $ModId and version $NewVersion."
}

$OldJars = @(Get-ChildItem -LiteralPath $ModsDir -Filter "$ModId*.jar" -File)
$DeletedOldJars = @()
foreach ($jar in $OldJars) {
    $jarMetadata = Get-JarMetadata $jar.FullName
    if ($jarMetadata -match "modId\s*=\s*`"$([regex]::Escape($ModId))`"") {
        $DeletedOldJars += $jar.FullName
        Remove-Item -LiteralPath $jar.FullName -Force
    }
}

$TargetJar = Join-Path $ModsDir (Split-Path -Leaf $BuiltJar)
Copy-Item -LiteralPath $BuiltJar -Destination $TargetJar -Force

$SourceHash = (Get-FileHash -Algorithm SHA256 -LiteralPath $BuiltJar).Hash
$TargetHash = (Get-FileHash -Algorithm SHA256 -LiteralPath $TargetJar).Hash
$SourceItem = Get-Item -LiteralPath $BuiltJar
$TargetItem = Get-Item -LiteralPath $TargetJar
$Remaining = @(Get-ChildItem -LiteralPath $ModsDir -Filter "$ModId*.jar" -File | Where-Object {
    (Get-JarMetadata $_.FullName) -match "modId\s*=\s*`"$([regex]::Escape($ModId))`""
})

$Report = [ordered]@{
    ModId = $ModId
    PreviousVersion = $PreviousVersion
    InstalledVersion = $NewVersion
    BuiltJar = $BuiltJar
    InstalledJar = $TargetJar
    DeletedOldJars = $DeletedOldJars
    SourceSize = $SourceItem.Length
    InstalledSize = $TargetItem.Length
    SourceSha256 = $SourceHash
    InstalledSha256 = $TargetHash
    HashesMatch = ($SourceHash -eq $TargetHash)
    RemainingJarsForMod = $Remaining.Count
    OnlyInstalledJarRemains = ($Remaining.Count -eq 1 -and $Remaining[0].FullName -eq $TargetJar)
    MetadataContainsModId = ($Metadata -match "modId\s*=\s*`"$([regex]::Escape($ModId))`"")
    MetadataContainsVersion = ($Metadata -match "version\s*=\s*`"$([regex]::Escape($NewVersion))`"")
    MetadataContainsJourneyMapRequired = ($Metadata -match "modId\s*=\s*`"journeymap`"[\s\S]*?type\s*=\s*`"required`"")
    MetadataContainsIERequired = ($Metadata -match "modId\s*=\s*`"immersiveengineering`"[\s\S]*?type\s*=\s*`"required`"")
    MetadataContainsIPRequired = ($Metadata -match "modId\s*=\s*`"immersivepetroleum`"[\s\S]*?type\s*=\s*`"required`"")
}

$ReportDir = Join-Path $ProjectRoot "build"
New-Item -ItemType Directory -Force -Path $ReportDir | Out-Null
$ReportPath = Join-Path $ReportDir "install-report.json"
$Report | ConvertTo-Json -Depth 5 | Set-Content -LiteralPath $ReportPath -Encoding UTF8

if (-not $Report.HashesMatch -or -not $Report.OnlyInstalledJarRemains) {
    throw "Install verification failed. See $ReportPath"
}

$Report
