@echo off
echo 🚀 Starting batch upload to Modrinth...

set VERSIONS=mc1.20 mc1.20.1 mc1.20.2 mc1.20.3 mc1.20.4 mc1.20.5 mc1.20.6 mc1.21 mc1.21.1 mc1.21.2 mc1.21.3 mc1.21.4 mc1.21.5 mc1.21.6 mc1.21.7 mc1.21.8 mc1.21.9 mc1.21.10 mc1.21.11

for %%v in (%VERSIONS%) do (
    echo.
    echo 📦 Processing version: %%v
    call gradlew :%%v:modrinth --no-configuration-cache
    if %errorlevel% neq 0 (
        echo ❌ Failed on %%v. Stopping.
        pause
        exit /b 1
    )
)

echo.
echo ✅ All versions uploaded successfully!
pause