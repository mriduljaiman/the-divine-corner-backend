#!/bin/bash

# Divine Corner Backend - Docker Build Script

set -e

echo "======================================"
echo "Divine Corner Backend - Docker Build"
echo "======================================"
echo ""

# Colors for output
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m' # No Color

# Check if Docker is installed
if ! command -v docker &> /dev/null; then
    echo -e "${RED}Error: Docker is not installed${NC}"
    exit 1
fi

echo -e "${YELLOW}Step 1: Building Docker image...${NC}"
docker build -t divine-corner-backend:latest .

if [ $? -eq 0 ]; then
    echo -e "${GREEN}✓ Docker image built successfully${NC}"
else
    echo -e "${RED}✗ Docker build failed${NC}"
    exit 1
fi

echo ""
echo -e "${YELLOW}Step 2: Image information${NC}"
docker images divine-corner-backend:latest

echo ""
echo -e "${GREEN}Build complete!${NC}"
echo ""
echo "To run the container:"
echo "  docker run -p 8080:8080 divine-corner-backend:latest"
echo ""
echo "To run with docker-compose:"
echo "  docker-compose up"
echo ""
echo "To push to a registry:"
echo "  docker tag divine-corner-backend:latest your-registry/divine-corner-backend:latest"
echo "  docker push your-registry/divine-corner-backend:latest"
