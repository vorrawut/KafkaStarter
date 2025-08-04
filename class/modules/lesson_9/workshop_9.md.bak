# Lesson 9 Workshop: Manual Acknowledgment & Idempotent Consumers

## 🎯 What We Want to Build

A comprehensive exactly-once processing system demonstrating:
1. **Manual acknowledgment patterns** with precise control over message commits
2. **Idempotent consumer implementations** preventing duplicate processing
3. **Database-based deduplication** with transactional consistency
4. **Conditional acknowledgment strategies** based on processing results
5. **Monitoring and metrics** for acknowledgment tracking
6. **Error handling patterns** distinguishing retryable vs permanent failures

## 📋 Expected Result

By the end of this workshop:
- Manual acknowledgment consumer that only commits after successful processing
- Idempotent consumer that prevents duplicate message processing
- Database-backed deduplication with transactional guarantees
- Conditional acknowledgment based on processing outcomes
- Comprehensive monitoring of acknowledgment patterns and duplicate detection
- Error handling that properly manages retries and permanent failures

## 🚀 Step-by-Step Code Walkthrough

### Step 1: Manual Acknowledgment Consumer

Create `src/main/kotlin/com/learning/KafkaStarter/service/ManualAckOrderProcessor.kt`:

```kotlin
// TODO: Create manual acknowledgment consumer
// TODO: Implement precise acknowledgment control
// TODO: Add error handling for different failure types
// TODO: Include processing time tracking
```

**What to implement:**
- Consumer with manual acknowledgment enabled
- Processing logic with multiple steps
- Conditional acknowledgment based on success/failure
- Comprehensive error classification and handling

### Step 2: Idempotent Payment Processor

Create `src/main/kotlin/com/learning/KafkaStarter/service/IdempotentPaymentProcessor.kt`:

```kotlin
// TODO: Create idempotent payment processing
// TODO: Implement message deduplication logic
// TODO: Add result caching for duplicate detection
// TODO: Include cleanup for old processed messages
```

**What to implement:**
- In-memory deduplication using ConcurrentHashMap
- Unique message ID extraction and validation
- Result caching for already processed messages
- Periodic cleanup of old entries to prevent memory leaks

### Step 3: Database-Based Deduplication

Create `src/main/kotlin/com/learning/KafkaStarter/entity/ProcessedMessage.kt`:

```kotlin
// TODO: Create entity for tracking processed messages
// TODO: Include message metadata and processing results
// TODO: Add indexes for efficient lookups
// TODO: Include timestamps for cleanup operations
```

Create `src/main/kotlin/com/learning/KafkaStarter/service/DatabaseIdempotentConsumer.kt`:

```kotlin
// TODO: Implement database-backed idempotent consumer
// TODO: Add transactional processing with deduplication
// TODO: Include atomic check-and-process operations
// TODO: Add error handling with transaction rollback
```

**What to implement:**
- JPA entity for tracking processed messages
- Repository with efficient lookup methods
- Transactional consumer with atomic operations
- Proper error handling with transaction management

### Step 4: Conditional Acknowledgment Service

Create `src/main/kotlin/com/learning/KafkaStarter/service/ConditionalAckConsumer.kt`:

```kotlin
// TODO: Create conditional acknowledgment logic
// TODO: Implement different acknowledgment strategies
// TODO: Add retry logic with exponential backoff
// TODO: Include dead letter topic integration
```

**What to implement:**
- Multiple acknowledgment conditions based on processing results
- Retry strategies with configurable limits
- Dead letter topic routing for permanent failures
- Monitoring and metrics for acknowledgment decisions

### Step 5: Transactional Consumer

Create `src/main/kotlin/com/learning/KafkaStarter/config/TransactionalKafkaConfig.kt`:

```kotlin
// TODO: Configure transactional Kafka settings
// TODO: Set up exactly-once semantics
// TODO: Include producer and consumer transaction setup
// TODO: Add error handling for transaction failures
```

Create `src/main/kotlin/com/learning/KafkaStarter/service/TransactionalOrderProcessor.kt`:

```kotlin
// TODO: Implement transactional message processing
// TODO: Add cross-topic transactional messaging
// TODO: Include database and Kafka transaction coordination
// TODO: Add rollback handling for failed transactions
```

**What to implement:**
- Transactional Kafka configuration with exactly-once semantics
- Consumer that processes messages within transactions
- Producer operations within the same transaction
- Comprehensive error handling with automatic rollback

### Step 6: Acknowledgment Metrics and Monitoring

Create `src/main/kotlin/com/learning/KafkaStarter/monitoring/AckMetricsCollector.kt`:

```kotlin
// TODO: Create acknowledgment metrics collection
// TODO: Track processing times and success rates
// TODO: Monitor duplicate detection and prevention
// TODO: Add alerting for acknowledgment delays
```

**What to implement:**
- Micrometer metrics for acknowledgment tracking
- Processing time measurement and reporting
- Duplicate detection rate monitoring
- Consumer lag tracking specific to manual acknowledgment

### Step 7: Batch Processing with Manual Acknowledgment

