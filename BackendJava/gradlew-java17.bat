@echo off
REM Script per eseguire Gradle con Java 17

set "JAVA_HOME=C:\Program Files\Eclipse Adoptium\jdk-17.0.17.10-hotspot"
set "PATH=%JAVA_HOME%\bin;%PATH%"

echo Usando Java 17 da: %JAVA_HOME%
java -version

echo.
echo Eseguendo Gradle...
gradlew.bat %*
