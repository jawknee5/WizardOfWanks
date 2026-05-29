#!/bin/sh

# Test WizardOfWanks API endpoints
BASE_URL="http://localhost:5000"

echo "Testing WizardOfWanks API..."
echo ""

# Test 1: Health check
echo "✓ Health Check:"
wget -q -O - "$BASE_URL/api/health" | grep -q "healthy" && echo "  PASSED - API is healthy" || echo "  FAILED"
echo ""

# Test 2: Home page load
echo "✓ Homepage Load:"
wget -q -O - "$BASE_URL/" | grep -q "WizardOfWanks" && echo "  PASSED - Homepage loaded" || echo "  FAILED"
echo ""

# Test 3: Check for login form
echo "✓ Login Form Check:"
wget -q -O - "$BASE_URL/" | grep -q "loginForm" && echo "  PASSED - Login form present" || echo "  FAILED"
echo ""

# Test 4: Check for Load Templates button
echo "✓ Load Templates Button Check:"
wget -q -O - "$BASE_URL/" | grep -q "Load Templates" && echo "  PASSED - Button element found" || echo "  FAILED"
echo ""

echo "All checks completed!"
