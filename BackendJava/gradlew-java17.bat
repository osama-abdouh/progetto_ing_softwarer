@echo off
REM Script per eseguire Gradle con Java 17

set "JAVA_HOME=C:\Program Files\Eclipse Adoptium\jdk-17.0.17.10-hotspot"

if not exist "%JAVA_HOME%\bin\java.exe" (
    echo ERRORE: Java 17 non trovato in %JAVA_HOME%
    echo Installare Java 17 da https://adoptium.net/temurin/releases/?version=17
    pause
    exit /b 1
)

set "PATH=%JAVA_HOME%\bin;%PATH%"

echo ========================================
echo Usando Java 17
echo ========================================
"%JAVA_HOME%\bin\java.exe" -version
echo.

echo Eseguendo Gradle...
call "%~dp0gradlew.bat" %*
