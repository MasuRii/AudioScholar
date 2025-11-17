@echo off
setlocal enabledelayedexpansion

echo ==========================================
echo AudioScholar Spring Boot Launcher
echo ==========================================
echo.

REM Check if .env file exists
if not exist ".env" (
    echo WARNING: .env file not found in current directory!
    echo Make sure you are running this from the project root directory.
    echo.
)

REM Load .env file and export all variables
echo Loading .env file...
if exist ".env" (
    for /f "usebackq tokens=1,2 delims==" %%i in (".env") do (
        REM Skip comments and empty lines
        if not "%%i"=="" if not "%%i:~0,1"=="#" (
            if not "%%j"=="" (
                REM Set environment variable for current session
                set "%%i=%%j"
                REM Export to system environment
                setx "%%i" "%%j" >nul 2>&1
                echo   Set: %%i=%%j
            )
        )
    )
) else (
    echo No .env file found. Using system environment variables.
)

echo.
echo Starting Spring Boot application...
echo ==========================================
echo.

REM Run Spring Boot application with local profile
mvn spring-boot:run -Dspring-boot.run.profiles=local

echo.
echo Application stopped.
pause