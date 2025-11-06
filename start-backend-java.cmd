@echo off
setlocal

REM === Backend Java environment (edit these if needed) ===
REM Values mapped from Backend/.env:
REM DATABASE_URL="postgres://postgres:0703@localhost:5432/RealTech"
if "%SPRING_DATASOURCE_URL%"=="" set SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/RealTech
if "%SPRING_DATASOURCE_USERNAME%"=="" set SPRING_DATASOURCE_USERNAME=postgres
if "%SPRING_DATASOURCE_PASSWORD%"=="" set SPRING_DATASOURCE_PASSWORD=0703
if "%JWT_SECRET%"=="" set JWT_SECRET=RealTech2024SecureJWTKeyForProductionUseOnly123456789!
if "%UPLOADS_DIR%"=="" set "UPLOADS_DIR=%~dp0Backend\uploads"

cd /d "%~dp0"

echo ========================================
echo Starting Backend Java (Spring Boot)
echo ========================================
echo.
echo Configuration:
echo   SPRING_DATASOURCE_URL=%SPRING_DATASOURCE_URL%
echo   SPRING_DATASOURCE_USERNAME=%SPRING_DATASOURCE_USERNAME%
echo   UPLOADS_DIR=%UPLOADS_DIR%
echo.

REM Set Java 17 for compatibility
set "JAVA_HOME=C:\Program Files\Eclipse Adoptium\jdk-17.0.17.10-hotspot"
set "PATH=%JAVA_HOME%\bin;%PATH%"

echo Building and running with Gradle (Java 17)...
cd /d "%~dp0BackendJava"
call gradlew-java17.bat bootRun

endlocal
