@echo off
setlocal
cd /d "%~dp0"
echo ==== ClickGUI mod builder ====
where java >nul 2>nul || (echo [!] Java not found. Install JDK 21: https://adoptium.net/ & pause & exit /b 1)
powershell -NoProfile -ExecutionPolicy Bypass -File "tools\setup.ps1" || (echo [!] Setup failed, check internet connection & pause & exit /b 1)
call ".gradle-dist\gradle-9.2.1\bin\gradle.bat" build --no-daemon || (echo [!] Build failed. Copy the error above and send it to Karen. & pause & exit /b 1)
if not exist out mkdir out
for %%f in (build\libs\*.jar) do (echo %%~nf | findstr /i sources >nul || copy /y "%%f" out\ >nul)
echo.
echo DONE! Your mod is in the "out" folder.
echo Put it into .minecraft\mods together with Fabric API.
pause
