<#
.SYNOPSIS
  Build (optional) and deploy the ToDo app to an Android emulator:
    1. Start the emulator if none is already running.
    2. Install (push) the APK to the running emulator.

.PARAMETER AvdName
  Name of the AVD to start if no emulator is already running.
  Defaults to "Pixel_7a_API_34". Run `emulator -list-avds` to see other options
  (e.g. Automotive_1080p_landscape_API_33, SafeCall_API34).

.PARAMETER ApkPath
  Path to an already-built APK to install. If omitted, the script builds the
  debug APK via gradlew and uses app\build\outputs\apk\debug\app-debug.apk.

.PARAMETER SkipBuild
  Skip the gradlew build step and install the APK at -ApkPath (or the default
  debug APK path) as-is.

.EXAMPLE
  .\scripts\run-android.ps1

.EXAMPLE
  .\scripts\run-android.ps1 -AvdName SafeCall_API34

.EXAMPLE
  .\scripts\run-android.ps1 -SkipBuild -ApkPath .\app\build\outputs\apk\debug\app-debug.apk
#>

param(
    [string]$AvdName = "Pixel_7a_API_34",
    [string]$ApkPath,
    [switch]$SkipBuild
)

$ErrorActionPreference = "Stop"

$AndroidDir = Split-Path -Parent $PSScriptRoot

function Get-SdkDir {
    $localProps = Join-Path $AndroidDir "local.properties"
    if (Test-Path $localProps) {
        $line = Get-Content $localProps | Where-Object { $_ -match "^sdk\.dir=" }
        if ($line) {
            $raw = $line -replace "^sdk\.dir=", ""
            $raw = $raw -replace "\\:", ":"
            $raw = $raw -replace "\\\\", "\"
            return $raw
        }
    }
    if ($env:ANDROID_HOME) { return $env:ANDROID_HOME }
    if ($env:ANDROID_SDK_ROOT) { return $env:ANDROID_SDK_ROOT }
    throw "Could not resolve Android SDK dir (checked local.properties, ANDROID_HOME, ANDROID_SDK_ROOT)."
}

$SdkDir = Get-SdkDir
$Adb = Join-Path $SdkDir "platform-tools\adb.exe"
$Emulator = Join-Path $SdkDir "emulator\emulator.exe"

if (-not (Test-Path $Adb)) { throw "adb not found at $Adb" }
if (-not (Test-Path $Emulator)) { throw "emulator not found at $Emulator" }

# Force ANDROID_SDK_ROOT/ANDROID_HOME to the resolved SDK dir so the emulator
# doesn't fall back to a different SDK install (e.g. ANDROID_HOME pointing at
# a separate, incomplete SDK without this AVD's system image).
$env:ANDROID_SDK_ROOT = $SdkDir
$env:ANDROID_HOME = $SdkDir

# ---------------------------------------------------------------------------
# 1) Start emulator if none is running
# ---------------------------------------------------------------------------

function Get-RunningEmulatorSerial {
    $devices = & $Adb devices
    $match = $devices | Select-String "^(emulator-\d+)\s+device$"
    if ($match) { return $match.Matches[0].Groups[1].Value }
    return $null
}

$serial = Get-RunningEmulatorSerial

if (-not $serial) {
    Write-Host "No running emulator found. Starting AVD '$AvdName'..."

    $avdList = & $Emulator -list-avds
    if ($avdList -notcontains $AvdName) {
        throw "AVD '$AvdName' not found. Available AVDs: $($avdList -join ', ')"
    }

    Start-Process -FilePath $Emulator -ArgumentList @("-avd", $AvdName, "-netdelay", "none", "-netspeed", "full") -WindowStyle Minimized

    Write-Host "Waiting for emulator device to attach..."
    & $Adb wait-for-device

    $serial = Get-RunningEmulatorSerial
    $retries = 0
    while (-not $serial -and $retries -lt 30) {
        Start-Sleep -Seconds 2
        $serial = Get-RunningEmulatorSerial
        $retries++
    }
    if (-not $serial) { throw "Emulator did not come online in time." }

    Write-Host "Waiting for Android boot to complete on $serial..."
    $booted = $false
    for ($i = 0; $i -lt 60; $i++) {
        $prop = (& $Adb -s $serial shell getprop sys.boot_completed 2>$null).Trim()
        if ($prop -eq "1") { $booted = $true; break }
        Start-Sleep -Seconds 2
    }
    if (-not $booted) { throw "Emulator boot did not complete in time." }

    Write-Host "Emulator '$AvdName' is up ($serial)."
} else {
    Write-Host "Emulator already running ($serial)."
}

# ---------------------------------------------------------------------------
# 2) Build (optional) and install the APK
# ---------------------------------------------------------------------------

if (-not $ApkPath) {
    $ApkPath = Join-Path $AndroidDir "app\build\outputs\apk\debug\app-debug.apk"
}

if (-not $SkipBuild) {
    Write-Host "Building debug APK..."
    Push-Location $AndroidDir
    try {
        & "$AndroidDir\gradlew.bat" assembleDebug
        if ($LASTEXITCODE -ne 0) { throw "gradlew assembleDebug failed with exit code $LASTEXITCODE" }
    } finally {
        Pop-Location
    }
}

if (-not (Test-Path $ApkPath)) {
    throw "APK not found at $ApkPath"
}

Write-Host "Installing $ApkPath on $serial..."
& $Adb -s $serial install -r $ApkPath
if ($LASTEXITCODE -ne 0) { throw "adb install failed with exit code $LASTEXITCODE" }

Write-Host "Done. App installed on emulator $serial."
