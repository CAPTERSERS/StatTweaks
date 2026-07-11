@echo off
REM StatTweaks Quick Test Script
REM This script automates the build and setup process for testing

setlocal enabledelayedexpansion

echo.
echo ╔════════════════════════════════════════════════════════════╗
echo ║     StatTweaks - Quick Build & Test Setup Script          ║
echo ╚════════════════════════════════════════════════════════════╝
echo.

REM Get Minecraft directory
set MINECRAFT_DIR=%APPDATA%\.minecraft

echo 📁 Minecraft folder: %MINECRAFT_DIR%
echo.

REM Step 1: Build the mod
echo ▶ Step 1/4: Building mod...
echo.
call gradlew build
if errorlevel 1 (
    echo ❌ Build failed!
    pause
    exit /b 1
)
echo ✅ Build successful!
echo.

REM Step 2: Copy JAR files
echo ▶ Step 2/4: Copying JAR files...
mkdir "%MINECRAFT_DIR%\mods" 2>nul

REM Copy Fabric JAR
if exist "build\libs\fabric-*.jar" (
    for %%f in (build\libs\fabric-*.jar) do (
        copy "%%f" "%MINECRAFT_DIR%\mods\" /Y >nul
        echo ✅ Copied: %%~nf
    )
)

REM Copy NeoForge JAR
if exist "build\libs\neoforge-*.jar" (
    for %%f in (build\libs\neoforge-*.jar) do (
        copy "%%f" "%MINECRAFT_DIR%\mods\" /Y >nul
        echo ✅ Copied: %%~nf
    )
)
echo.

REM Step 3: Create config directory and file
echo ▶ Step 3/4: Setting up configuration...
mkdir "%MINECRAFT_DIR%\config" 2>nul

REM Copy example config
if exist "examples\example-config.json" (
    copy "examples\example-config.json" "%MINECRAFT_DIR%\config\CPT_StatTweaks_Config.json" /Y >nul
    echo ✅ Configuration file created: CPT_StatTweaks_Config.json
) else (
    echo ⚠️  Example config not found, creating minimal config...
    (
        echo {
        echo   "items": {
        echo     "minecraft:diamond_sword": {
        echo       "attributes": {
        echo         "minecraft:generic.attack_damage": 12.0,
        echo         "minecraft:generic.attack_speed": 2.0
        echo       }
        echo     }
        echo   }
        echo }
    ) > "%MINECRAFT_DIR%\config\CPT_StatTweaks_Config.json"
    echo ✅ Minimal configuration created
)
echo.

REM Step 4: Instructions
echo ▶ Step 4/4: Setup complete!
echo.
echo ════════════════════════════════════════════════════════════
echo.
echo 🎮 NEXT STEPS:
echo.
echo 1. Open Minecraft Launcher
echo 2. Select Fabric profile (or NeoForge)
echo 3. Click "Play"
echo 4. Once in game:
echo    - Create a new Creative world
echo    - Run: /give @s minecraft:diamond_sword
echo    - Check the sword damage (should be ~12.0 instead of 7.0)
echo    - Check console for "[STATTWEAKS]" messages
echo.
echo 📁 Files created:
echo    • Mod JAR: %MINECRAFT_DIR%\mods\
echo    • Config:  %MINECRAFT_DIR%\config\CPT_StatTweaks_Config.json
echo.
echo 📚 Documentation:
echo    • Full guide:  TESTING_GUIDE.md
echo    • Config help: CONFIG_GUIDE.md
echo    • Architecture: ARCHITECTURE.md
echo.
echo ════════════════════════════════════════════════════════════
echo.
echo ✅ Ready to test! Press any key to continue...
echo.
pause
