@echo off
REM Divine Corner Backend - Docker Build Script (Windows)

echo ======================================
echo Divine Corner Backend - Docker Build
echo ======================================
echo.

REM Check if Docker is running
docker version >nul 2>&1
if errorlevel 1 (
    echo Error: Docker is not running or not installed
    exit /b 1
)

echo Step 1: Building Docker image...
docker build -t divine-corner-backend:latest .

if errorlevel 1 (
    echo Build failed!
    exit /b 1
)

echo.
echo Build successful!
echo.
echo Step 2: Image information
docker images divine-corner-backend:latest

echo.
echo ======================================
echo Build complete!
echo ======================================
echo.
echo To run the container:
echo   docker run -p 8080:8080 divine-corner-backend:latest
echo.
echo To run with docker-compose:
echo   docker-compose up
echo.
echo To push to a registry:
echo   docker tag divine-corner-backend:latest your-registry/divine-corner-backend:latest
echo   docker push your-registry/divine-corner-backend:latest
echo.

pause