Create `src/main/kotlin/com/learning/KafkaStarter/service/BatchAckProcessor.kt`:

```kotlin
// TODO: Implement batch processing with manual ack
// TODO: Add partial failure handling
// TODO: Include dead letter topic routing for failed items
// TODO: Add metrics for batch processing efficiency
```

**What to implement:**
- Batch consumer with manual acknowledgment
- Partial failure handling strategies
- Dead letter topic routing for individual failures
- Batch processing metrics and monitoring

### Step 8: Consumer Configuration

Update `src/main/resources/application.yml`:

```yaml
# TODO: Configure manual acknowledgment settings
# TODO: Add transactional consumer properties
# TODO: Include monitoring and metrics configuration
# TODO: Set up retry and timeout configurations
```

**What to configure:**
- Manual acknowledgment mode settings
- Transactional isolation levels
- Consumer timeout and retry configurations
- Monitoring and metrics exposure

### Step 9: Integration Testing Framework

Create `src/test/kotlin/com/learning/KafkaStarter/ManualAckIntegrationTest.kt`:

```kotlin
// TODO: Create integration tests for manual acknowledgment
// TODO: Test idempotent processing scenarios
// TODO: Verify duplicate prevention works correctly
// TODO: Test error handling and retry logic
```

**What to test:**
- Manual acknowledgment prevents message loss on failure
- Idempotent processing correctly handles duplicates
- Transactional processing maintains consistency
- Error scenarios trigger proper retry or dead letter routing

### Step 10: REST API for Testing and Monitoring

Create `src/main/kotlin/com/learning/KafkaStarter/controller/AckTestController.kt`:

```kotlin
// TODO: Create REST endpoints for testing acknowledgment
// TODO: Add endpoints for monitoring acknowledgment status
// TODO: Include duplicate simulation and testing
// TODO: Add metrics and status reporting endpoints
```

**What to implement:**
- Endpoints to trigger various acknowledgment scenarios
- Monitoring endpoints for acknowledgment metrics
- Test endpoints for simulating duplicates and failures
- Status reporting for consumer processing state

## 🔧 How to Run

### 1. Start Kafka Environment
```bash
cd docker
docker-compose up -d

# Verify all services are running
docker-compose ps
```

### 2. Configure Application for Manual Acknowledgment
```bash
# Update application.yml with manual acknowledgment settings
# Start application with manual ack profile
./gradlew bootRun --args="--spring.profiles.active=manual-ack"
```

### 3. Test Manual Acknowledgment
```bash
# Send test messages
curl -X POST http://localhost:8095/api/ack-test/order \
  -H "Content-Type: application/json" \
  -d '{
    "orderId": "order-123",
    "customerId": "customer-456",
    "amount": 100.00,
    "items": ["item1", "item2"]
  }'

# Monitor acknowledgment status
curl http://localhost:8095/api/ack-test/status
```

### 4. Test Idempotent Processing
```bash
# Send duplicate messages
curl -X POST http://localhost:8095/api/ack-test/payment \
  -H "Content-Type: application/json" \
  -d '{
    "paymentId": "payment-789",
    "messageId": "unique-msg-123",
    "amount": 50.00,
    "customerId": "customer-456"
  }'

# Send the same message again (should be deduplicated)
curl -X POST http://localhost:8095/api/ack-test/payment \
  -H "Content-Type: application/json" \
  -d '{
    "paymentId": "payment-789", 
    "messageId": "unique-msg-123",
    "amount": 50.00,
    "customerId": "customer-456"
  }'

# Check deduplication metrics
curl http://localhost:8095/api/ack-test/duplicates
```

### 5. Test Transactional Processing
```bash
# Send transactional order
curl -X POST http://localhost:8095/api/ack-test/transactional-order \
  -H "Content-Type: application/json" \
  -d '{
    "orderId": "tx-order-999",
    "customerId": "customer-789",
    "amount": 200.00,
    "requiresInventoryUpdate": true
  }'

# Monitor transaction status
curl http://localhost:8095/api/ack-test/transaction-status/tx-order-999
```

### 6. Simulate Error Scenarios
```bash
# Simulate retryable error
curl -X POST http://localhost:8095/api/ack-test/simulate-error \
  -H "Content-Type: application/json" \
  -d '{
    "errorType": "NETWORK_TIMEOUT",
    "messageId": "error-test-123",
    "shouldRetry": true
  }'

# Simulate permanent error
curl -X POST http://localhost:8095/api/ack-test/simulate-error \
  -H "Content-Type: application/json" \
  -d '{
    "errorType": "INVALID_DATA",
    "messageId": "error-test-456", 
    "shouldRetry": false
  }'
```

### 7. Monitor Acknowledgment Metrics
```bash
# Check acknowledgment metrics
curl http://localhost:8095/actuator/metrics/kafka.acknowledgments

# Check processing time metrics
curl http://localhost:8095/actuator/metrics/kafka.message.processing.time

# Check duplicate detection metrics
curl http://localhost:8095/actuator/metrics/kafka.duplicates.detected

# Check consumer lag
curl http://localhost:8095/actuator/metrics/kafka.consumer.lag
```

