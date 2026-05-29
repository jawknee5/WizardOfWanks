#!/bin/bash

# WizardOfWanks Enterprise - Startup Script
# Location: /path/to/WizardOfWanks/

echo "=========================================="
echo "   WizardOfWanks Enterprise v2.0.0"
echo "=========================================="
echo ""

# Check if running from correct directory
if [ ! -f "enterprise/package.json" ]; then
  echo "Error: Please run this from WizardOfWanks directory"
  exit 1
fi

echo "Starting WizardOfWanks services..."
echo ""

# Start docker-compose
docker-compose -f docker-compose.wiz.yml up -d

echo ""
echo "=========================================="
echo "   Services Starting..."
echo "=========================================="
echo ""
echo "Wait 30 seconds for services to be healthy"
echo ""
echo "Access the application at:"
echo "   http://localhost:5000"
echo ""
echo "Demo Login:"
echo "   Email: admin@wizardofwanks.com"
echo "   Password: password123"
echo ""
echo "To view logs:"
echo "   docker-compose -f docker-compose.wiz.yml logs -f"
echo ""
echo "To stop:"
echo "   docker-compose -f docker-compose.wiz.yml down"
echo ""
echo "=========================================="
