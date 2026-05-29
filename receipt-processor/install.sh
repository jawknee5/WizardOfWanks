#!/bin/bash

# Receipt Processor Enterprise - 1-Click Installer
# Works on macOS, Linux, Windows (via WSL/Cygwin)

set -e

echo "=========================================="
echo "Receipt Processor Enterprise - Setup"
echo "=========================================="

# Check Node.js
if ! command -v node &> /dev/null; then
  echo "❌ Node.js not found. Please install Node.js 16+"
  exit 1
fi

echo "✅ Node.js version: $(node --version)"

# Check npm
if ! command -v npm &> /dev/null; then
  echo "❌ npm not found"
  exit 1
fi

echo "✅ npm version: $(npm --version)"

# Install dependencies
echo ""
echo "📦 Installing dependencies..."
npm install

# Build for all platforms (optional)
if [ "$1" == "--build-all" ]; then
  echo ""
  echo "🔨 Building installers for all platforms..."
  npm run electron-build
  echo "✅ Installers built in /dist folder"
fi

# Build web version
echo ""
echo "🌐 Building web version..."
npm run build

echo ""
echo "=========================================="
echo "✅ Setup Complete!"
echo "=========================================="
echo ""
echo "To start development:"
echo "  npm run dev"
echo ""
echo "To build production:"
echo "  npm run electron-build"
echo ""
echo "To start web server:"
echo "  npm start"
echo ""
echo "Installers will be in /dist folder"
echo "========================================"