### 8. Test Batch Processing
```bash
# Send batch of messages
curl -X POST http://localhost:8095/api/ack-test/batch-analytics \
  -H "Content-Type: application/json" \
  -d '{
    "batchSize": 10,
    "simulateFailures": true,
    "failureRate": 0.1
  }'

# Check batch processing results
curl http://localhost:8095/api/ack-test/batch-status
```

### 9. Database Verification
```bash
# Check processed messages in database
curl http://localhost:8095/api/ack-test/processed-messages

# Check for duplicate detection
curl http://localhost:8095/api/ack-test/processed-messages/duplicates

# View processing statistics
curl http://localhost:8095/api/ack-test/processing-stats
```

## ✅ Success Criteria

- [ ] Manual acknowledgment prevents message loss during processing failures
- [ ] Idempotent consumers correctly detect and handle duplicate messages
- [ ] Database-based deduplication works with transactional consistency
- [ ] Conditional acknowledgment properly routes messages based on processing results
- [ ] Transactional processing maintains exactly-once semantics
- [ ] Monitoring accurately tracks acknowledgment metrics and duplicate detection
- [ ] Error handling distinguishes between retryable and permanent failures
- [ ] Batch processing handles partial failures appropriately

## 🔍 Debugging Tips

### Common Issues and Solutions

1. **Messages Not Being Acknowledged**
   ```bash
   # Check consumer configuration
   curl http://localhost:8095/actuator/kafka | jq '.consumers'
   
   # Verify manual acknowledgment is enabled
   curl http://localhost:8095/actuator/env | grep "enable-auto-commit"
   
   # Check for processing errors
   curl http://localhost:8095/api/ack-test/error-summary
   ```

2. **Duplicate Messages Still Being Processed**
   ```bash
   # Check deduplication cache size
   curl http://localhost:8095/api/ack-test/cache-stats
   
   # Verify message ID extraction
   curl http://localhost:8095/api/ack-test/message-ids
   
   # Check database deduplication records
   curl http://localhost:8095/api/ack-test/processed-messages/recent
   ```

3. **Transaction Failures**
   ```bash
   # Check transaction configuration
   curl http://localhost:8095/actuator/configprops | grep transaction
   
   # Monitor transaction metrics
   curl http://localhost:8095/actuator/metrics/kafka.transaction
   
   # Check for isolation level issues
   curl http://localhost:8095/api/ack-test/transaction-diagnostics
   ```

### Performance Monitoring

```bash
# Monitor acknowledgment delays
curl http://localhost:8095/actuator/metrics/kafka.acknowledgment.delay

# Check processing throughput
curl http://localhost:8095/actuator/metrics/kafka.processing.throughput

# Monitor consumer lag for manual ack consumers
curl http://localhost:8095/actuator/metrics/kafka.consumer.lag

# Check memory usage for deduplication cache
curl http://localhost:8095/actuator/metrics/jvm.memory.used
```

## 📊 Learning Validation

### Understanding Check Questions

1. **Manual Acknowledgment**: When should you acknowledge a message vs when should you not?
2. **Idempotency**: What are the different strategies for preventing duplicate processing?
3. **Transactions**: How do Kafka transactions ensure exactly-once semantics?
4. **Error Handling**: How do you distinguish between retryable and permanent errors?
5. **Monitoring**: What metrics are most important for manual acknowledgment consumers?

### Hands-On Challenges

1. **Build custom acknowledgment strategy** that acknowledges messages based on business logic
2. **Implement time-based deduplication** that handles messages arriving out of order
3. **Create cross-service transaction** that coordinates Kafka and database operations
4. **Design error classification system** with configurable retry policies
5. **Build monitoring dashboard** showing acknowledgment patterns and duplicate rates

## 🎯 Key Learning Outcomes

After completing this workshop, you'll understand:

### 🎯 **Manual Acknowledgment Mastery**
- When and how to use manual acknowledgment for precise control
- Error handling strategies for different failure scenarios
- Performance implications and monitoring requirements

### 🔒 **Idempotent Processing Expertise**
- Multiple deduplication strategies and their trade-offs
- Database-backed vs in-memory deduplication approaches
- Cleanup strategies for long-running applications

### 🔄 **Exactly-Once Semantics**
- Kafka transactional processing patterns
- Cross-system transaction coordination
- Consistency guarantees and their limitations

### 📊 **Production Monitoring**
- Essential metrics for acknowledgment-based consumers
- Alerting strategies for processing delays and failures
- Performance optimization techniques

## 🚀 What's Next?

Congratulations! You've mastered exactly-once processing with manual acknowledgment and idempotent consumers.

Ready for **Lesson 10: Message Transformation & Filtering**? Learn how to build data processing pipelines that transform, enrich, and route messages based on content and business rules.

---

*Manual acknowledgment and idempotent processing are fundamental patterns for building reliable, consistent Kafka applications. These techniques ensure your systems handle failures gracefully while maintaining data integrity.*