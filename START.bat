@echo off
REM WizardOfWanks Enterprise - Startup Script
REM Location: H:\WizardOfWanks\

echo ==========================================
echo    WizardOfWanks Enterprise v2.0.0
echo ==========================================
echo.

REM Check if running from correct directory
if not exist "enterprise\package.json" (
  echo Error: Please run this from H:\WizardOfWanks directory
  exit /b 1
)

echo Starting WizardOfWanks services...
echo.

REM Start docker-compose
cd /d "%~dp0"
docker-compose -f docker-compose.wiz.yml up -d

echo.
echo ==========================================
echo    Services Starting...
echo ==========================================
echo.
echo Wait 30 seconds for services to be healthy
echo.
echo Access the application at:
echo   http://localhost:5000
echo.
echo Demo Login:
echo   Email: admin@wizardofwanks.com
echo   Password: password123
echo.
echo To view logs:
echo   docker-compose -f docker-compose.wiz.yml logs -f
echo.
echo To stop:
echo   docker-compose -f docker-compose.wiz.yml down
echo.
echo ==========================================
pause
