# Workshop

## Development Tools - Building Production-Ready Kafka Applications

## 🎯 What We Want to Build

A comprehensive development toolkit demonstrating:
1. **Advanced debugging techniques** with message inspection and tracing
2. **Comprehensive testing framework** with unit, integration, and load tests
3. **Production monitoring setup** with metrics, health checks, and alerting
4. **CLI automation tools** for development and operations
5. **Error tracking and analysis** with detailed diagnostics
6. **Performance profiling** and optimization tools

## 📋 Expected Result

By the end of this workshop:
- Complete testing framework with embedded Kafka and TestContainers
- Production-ready monitoring with Micrometer and custom metrics
- Advanced debugging tools for message inspection and troubleshooting
- CLI automation scripts for common development tasks
- Error tracking system with analytics and reporting
- Performance profiling toolkit with benchmarking capabilities

## 🚀 Step-by-Step Code Walkthrough

### Step 1: Advanced Testing Framework

Create `src/test/kotlin/com/learning/KafkaStarter/testing/KafkaTestFramework.kt`:

```kotlin
// TODO: Create comprehensive testing utilities
// TODO: Implement embedded Kafka setup helpers
// TODO: Add TestContainers integration
// TODO: Create test data generators and utilities
```

**What to implement:**
- Embedded Kafka test configuration
- TestContainers setup for integration tests
- Test data generation utilities
- Test assertion helpers for Kafka scenarios

### Step 2: Debugging & Inspection Tools

Create `src/main/kotlin/com/learning/KafkaStarter/debug/KafkaDebugger.kt`:

```kotlin
// TODO: Create message inspection utilities
// TODO: Implement consumer state debugging
// TODO: Add partition and offset analysis
// TODO: Create message trace logging
```

**What to implement:**
- Message inspection and detailed logging
- Consumer state analysis and reporting
- Partition assignment debugging
- Message flow tracing capabilities

### Step 3: Performance Profiling Service

Create `src/main/kotlin/com/learning/KafkaStarter/monitoring/KafkaPerformanceProfiler.kt`:

```kotlin
// TODO: Implement operation timing and profiling
// TODO: Add throughput measurement capabilities
// TODO: Create performance reporting
// TODO: Include memory usage analysis
```

**What to implement:**
- Operation timing and performance measurement
- Throughput analysis and reporting
- Memory usage profiling
- Performance bottleneck identification

### Step 4: Error Tracking & Analysis

Create `src/main/kotlin/com/learning/KafkaStarter/monitoring/KafkaErrorTracker.kt`:

```kotlin
// TODO: Create error tracking and categorization
// TODO: Implement error analytics and reporting
// TODO: Add error pattern detection
// TODO: Include recovery recommendations
```

**What to implement:**
- Comprehensive error tracking and categorization
- Error pattern analysis and detection
- Recovery strategy recommendations
- Error reporting and visualization

### Step 5: Custom Metrics & Monitoring

Create `src/main/kotlin/com/learning/KafkaStarter/monitoring/KafkaMetricsCollector.kt`:

```kotlin
// TODO: Implement custom Kafka metrics collection
// TODO: Add business metrics tracking
// TODO: Create Micrometer integration
// TODO: Include alerting thresholds
```

**What to implement:**
- Custom Kafka metrics collection
- Business-specific metrics tracking
- Micrometer integration for Prometheus
- Alerting threshold configuration

### Step 6: Health Check System

Create `src/main/kotlin/com/learning/KafkaStarter/health/KafkaHealthIndicator.kt`:

```kotlin
// TODO: Create comprehensive health checks
// TODO: Implement connectivity testing
// TODO: Add component health monitoring
// TODO: Include dependency health checks
```

**What to implement:**
- Kafka connectivity health checks
- Schema Registry health monitoring
- Consumer group health assessment
- Overall system health reporting

### Step 7: Custom Actuator Endpoints

Create `src/main/kotlin/com/learning/KafkaStarter/actuator/KafkaActuatorEndpoint.kt`:

```kotlin
// TODO: Create custom actuator endpoints
// TODO: Implement Kafka-specific information exposure
// TODO: Add operational management capabilities
// TODO: Include diagnostic information
```

