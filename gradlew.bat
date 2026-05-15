@echo off
setlocal
set DIR=%~dp0

if exist "%DIR%gradle\wrapper\gradle-wrapper.jar" (
  java -classpath "%DIR%gradle\wrapper\gradle-wrapper.jar" org.gradle.wrapper.GradleWrapperMain %*
  exit /b %errorlevel%
)

where gradle >nul 2>nul
if %errorlevel% equ 0 (
  gradle -p "%DIR%" %*
  exit /b %errorlevel%
)

echo Gradle wrapper jar is missing and gradle is not installed.
exit /b 1
