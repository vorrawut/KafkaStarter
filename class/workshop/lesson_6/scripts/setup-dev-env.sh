#!/bin/bash
# setup-dev-env.sh - Development Environment Setup Automation

# TODO: Add environment validation
# TODO: Check for required dependencies
# TODO: Validate Docker and Docker Compose versions
# TODO: Include system requirement checks

echo "🚀 Setting up Kafka development environment..."

# TODO: Implement dependency checking
# TODO: Verify Docker is installed and running
# TODO: Check Docker Compose version compatibility
# TODO: Validate available system resources

# TODO: Create Docker service startup
# TODO: Start services in correct order
# TODO: Wait for services to be ready
# TODO: Include health check validation

echo "📦 Starting Docker services..."
# TODO: Navigate to docker directory
# TODO: Run docker-compose up -d
# TODO: Implement service readiness checking

# TODO: Implement service readiness waiting
# TODO: Check Kafka broker is accepting connections
# TODO: Verify Schema Registry is responsive
# TODO: Validate Kafka UI is accessible

echo "⏳ Waiting for services to be ready..."
# TODO: Implement intelligent waiting with retries
# TODO: Check service health endpoints
# TODO: Validate port accessibility

# TODO: Create development topics
# TODO: Create topics needed for development
# TODO: Configure appropriate partition counts
# TODO: Set retention policies for development

echo "📝 Creating development topics..."
# TODO: Create user-events-dev topic
# TODO: Create order-events-dev topic
# TODO: Create test topics for various scenarios

# TODO: Implement environment verification
# TODO: Test connectivity to all services
# TODO: Verify topic creation was successful
# TODO: Check Schema Registry functionality

echo "✅ Verifying setup..."
# TODO: Curl Kafka UI health endpoint
# TODO: Test Schema Registry connectivity
# TODO: Validate AKHQ accessibility

# TODO: Create summary output
# TODO: Show service URLs and status
# TODO: Provide next steps and useful commands
# TODO: Include troubleshooting tips

echo "🎉 Development environment is ready!"
echo ""
echo "🔗 Useful URLs:"
echo "  Kafka UI: http://localhost:8080"
echo "  AKHQ: http://localhost:8082"
echo "  Schema Registry: http://localhost:8081"
echo ""
echo "🛠️ Useful commands:"
echo "  ./kafka-dev-tools.sh topics"
echo "  ./kafka-dev-tools.sh health"

# TODO: Add cleanup function
# TODO: Create stop-dev-env.sh companion script
# TODO: Include backup and restore capabilities
# TODO: Add configuration templating

# HINT: Use health check endpoints for readiness
# HINT: Implement exponential backoff for retries
# HINT: Add colored output for better user experience