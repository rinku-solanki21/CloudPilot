@REM ----------------------------------------------------------------------------
@REM Maven Wrapper script for Windows (CloudPilot)
@REM ----------------------------------------------------------------------------

@echo off
setlocal

set "DIRNAME=%~dp0"
if "%DIRNAME%" == "" set "DIRNAME=."

REM Check if IntelliJ IDEA bundled Maven or JBR is available
if defined JAVA_HOME goto findMaven
if exist "C:\Program Files\JetBrains\IntelliJ IDEA 2026.2.1\jbr" (
    set "JAVA_HOME=C:\Program Files\JetBrains\IntelliJ IDEA 2026.2.1\jbr"
)

:findMaven
if exist "C:\Program Files\JetBrains\IntelliJ IDEA 2026.2.1\plugins\maven-plugin\lib\maven3\bin\mvn.cmd" (
    "C:\Program Files\JetBrains\IntelliJ IDEA 2026.2.1\plugins\maven-plugin\lib\maven3\bin\mvn.cmd" %*
    goto end
)

REM Fallback to system mvn
mvn %*

:end
endlocal
