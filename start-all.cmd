@echo off
setlocal

REM Optionally predefine env here (else start-backend-java.cmd has safe defaults)
REM set SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/postgres
REM set SPRING_DATASOURCE_USERNAME=postgres
REM set SPRING_DATASOURCE_PASSWORD=postgres
REM set JWT_SECRET=this-is-a-long-secret-key-change-me-32chars-min
REM set "UPLOADS_DIR=%~dp0Backend\uploads"

cd /d "%~dp0"

echo Launching Backend Java and Frontend in separate windows...
start "BackendJava" cmd /k "%~dp0start-backend-java.cmd"
REM small delay to let backend boot logs start showing
timeout /t 2 /nobreak >nul
start "Frontend" cmd /k "%~dp0start-frontend.cmd"

echo.
echo ========================================
echo Backend Java e Frontend avviati!
echo ========================================
echo.
echo Backend Java: http://localhost:8080
echo Frontend:     http://localhost:4200
echo.
echo Premi un tasto per chiudere questa finestra...
pause >nul

endlocal