**What to implement:**
- Custom actuator endpoints for Kafka
- Operational management interfaces
- Diagnostic information exposure
- Administrative capabilities

### Step 8: CLI Automation Tools

Create `scripts/kafka-dev-tools.sh`:

```bash
# TODO: Create CLI automation scripts
# TODO: Implement common development tasks
# TODO: Add environment setup automation
# TODO: Include debugging and monitoring commands
```

**What to implement:**
- Development environment setup scripts
- Common Kafka operation automation
- Debugging and troubleshooting commands
- Monitoring and health check scripts

### Step 9: Load Testing Framework

Create `src/test/kotlin/com/learning/KafkaStarter/load/KafkaLoadTestFramework.kt`:

```kotlin
// TODO: Create load testing utilities
// TODO: Implement throughput testing
// TODO: Add stress testing scenarios
// TODO: Include performance benchmarking
```

**What to implement:**
- Load testing framework and utilities
- Throughput and latency testing
- Stress testing scenarios
- Performance benchmarking tools

### Step 10: Integration Test Suite

Create `src/test/kotlin/com/learning/KafkaStarter/integration/ComprehensiveKafkaIntegrationTest.kt`:

```kotlin
// TODO: Create comprehensive integration tests
// TODO: Test all components together
// TODO: Validate monitoring and alerting
// TODO: Include failure scenario testing
```

**What to implement:**
- End-to-end integration test scenarios
- Component interaction validation
- Monitoring and alerting verification
- Failure recovery testing

## 🔧 How to Run

### 1. Start Complete Environment
```bash
cd docker
docker-compose up -d

# Run environment setup script
./scripts/setup-dev-env.sh
```

### 2. Run Comprehensive Tests
```bash
# Unit tests
./gradlew test

# Integration tests
./gradlew integrationTest

# Load tests
./gradlew loadTest
```

### 3. Test Debugging Tools
```bash
# Start application with debug logging
./gradlew bootRun --args="--logging.level.com.learning.KafkaStarter=DEBUG"

# Test message inspection
curl -X POST http://localhost:8090/api/debug/inspect-topic/user-events

# Analyze consumer state
curl http://localhost:8090/api/debug/consumer-state/user-events-group
```

### 4. Monitor Performance
```bash
# View performance metrics
curl http://localhost:8090/actuator/metrics/kafka.message.processing.time

# Get performance report
curl http://localhost:8090/api/monitoring/performance-report

# Check system health
curl http://localhost:8090/actuator/health
```

### 5. Test Error Tracking
```bash
# Trigger test errors
curl -X POST http://localhost:8090/api/debug/trigger-error \
  -H "Content-Type: application/json" \
  -d '{"errorType": "SerializationException", "count": 5}'

# View error summary
curl http://localhost:8090/api/monitoring/error-summary
```

### 6. Use CLI Tools
```bash
# Make scripts executable
chmod +x scripts/*.sh

# List topics with details
./scripts/kafka-dev-tools.sh topics

# Tail topic messages
./scripts/kafka-dev-tools.sh tail user-events

# Check consumer lag
./scripts/kafka-dev-tools.sh lag user-events-group

# Reset consumer group
./scripts/kafka-dev-tools.sh reset user-events-group user-events
```

### 7. Load Testing
```bash
# Run throughput test
curl -X POST http://localhost:8090/api/load-test/throughput \
  -H "Content-Type: application/json" \
  -d '{
    "messageCount": 10000,
    "concurrency": 10,
    "topic": "load-test"
  }'

# Run stress test
curl -X POST http://localhost:8090/api/load-test/stress \
  -H "Content-Type: application/json" \
  -d '{
    "duration": "5m",
    "maxThroughput": 1000
  }'
```

### 8. Monitor in Real-Time
```bash
# Watch Kafka UI metrics
open http://localhost:8080

# Monitor application metrics
open http://localhost:8090/actuator/prometheus

# View health dashboard
open http://localhost:8090/actuator/health
```

## ✅ Success Criteria

