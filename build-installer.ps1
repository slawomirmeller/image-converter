# Build script for creating Windows installer
# Requires JDK 17+ with jpackage

$ErrorActionPreference = "Stop"

$projectDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$targetDir = Join-Path $projectDir "target"
# Use external temp directory to avoid jpackage copying itself recursively
$buildDir = "C:\ImageConverterBuild"
$installerDir = Join-Path $buildDir "installer"
$distDir = Join-Path $projectDir "dist"

Write-Host "=== Image to WebP Converter - Windows Installer Builder ===" -ForegroundColor Cyan
Write-Host ""

# Step 1: Build the project with Maven
Write-Host "[1/5] Building project with Maven..." -ForegroundColor Yellow
$mvnPath = "C:\Program Files\JetBrains\IntelliJ IDEA 2022.3\plugins\maven\lib\maven3\bin\mvn.cmd"

if (Test-Path $mvnPath) {
    & $mvnPath clean package -f "$projectDir\pom.xml" -DskipTests
} else {
    mvn clean package -f "$projectDir\pom.xml" -DskipTests
}

if ($LASTEXITCODE -ne 0) {
    Write-Host "Maven build failed!" -ForegroundColor Red
    exit 1
}

Write-Host "Maven build successful!" -ForegroundColor Green
Write-Host ""

# Step 2: Create installer directory
Write-Host "[2/5] Preparing directories..." -ForegroundColor Yellow
if (Test-Path $buildDir) {
    Remove-Item -Path $buildDir -Recurse -Force -ErrorAction SilentlyContinue
}
if (Test-Path $distDir) {
    Remove-Item -Path $distDir -Recurse -Force -ErrorAction SilentlyContinue
}
New-Item -ItemType Directory -Path $installerDir -Force | Out-Null
New-Item -ItemType Directory -Path $distDir -Force | Out-Null

# Step 3: Find JAR file
$jarFile = Get-ChildItem -Path $targetDir -Filter "image-converter-*.jar" |
           Where-Object { $_.Name -notmatch "original" } |
           Select-Object -First 1

if (-not $jarFile) {
    Write-Host "JAR file not found in target directory!" -ForegroundColor Red
    exit 1
}

Write-Host "Found JAR: $($jarFile.Name)" -ForegroundColor Green
Write-Host ""

# Step 4: Create app-image with jpackage
Write-Host "[3/5] Creating application with jpackage..." -ForegroundColor Yellow
Write-Host "This may take a few minutes..." -ForegroundColor Gray

# Create a dedicated input folder with only the JAR file
$inputDir = Join-Path $targetDir "jpackage_input"
if (Test-Path $inputDir) {
    Remove-Item -Path $inputDir -Recurse -Force
}
New-Item -ItemType Directory -Path $inputDir -Force | Out-Null
Copy-Item -Path $jarFile.FullName -Destination $inputDir

$jpackageArgs = @(
    "--type", "app-image",
    "--input", $inputDir,
    "--dest", $installerDir,
    "--name", "ImageToWebPConverter",
    "--main-jar", $jarFile.Name,
    "--main-class", "com.imageconverter.Launcher",
    "--app-version", "1.0.0",
    "--vendor", "ImageTools",
    "--description", "Convert images to WebP format",
    "--java-options", "-Xmx512m"
)

& jpackage @jpackageArgs

if ($LASTEXITCODE -ne 0) {
    Write-Host "jpackage failed!" -ForegroundColor Red
    exit 1
}

Write-Host "Application created successfully!" -ForegroundColor Green
Write-Host ""

# Step 5: Create installer script
Write-Host "[4/5] Creating installer script..." -ForegroundColor Yellow

$installScript = @'
@echo off
:: Image to WebP Converter - Installer
:: This script installs the application for all users

echo ============================================
echo  Image to WebP Converter - Installer
echo ============================================
echo.

:: Check for admin rights
net session >nul 2>&1
if %errorLevel% neq 0 (
    echo This installer requires administrator privileges.
    echo Please right-click and select "Run as administrator"
    pause
    exit /b 1
)

:: Get the directory where this script is located
set "SCRIPT_DIR=%~dp0"
set "INSTALL_DIR=%ProgramFiles%\ImageToWebPConverter"
set "START_MENU=%ProgramData%\Microsoft\Windows\Start Menu\Programs\Image Tools"
set "DESKTOP_SHORTCUT=%PUBLIC%\Desktop\Image to WebP Converter.lnk"

