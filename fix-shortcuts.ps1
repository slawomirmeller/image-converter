# Fix shortcuts for already installed application
# Run this script as Administrator to fix existing shortcuts

$ErrorActionPreference = "Stop"

Write-Host "=== Fixing Image to WebP Converter Shortcuts ===" -ForegroundColor Cyan
Write-Host ""

# Check for admin rights
$currentPrincipal = New-Object Security.Principal.WindowsPrincipal([Security.Principal.WindowsIdentity]::GetCurrent())
$isAdmin = $currentPrincipal.IsInRole([Security.Principal.WindowsBuiltInRole]::Administrator)

if (-not $isAdmin) {
    Write-Host "ERROR: This script requires administrator privileges." -ForegroundColor Red
    Write-Host "Please right-click and select 'Run as Administrator'" -ForegroundColor Yellow
    pause
    exit 1
}

$installDir = "$env:ProgramFiles\ImageToWebPConverter"
$startMenuDir = "$env:ProgramData\Microsoft\Windows\Start Menu\Programs\Image Tools"
$desktopShortcut = "$env:PUBLIC\Desktop\Image to WebP Converter.lnk"
$exePath = Join-Path $installDir "ImageToWebPConverter.exe"

# Check if application is installed
if (-not (Test-Path $exePath)) {
    Write-Host "ERROR: Application not found at: $exePath" -ForegroundColor Red
    Write-Host "Please make sure the application is installed." -ForegroundColor Yellow
    pause
    exit 1
}

Write-Host "Found application at: $exePath" -ForegroundColor Green
Write-Host ""

# Function to create/fix shortcut
function Fix-Shortcut {
    param(
        [string]$ShortcutPath,
        [string]$TargetPath,
        [string]$WorkingDirectory,
        [string]$Description
    )
    
    try {
        # Create directory if it doesn't exist
        $shortcutDir = Split-Path $ShortcutPath -Parent
        if (-not (Test-Path $shortcutDir)) {
            New-Item -ItemType Directory -Path $shortcutDir -Force | Out-Null
        }
        
        # Create the shortcut
        $WshShell = New-Object -ComObject WScript.Shell
        $Shortcut = $WshShell.CreateShortcut($ShortcutPath)
        $Shortcut.TargetPath = $TargetPath
        $Shortcut.WorkingDirectory = $WorkingDirectory
        $Shortcut.IconLocation = "$TargetPath,0"
        $Shortcut.Description = $Description
        $Shortcut.Save()
        
        Write-Host "  [OK] Created/fixed: $ShortcutPath" -ForegroundColor Green
        return $true
    }
    catch {
        Write-Host "  [FAILED] $ShortcutPath - $($_.Exception.Message)" -ForegroundColor Red
        return $false
    }
}

# Fix Start Menu shortcut
Write-Host "Fixing Start Menu shortcut..." -ForegroundColor Yellow
$startMenuShortcut = Join-Path $startMenuDir "Image to WebP Converter.lnk"
Fix-Shortcut -ShortcutPath $startMenuShortcut `
             -TargetPath $exePath `
             -WorkingDirectory $installDir `
             -Description "Convert images to WebP format"

# Fix Desktop shortcut
Write-Host "Fixing Desktop shortcut..." -ForegroundColor Yellow
Fix-Shortcut -ShortcutPath $desktopShortcut `
             -TargetPath $exePath `
             -WorkingDirectory $installDir `
             -Description "Convert images to WebP format"

Write-Host ""
Write-Host "=== Shortcuts Fixed! ===" -ForegroundColor Green
Write-Host ""
Write-Host "You can now use:" -ForegroundColor Cyan
Write-Host "  - Start Menu: Image Tools > Image to WebP Converter" -ForegroundColor White
Write-Host "  - Desktop shortcut" -ForegroundColor White
Write-Host ""
pause