- [ ] All test types (unit, integration, load) pass successfully
- [ ] Debugging tools provide detailed message and consumer inspection
- [ ] Performance profiling identifies bottlenecks and optimization opportunities
- [ ] Error tracking captures and categorizes all error types
- [ ] Monitoring system reports accurate metrics and health status
- [ ] CLI tools automate common development and operational tasks
- [ ] Load testing framework validates system performance under stress

## 🔍 Debugging Tips

### Common Development Issues

1. **Test Environment Setup**
   ```bash
   # Check Docker services
   docker-compose ps
   
   # Verify Kafka connectivity
   docker exec kafka-starter-broker kafka-broker-api-versions --bootstrap-server localhost:9092
   
   # Check test containers
   docker ps | grep testcontainers
   ```

2. **Debugging Test Failures**
   ```bash
   # Run tests with detailed output
   ./gradlew test --info --stacktrace
   
   # Check embedded Kafka logs
   tail -f build/test-results/test/TEST-*.xml
   
   # Verify test data
   ./scripts/kafka-dev-tools.sh describe test-topic
   ```

3. **Performance Issues**
   ```bash
   # Check JVM metrics
   curl http://localhost:8090/actuator/metrics/jvm.memory.used
   
   # Monitor GC activity
   curl http://localhost:8090/actuator/metrics/jvm.gc.pause
   
   # Analyze message processing times
   curl http://localhost:8090/api/monitoring/processing-times
   ```

### Monitoring & Alerting

```bash
# Set up metric alerts
curl -X POST http://localhost:8090/api/monitoring/alerts \
  -H "Content-Type: application/json" \
  -d '{
    "metric": "kafka.consumer.lag.max",
    "threshold": 1000,
    "action": "slack_notification"
  }'

# Test alert triggers
curl -X POST http://localhost:8090/api/debug/trigger-alert/high-consumer-lag
```

## 📊 Learning Validation

### Understanding Check Questions

1. **Testing**: What are the differences between unit, integration, and load testing for Kafka applications?
2. **Debugging**: How do you trace a message through the entire Kafka processing pipeline?
3. **Monitoring**: What are the most important metrics to monitor in a production Kafka system?
4. **Performance**: How do you identify and resolve Kafka performance bottlenecks?
5. **Error Handling**: What strategies help distinguish between transient and permanent errors?

### Hands-On Challenges

1. **Build a custom test framework** that validates message ordering across partitions
2. **Create a debugging tool** that tracks message lineage from producer to consumer
3. **Implement monitoring dashboard** with real-time Kafka metrics and alerts
4. **Design load test suite** that validates system behavior under various failure scenarios
5. **Build CLI automation** for complete environment setup and teardown

## 🎯 Key Learning Outcomes

After completing this workshop, you'll understand:

### 🧪 **Testing Excellence**
- How to structure comprehensive test suites for Kafka applications
- When to use embedded Kafka vs TestContainers vs mocks
- Load testing strategies and performance validation techniques

### 🔍 **Debugging Mastery**
- Advanced message inspection and tracing techniques
- Consumer state analysis and troubleshooting approaches
- Performance profiling and bottleneck identification methods

### 📊 **Production Monitoring**
- Essential metrics for Kafka application health
- Custom metric collection and alerting strategies
- Health check implementation and dependency monitoring

### 🛠️ **Development Productivity**
- CLI automation for common development tasks
- Environment setup and teardown automation
- Debugging workflow optimization and tooling

## 🚀 Phase 1 Complete! 

🎉 **Congratulations!** You've mastered the **Kafka Foundations** with:

✅ **Event-driven architecture** principles and patterns  
✅ **Producer/Consumer development** with Spring Boot + Kotlin  
✅ **Topic and partition management** strategies  
✅ **Schema evolution** with Avro and Protobuf  
✅ **Development tools** and production best practices  

### 🌟 **What's Next?**

Ready for **Phase 2: Building Resilient Messaging Patterns**? 

Continue with [Lesson 7: Consumer Groups & Load Balancing](../lesson_7/concept.md) to learn advanced consumer patterns, scaling strategies, and fault-tolerant message processing.

---

*With these development tools and testing techniques, you're equipped to build, debug, and monitor enterprise-grade Kafka applications. The foundation is solid - time to build resilient, scalable systems!*