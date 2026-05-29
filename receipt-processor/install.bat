@echo off
REM Receipt Processor Enterprise - 1-Click Windows Installer

echo ==========================================
echo Receipt Processor Enterprise - Setup
echo ==========================================

REM Check Node.js
where node >nul 2>nul
if %errorlevel% neq 0 (
  echo Error: Node.js not found. Please install Node.js 16+
  exit /b 1
)

for /f "tokens=*" %%i in ('node --version') do set NODE_VERSION=%%i
echo Node.js version: %NODE_VERSION%

REM Check npm
where npm >nul 2>nul
if %errorlevel% neq 0 (
  echo Error: npm not found
  exit /b 1
)

for /f "tokens=*" %%i in ('npm --version') do set NPM_VERSION=%%i
echo npm version: %NPM_VERSION%

REM Install dependencies
echo.
echo Installing dependencies...
call npm install

if "%1"=="--build-all" (
  echo.
  echo Building installers for all platforms...
  call npm run electron-build
  echo Installers built in \dist folder
)

REM Build web version
echo.
echo Building web version...
call npm run build

echo.
echo ==========================================
echo Setup Complete!
echo ==========================================
echo.
echo To start development:
echo   npm run dev
echo.
echo To build production:
echo   npm run electron-build
echo.
echo To start web server:
echo   npm start
echo.
echo Installers will be in \dist folder
echo ==========================================
pause
