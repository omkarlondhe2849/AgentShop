@REM Maven Wrapper batch script
@REM Auto-downloads Maven if not available

@echo off
set MAVEN_PROJECTBASEDIR=%~dp0
set WRAPPER_JAR="%MAVEN_PROJECTBASEDIR%.mvn\wrapper\maven-wrapper.jar"

if exist %WRAPPER_JAR% (
    java -jar %WRAPPER_JAR% %*
) else (
    echo Maven wrapper JAR not found. Please run: mvn wrapper:wrapper
    exit /b 1
)
