@echo off
REM Script per aggiornare la cache dell'IDE e risolvere errori fantasma

echo Pulizia cache Gradle...
rmdir /s /q .gradle 2>nul
rmdir /s /q build 2>nul

echo.
echo Ricompilazione progetto...
call gradlew.bat clean compileJava

echo.
echo Riavvia VS Code per vedere le modifiche!
echo Oppure usa: Ctrl+Shift+P -> "Java: Clean Java Language Server Workspace"
pause
