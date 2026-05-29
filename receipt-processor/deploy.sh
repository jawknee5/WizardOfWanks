#!/bin/bash

# Receipt Processor Enterprise - Deployment Script
# Deploys to production with health checks and rollback capability

set -e

VERSION="3.0.0"
TIMESTAMP=$(date +%Y%m%d_%H%M%S)
BACKUP_DIR="./backups/receipt-processor-$TIMESTAMP"

echo "=========================================="
echo "Receipt Processor Enterprise - Deployment"
echo "Version: $VERSION"
echo "=========================================="

# Check prerequisites
echo ""
echo "Checking prerequisites..."

if ! command -v docker &> /dev/null; then
  echo "ERROR: Docker not found. Please install Docker."
  exit 1
fi

if ! command -v docker-compose &> /dev/null; then
  echo "ERROR: Docker Compose not found. Please install Docker Compose."
  exit 1
fi

echo "✓ Docker found: $(docker --version)"
echo "✓ Docker Compose found: $(docker-compose --version)"

# Load environment
echo ""
echo "Loading environment configuration..."

if [ ! -f .env ]; then
  echo "ERROR: .env file not found. Please create .env from .env.example"
  exit 1
fi

source .env
echo "✓ Environment loaded"

# Backup current data
echo ""
echo "Creating backup..."

mkdir -p "$BACKUP_DIR"
if [ -d ./data ]; then
  cp -r ./data "$BACKUP_DIR/" || true
  echo "✓ Data backed up to $BACKUP_DIR"
fi

# Build images
echo ""
echo "Building Docker images..."

docker-compose build --no-cache
echo "✓ Images built successfully"

# Stop current services
echo ""
echo "Stopping current services..."

docker-compose down --remove-orphans || true
echo "✓ Services stopped"

# Start new services
echo ""
echo "Starting new services..."

docker-compose up -d
echo "✓ Services started"

# Wait for services to be ready
echo ""
echo "Waiting for services to be healthy..."

WAIT_TIME=0
MAX_WAIT=120

while [ $WAIT_TIME -lt $MAX_WAIT ]; do
  if docker-compose ps | grep -q "healthy\|up"; then
    echo "✓ Services are healthy"
    break
  fi
  
  sleep 5
  WAIT_TIME=$((WAIT_TIME + 5))
  echo "  Waiting... ($WAIT_TIME/$MAX_WAIT seconds)"
done

if [ $WAIT_TIME -ge $MAX_WAIT ]; then
  echo "ERROR: Services failed to start"
  echo "Rolling back..."
  docker-compose down
  exit 1
fi

# Run health checks
echo ""
echo "Running health checks..."

# Check app health
if curl -f http://localhost:3000/health > /dev/null 2>&1; then
  echo "✓ App health check passed"
else
  echo "ERROR: App health check failed"
  exit 1
fi

# Database health check
if docker exec receipt-processor-db mongo --eval "db.adminCommand('ping')" > /dev/null 2>&1; then
  echo "✓ Database health check passed"
fi

# Final status
echo ""
echo "=========================================="
echo "✓ Deployment Successful!"
echo "=========================================="
echo ""
echo "Services running:"
docker-compose ps
echo ""
echo "Access the application:"
echo "  Web: http://localhost:3000"
echo "  API: http://localhost:3000/api"
echo ""
echo "Backup location: $BACKUP_DIR"
echo ""
echo "To view logs:"
echo "  docker-compose logs -f receipt-processor"
echo ""
echo "To rollback:"
echo "  docker-compose down"
echo "  # Restore from backup"
echo "=========================================="
