#!/bin/bash

# WizardOfWanks System Verification Suite

echo "=========================================="
echo "WizardOfWanks Enterprise - Verification Suite"
echo "=========================================="
echo ""

# Colors
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Test counters
TOTAL=0
PASSED=0
FAILED=0

# Function to run a test
run_test() {
    local test_name=$1
    local command=$2
    TOTAL=$((TOTAL + 1))
    
    echo -n "Test $TOTAL: $test_name... "
    
    if eval "$command" > /dev/null 2>&1; then
        echo -e "${GREEN}PASSED${NC}"
        PASSED=$((PASSED + 1))
    else
        echo -e "${RED}FAILED${NC}"
        FAILED=$((FAILED + 1))
    fi
}

# ==================== CONTAINER TESTS ====================
echo "Container Status Tests:"
run_test "WizardOfWanks container running" "docker ps | grep wizard-of-wanks"
run_test "PostgreSQL container running" "docker ps | grep wizard-postgres"
run_test "App port 5000 exposed" "docker ps | grep ':5000'"
run_test "Database port 5432 exposed" "docker ps | grep ':5432'"
echo ""

# ==================== API TESTS ====================
echo "API Endpoint Tests:"
run_test "Health check endpoint" "docker exec wizard-of-wanks wget -q -O - http://localhost:5000/api/health | grep -q 'healthy'"
run_test "Homepage loads" "docker exec wizard-of-wanks wget -q -O - http://localhost:5000/ | grep -q 'WizardOfWanks'"
run_test "Categories endpoint" "docker exec wizard-of-wanks wget -q -O - http://localhost:5000/api/categories | grep -q 'Business'"
echo ""

# ==================== DATABASE TESTS ====================
echo "Database Tests:"
run_test "Database connectivity" "docker exec wizard-postgres pg_isready -U wizard -d wizard_db | grep -q 'accepting'"
run_test "Users table exists" "docker exec wizard-postgres psql -U wizard -d wizard_db -c \"SELECT 1 FROM information_schema.tables WHERE table_name='users'\" | grep -q '1'"
run_test "Templates table exists" "docker exec wizard-postgres psql -U wizard -d wizard_db -c \"SELECT 1 FROM information_schema.tables WHERE table_name='templates'\" | grep -q '1'"
run_test "Audit logs table exists" "docker exec wizard-postgres psql -U wizard -d wizard_db -c \"SELECT 1 FROM information_schema.tables WHERE table_name='audit_logs'\" | grep -q '1'"
echo ""

# ==================== FEATURE TESTS ====================
echo "Feature Tests:"
run_test "Login form present" "docker exec wizard-of-wanks wget -q -O - http://localhost:5000/ | grep -q 'loginForm'"
run_test "Categories in UI" "docker exec wizard-of-wanks wget -q -O - http://localhost:5000/ | grep -q 'Business'"
run_test "Create button present" "docker exec wizard-of-wanks wget -q -O - http://localhost:5000/ | grep -q 'Create New Template'"
run_test "Dashboard elements" "docker exec wizard-of-wanks wget -q -O - http://localhost:5000/ | grep -q 'Dashboard'"
echo ""

# ==================== SERVICE HEALTH ====================
echo "Service Health Tests:"
run_test "App responding to requests" "docker exec wizard-of-wanks wget -q -O - http://localhost:5000/ | grep -q 'DOCTYPE'"
run_test "Database responding" "docker exec wizard-postgres pg_isready -U wizard"
run_test "No critical errors in app logs" "! docker logs wizard-of-wanks 2>&1 | grep -i 'error' | grep -i 'critical'"
echo ""

# ==================== RESULTS ====================
echo "=========================================="
echo "Test Results Summary"
echo "=========================================="
echo "Total Tests: $TOTAL"
echo -e "Passed: ${GREEN}$PASSED${NC}"
echo -e "Failed: ${RED}$FAILED${NC}"
echo ""

if [ $FAILED -eq 0 ]; then
    echo -e "${GREEN}✓ All tests passed! System is 100% operational.${NC}"
    exit 0
else
    echo -e "${RED}✗ $FAILED test(s) failed. Please review.${NC}"
    exit 1
fi
