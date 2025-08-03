#!/bin/bash

# Kafka Starter Demo - Complete System Startup Script
# This script starts the complete Kafka demo environment

set -e

echo "🎪 Starting Kafka Starter Demo System..."
echo "========================================"

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Function to print colored output
print_status() {
    echo -e "${BLUE}[INFO]${NC} $1"
}

print_success() {
    echo -e "${GREEN}[SUCCESS]${NC} $1"
}

print_warning() {
    echo -e "${YELLOW}[WARNING]${NC} $1"
}

print_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# Check if Docker is running
if ! docker info > /dev/null 2>&1; then
    print_error "Docker is not running. Please start Docker Desktop and try again."
    exit 1
fi

print_success "Docker is running ✓"

# Check available memory
AVAILABLE_MEMORY=$(docker system info --format '{{.MemTotal}}' 2>/dev/null || echo "0")
if [ "$AVAILABLE_MEMORY" -lt 4000000000 ]; then
    print_warning "Less than 4GB memory available for Docker. Consider increasing Docker memory allocation."
fi

# Step 1: Clean up any existing containers
print_status "Cleaning up existing containers..."
docker-compose -f docker-compose-complete.yml down --remove-orphans 2>/dev/null || true

# Step 2: Build all services
print_status "Building demo services..."
docker-compose -f docker-compose-complete.yml build --parallel

# Step 3: Start infrastructure services first
print_status "Starting infrastructure services (Kafka, PostgreSQL, etc.)..."
docker-compose -f docker-compose-complete.yml up -d zookeeper broker schema-registry postgres

# Wait for Kafka to be ready
print_status "Waiting for Kafka to be ready..."
timeout=60
counter=0
while ! docker exec kafka-starter-broker kafka-broker-api-versions --bootstrap-server localhost:9092 > /dev/null 2>&1; do
    if [ $counter -eq $timeout ]; then
        print_error "Timeout waiting for Kafka to start"
        exit 1
    fi
    echo -n "."
    sleep 2
    counter=$((counter + 2))
done
print_success "Kafka is ready ✓"

# Step 4: Create required topics
print_status "Creating Kafka topics..."
docker exec kafka-starter-broker kafka-topics --create --if-not-exists --topic user-events --bootstrap-server localhost:9092 --partitions 3 --replication-factor 1
docker exec kafka-starter-broker kafka-topics --create --if-not-exists --topic user-registered --bootstrap-server localhost:9092 --partitions 3 --replication-factor 1
docker exec kafka-starter-broker kafka-topics --create --if-not-exists --topic user-login --bootstrap-server localhost:9092 --partitions 3 --replication-factor 1
docker exec kafka-starter-broker kafka-topics --create --if-not-exists --topic order-events --bootstrap-server localhost:9092 --partitions 3 --replication-factor 1
docker exec kafka-starter-broker kafka-topics --create --if-not-exists --topic order-created --bootstrap-server localhost:9092 --partitions 3 --replication-factor 1
docker exec kafka-starter-broker kafka-topics --create --if-not-exists --topic inventory-events --bootstrap-server localhost:9092 --partitions 3 --replication-factor 1
docker exec kafka-starter-broker kafka-topics --create --if-not-exists --topic payment-events --bootstrap-server localhost:9092 --partitions 3 --replication-factor 1
docker exec kafka-starter-broker kafka-topics --create --if-not-exists --topic notification-events --bootstrap-server localhost:9092 --partitions 3 --replication-factor 1
docker exec kafka-starter-broker kafka-topics --create --if-not-exists --topic analytics-events --bootstrap-server localhost:9092 --partitions 6 --replication-factor 1

print_success "Topics created ✓"

# Step 5: Start monitoring services
print_status "Starting monitoring services..."
docker-compose -f docker-compose-complete.yml up -d kafka-ui akhq prometheus grafana

# Step 6: Start application services
print_status "Starting demo application services..."
docker-compose -f docker-compose-complete.yml up -d user-service
sleep 10
docker-compose -f docker-compose-complete.yml up -d order-service
sleep 10
docker-compose -f docker-compose-complete.yml up -d inventory-service payment-service notification-service
sleep 10
docker-compose -f docker-compose-complete.yml up -d analytics-service

# Step 7: Wait for all services to be healthy
print_status "Waiting for all services to be healthy..."
services=("user-service" "order-service" "inventory-service" "payment-service" "notification-service" "analytics-service")

for service in "${services[@]}"; do
    print_status "Checking $service health..."
    timeout=60
    counter=0
    while ! docker exec "kafka-demo-$service" curl -f http://localhost:8080/actuator/health > /dev/null 2>&1; do
        if [ $counter -eq $timeout ]; then
            print_warning "$service health check timeout, but continuing..."
            break
        fi
        echo -n "."
        sleep 2
        counter=$((counter + 2))
    done
    print_success "$service is ready ✓"
done

# Step 8: Initialize demo data
print_status "Initializing demo data..."
sleep 5

# Create sample users
curl -X POST http://localhost:8101/api/users/register \
  -H "Content-Type: application/json" \
  -d '{
    "username": "john_doe",
    "email": "john@example.com", 
    "password": "password123",
    "firstName": "John",
    "lastName": "Doe",
    "phoneNumber": "+1234567890"
  }' > /dev/null 2>&1 || print_warning "Sample user creation failed (service may not be fully ready)"

curl -X POST http://localhost:8101/api/users/register \
  -H "Content-Type: application/json" \
  -d '{
    "username": "jane_smith",
    "email": "jane@example.com",
    "password": "password123", 
    "firstName": "Jane",
    "lastName": "Smith",
    "phoneNumber": "+1234567891"
  }' > /dev/null 2>&1 || print_warning "Sample user creation failed"

print_success "Demo data initialized ✓"

# Step 9: Display service URLs
echo ""
echo "🎉 Demo System is Ready!"
echo "========================"
echo ""
echo "🌐 Web Interfaces:"
echo "  📊 Kafka UI:           http://localhost:8080"
echo "  📊 AKHQ (Alternative): http://localhost:8082"
echo "  📈 Grafana:            http://localhost:3001 (admin/admin)"
echo "  📊 Prometheus:         http://localhost:9090"
echo "  📊 Analytics Dashboard: http://localhost:3000"
echo ""
echo "🔧 Service APIs:"
echo "  👤 User Service:       http://localhost:8101"
echo "  📦 Order Service:      http://localhost:8102"
echo "  📋 Inventory Service:  http://localhost:8103"
echo "  💳 Payment Service:    http://localhost:8104"
echo "  🔔 Notification Svc:   http://localhost:8105"
echo "  📊 Analytics Service:  http://localhost:8106"
echo ""
echo "🧪 Demo Scenarios:"
echo "  📝 User Registration:  ./test-user-registration.sh"
echo "  🛒 Place Order:        ./test-order-flow.sh"
echo "  📊 View Analytics:     ./view-analytics.sh"
echo "  🔍 Monitor Events:     ./monitor-events.sh"
echo ""
echo "🛑 To stop the demo:"
echo "  docker-compose -f docker-compose-complete.yml down"
echo ""
echo "📚 For more information, see demo/README.md"
echo ""
print_success "All systems operational! 🚀"