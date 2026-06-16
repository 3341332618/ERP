@echo off
setlocal
set BASE_DIR=%~dp0
set WRAPPER_DIR=%BASE_DIR%.mvn\wrapper
set MAVEN_VERSION=3.9.9
set MAVEN_HOME=%WRAPPER_DIR%\apache-maven-%MAVEN_VERSION%
set MAVEN_CMD=%MAVEN_HOME%\bin\mvn.cmd
set MAVEN_ZIP=%WRAPPER_DIR%\apache-maven-%MAVEN_VERSION%-bin.zip
set MAVEN_URL=https://archive.apache.org/dist/maven/maven-3/%MAVEN_VERSION%/binaries/apache-maven-%MAVEN_VERSION%-bin.zip

if not exist "%MAVEN_CMD%" (
  if not exist "%WRAPPER_DIR%" mkdir "%WRAPPER_DIR%"
  powershell -NoProfile -ExecutionPolicy Bypass -Command ^
    "$ErrorActionPreference='Stop';" ^
    "$zip='%MAVEN_ZIP%';" ^
    "$url='%MAVEN_URL%';" ^
    "if (!(Test-Path $zip)) { Invoke-WebRequest -Uri $url -OutFile $zip };" ^
    "Expand-Archive -LiteralPath $zip -DestinationPath '%WRAPPER_DIR%' -Force"
)

"%MAVEN_CMD%" %*
endlocal