echo Installing to: %INSTALL_DIR%
echo.

:: Remove old installation if exists
if exist "%INSTALL_DIR%" (
    echo Removing old installation...
    rd /s /q "%INSTALL_DIR%" 2>nul
)

:: Create installation directory
mkdir "%INSTALL_DIR%"

:: Copy all files from script directory
echo Copying files...
xcopy /E /I /Y "%SCRIPT_DIR%ImageToWebPConverter" "%INSTALL_DIR%" >nul

:: Create Start Menu folder
if not exist "%START_MENU%" mkdir "%START_MENU%"

:: Create Start Menu shortcut
echo Creating Start Menu shortcut...
powershell -Command "$ws = New-Object -ComObject WScript.Shell; $s = $ws.CreateShortcut('%START_MENU%\Image to WebP Converter.lnk'); $s.TargetPath = '%INSTALL_DIR%\ImageToWebPConverter.exe'; $s.WorkingDirectory = '%INSTALL_DIR%'; $s.IconLocation = '%INSTALL_DIR%\ImageToWebPConverter.exe,0'; $s.Description = 'Convert images to WebP format'; $s.Save()"

:: Create Desktop shortcut
echo Creating Desktop shortcut...
powershell -Command "$ws = New-Object -ComObject WScript.Shell; $s = $ws.CreateShortcut('%DESKTOP_SHORTCUT%'); $s.TargetPath = '%INSTALL_DIR%\ImageToWebPConverter.exe'; $s.WorkingDirectory = '%INSTALL_DIR%'; $s.IconLocation = '%INSTALL_DIR%\ImageToWebPConverter.exe,0'; $s.Description = 'Convert images to WebP format'; $s.Save()"

:: Create uninstaller
echo Creating uninstaller...
(
echo @echo off
echo echo Uninstalling Image to WebP Converter...
echo net session ^>nul 2^>^&1
echo if %%errorLevel%% neq 0 ^(
echo     echo Please run as administrator
echo     pause
echo     exit /b 1
echo ^)
echo rd /s /q "%INSTALL_DIR%"
echo del "%START_MENU%\Image to WebP Converter.lnk" 2^>nul
echo rd "%START_MENU%" 2^>nul
echo del "%DESKTOP_SHORTCUT%" 2^>nul
echo echo Uninstallation complete!
echo pause
) > "%INSTALL_DIR%\Uninstall.bat"

echo.
echo ============================================
echo  Installation Complete!
echo ============================================
echo.
echo The application has been installed to:
echo   %INSTALL_DIR%
echo.
echo You can find it in:
echo   - Start Menu: Image Tools / Image to WebP Converter
echo   - Desktop shortcut
echo.
echo To uninstall, run:
echo   %INSTALL_DIR%\Uninstall.bat
echo.
pause
'@

$installScript | Out-File -FilePath (Join-Path $installerDir "Install.bat") -Encoding ASCII

Write-Host "Installer script created!" -ForegroundColor Green
Write-Host ""

# Step 6: Create distribution ZIP
Write-Host "[5/5] Creating distribution package..." -ForegroundColor Yellow

$zipPath = Join-Path $distDir "ImageToWebPConverter-1.0.0-Windows.zip"
Compress-Archive -Path "$installerDir\*" -DestinationPath $zipPath -Force

$zipSize = [math]::Round((Get-Item $zipPath).Length / 1MB, 2)
Write-Host "Distribution package created: $zipPath ($zipSize MB)" -ForegroundColor Green

Write-Host ""
Write-Host "============================================" -ForegroundColor Green
Write-Host "  Build Complete!" -ForegroundColor Green
Write-Host "============================================" -ForegroundColor Green
Write-Host ""
Write-Host "Distribution package ready at:" -ForegroundColor Cyan
Write-Host "  $zipPath" -ForegroundColor White
Write-Host ""
Write-Host "To distribute:" -ForegroundColor Yellow
Write-Host "  1. Share the ZIP file with users" -ForegroundColor White
Write-Host "  2. Users extract the ZIP" -ForegroundColor White
Write-Host "  3. Run Install.bat as Administrator" -ForegroundColor White
Write-Host ""
Write-Host "After installation, the app will appear in:" -ForegroundColor Yellow
Write-Host "  - Start Menu: Image Tools > Image to WebP Converter" -ForegroundColor White
Write-Host "  - Desktop shortcut" -ForegroundColor White
Write-Host ""

# Open the dist directory
explorer $distDir
