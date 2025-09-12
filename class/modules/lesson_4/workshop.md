# Workshop

## Topics, Partitions & Offsets Management

## 🎯 What We Want to Build

A comprehensive topic management application that demonstrates:
1. **Dynamic topic creation** with custom partition counts
2. **Key-based message routing** to specific partitions
3. **Partition monitoring** and consumer lag tracking
4. **CLI integration** for topic administration
5. **Offset management** and replay capabilities

## 📋 Expected Result

By the end of this workshop:
- REST API for creating and managing topics
- Producer that demonstrates partition routing strategies
- Consumer that tracks partition assignments and offsets
- CLI commands for topic inspection and management
- Real-time partition metrics and monitoring

## 🚀 Step-by-Step Code Walkthrough

### Step 1: Topic Management Service

Create `src/main/kotlin/com/learning/KafkaStarter/service/TopicManagerService.kt`:

```kotlin
// TODO: Create service for dynamic topic management
// TODO: Implement methods for topic creation, deletion, and configuration
// TODO: Add partition count management functionality
// TODO: Include topic inspection and metadata retrieval
```

**What to implement:**
- Dynamic topic creation with configurable partitions
- Topic configuration management (retention, cleanup policy)
- Topic inspection and metadata retrieval
- Partition scaling operations

### Step 2: Partition-Aware Producer

Create `src/main/kotlin/com/learning/KafkaStarter/service/PartitionAwareProducer.kt`:

```kotlin
// TODO: Create producer that demonstrates partition routing
// TODO: Implement key-based partitioning strategies
// TODO: Add callback handlers for partition assignment feedback
// TODO: Include metrics collection for partition distribution
```

**What to implement:**
- Multiple partitioning strategies (key-based, round-robin, custom)
- Partition routing demonstration with different keys
- Producer callbacks showing partition assignments
- Metrics tracking message distribution across partitions

### Step 3: Partition-Tracking Consumer

Create `src/main/kotlin/com/learning/KafkaStarter/service/PartitionTrackingConsumer.kt`:

```kotlin
// TODO: Create consumer that tracks partition assignments
// TODO: Implement offset management and commit strategies
// TODO: Add partition rebalancing listeners
// TODO: Include lag monitoring and reporting
```

**What to implement:**
- Partition assignment tracking and logging
- Manual offset management with commit strategies
- Partition rebalancing event handling
- Consumer lag monitoring and alerting

### Step 4: Topic Administration REST API

Create `src/main/kotlin/com/learning/KafkaStarter/controller/TopicAdminController.kt`:

```kotlin
// TODO: Create REST endpoints for topic management
// TODO: Implement topic creation with partition configuration
// TODO: Add endpoints for topic inspection and metadata
// TODO: Include partition scaling and configuration updates
```

**What to implement:**
- POST endpoint for topic creation with custom partitions
- GET endpoints for topic listing and detailed information
- PUT endpoint for partition scaling
- DELETE endpoint for topic cleanup

### Step 5: Partition Metrics Service

Create `src/main/kotlin/com/learning/KafkaStarter/service/PartitionMetricsService.kt`:

```kotlin
// TODO: Create service for partition-level metrics
// TODO: Implement consumer lag tracking by partition
// TODO: Add message distribution monitoring
// TODO: Include throughput metrics and alerting
```

**What to implement:**
- Real-time consumer lag monitoring per partition
- Message distribution analysis across partitions
- Throughput metrics collection and reporting
- Partition health checking and alerting

### Step 6: CLI Integration Commands

Create `src/main/kotlin/com/learning/KafkaStarter/cli/KafkaCliService.kt`:

```kotlin
// TODO: Create service wrapping Kafka CLI commands
// TODO: Implement topic listing and description
// TODO: Add consumer group and offset management
// TODO: Include partition assignment inspection
```

**What to implement:**
- Programmatic Kafka CLI command execution
- Topic inspection and configuration retrieval
- Consumer group management and offset tracking
- Partition assignment and rebalancing monitoring

### Step 7: Configuration and Properties

Update `src/main/resources/application.yml`:

```yaml
# TODO: Add topic management configurations
# TODO: Configure partition assignment strategies
# TODO: Set up consumer offset management
# TODO: Include monitoring and metrics settings
```

**What to implement:**
- Topic auto-creation policies
- Default partition and replication settings
- Consumer offset reset and commit strategies
- Monitoring and metrics configuration

### Step 8: Integration Tests

Create `src/test/kotlin/com/learning/KafkaStarter/PartitionManagementIntegrationTest.kt`:

```kotlin
// TODO: Create integration tests for partition management
// TODO: Test topic creation and partition scaling
// TODO: Verify key-based partition routing
// TODO: Validate offset management and consumer lag tracking
```

**What to implement:**
- Topic lifecycle management testing
- Partition routing and key distribution validation
- Consumer offset management and replay testing
- Metrics collection and monitoring verification

## 🔧 How to Run

### 1. Start Kafka Environment
```bash
cd docker
docker-compose up -d
```

### 2. Build and Run Application
```bash
./gradlew bootRun
```

### 3. Create Topics with Custom Partitions
```bash
# Create topic with 3 partitions
curl -X POST http://localhost:8090/api/topics \
  -H "Content-Type: application/json" \
  -d '{
    "topicName": "user-activities",
    "partitions": 3,
    "replicationFactor": 1,
    "configs": {
      "retention.ms": "604800000",
      "cleanup.policy": "delete"
    }
  }'
```

### 4. Test Partition Routing
```bash
# Send messages with different keys to observe partition routing
curl -X POST http://localhost:8090/api/messages/send \
  -H "Content-Type: application/json" \
  -d '{
    "topic": "user-activities",
    "key": "user123",
    "message": {
      "userId": "user123",
      "activity": "login",
      "timestamp": "2024-01-01T10:00:00Z"
    }
  }'

curl -X POST http://localhost:8090/api/messages/send \
  -H "Content-Type: application/json" \
  -d '{
    "topic": "user-activities", 
    "key": "user456",
    "message": {
      "userId": "user456",
      "activity": "purchase",
      "timestamp": "2024-01-01T10:01:00Z"
    }
  }'
```

### 5. Monitor Partition Distribution
```bash
# Check partition assignments and message distribution
curl http://localhost:8090/api/topics/user-activities/partitions

# Monitor consumer lag by partition
curl http://localhost:8090/api/consumer-groups/user-activities-group/lag
```

### 6. Use CLI Commands
```bash
# List topics with partition details
docker exec kafka-starter-broker kafka-topics \
  --list --bootstrap-server localhost:9092

# Describe specific topic
docker exec kafka-starter-broker kafka-topics \
  --describe --topic user-activities \
  --bootstrap-server localhost:9092

# Check consumer group status
docker exec kafka-starter-broker kafka-consumer-groups \
  --bootstrap-server localhost:9092 \
  --group user-activities-group \
  --describe
```

### 7. View in Kafka UI
1. Open http://localhost:8080
2. Navigate to Topics → user-activities
3. View partition distribution and message details
4. Monitor consumer groups and lag metrics

## ✅ Success Criteria

- [ ] Can create topics with custom partition counts via API
- [ ] Messages with same key consistently route to same partition
- [ ] Consumer tracks partition assignments and offsets correctly
- [ ] Partition metrics show accurate lag and distribution data
- [ ] CLI commands successfully inspect topics and consumer groups
- [ ] Kafka UI displays topic and partition information correctly

## 🔍 Debugging Tips

### Common Issues

1. **Uneven Partition Distribution**
   ```bash
   # Check key distribution across partitions
   curl http://localhost:8090/api/topics/user-activities/key-distribution
   
   # Verify hash function behavior
   echo "user123" | md5sum  # Check key hashing
   ```

2. **Consumer Lag Issues**
   ```bash
   # Check consumer group status
   docker exec kafka-starter-broker kafka-consumer-groups \
     --bootstrap-server localhost:9092 \
     --group user-activities-group \
     --describe
   
   # Reset consumer offsets if needed
   docker exec kafka-starter-broker kafka-consumer-groups \
     --bootstrap-server localhost:9092 \
     --group user-activities-group \
     --reset-offsets --to-earliest \
     --topic user-activities --execute
   ```

3. **Topic Creation Failures**
   ```bash
   # Check topic configuration limits
   docker exec kafka-starter-broker kafka-configs \
     --bootstrap-server localhost:9092 \
     --entity-type brokers \
     --entity-name 1 \
     --describe
   ```

### Useful Monitoring Commands

```bash
# Watch partition assignments in real-time
watch -n 1 'docker exec kafka-starter-broker kafka-consumer-groups \
  --bootstrap-server localhost:9092 \
  --group user-activities-group \
  --describe'

# Monitor topic message rates
docker exec kafka-starter-broker kafka-run-class \
  kafka.tools.ConsumerPerformance \
  --bootstrap-server localhost:9092 \
  --topic user-activities \
  --messages 1000

# Check partition leader distribution
docker exec kafka-starter-broker kafka-topics \
  --bootstrap-server localhost:9092 \
  --describe --topic user-activities
```

## 📊 Learning Validation

### Understanding Check Questions

1. **Partitioning**: Why do messages with the same key go to the same partition?
2. **Scaling**: What happens to existing messages when you increase partition count?
3. **Ordering**: How does Kafka guarantee message ordering, and what are the limitations?
4. **Consumer Groups**: How do partition assignments work with multiple consumers?
5. **Offsets**: What's the difference between committing offsets manually vs. automatically?

### Hands-On Challenges

1. **Create a topic** with 6 partitions and observe key distribution
2. **Scale partition count** and verify existing message preservation
3. **Implement custom partitioner** based on message content
4. **Monitor consumer lag** and implement alerting thresholds
5. **Test offset reset** strategies with historical data replay

## 🎯 Key Learning Outcomes

After completing this workshop, you'll understand:

### 📊 **Storage Architecture**
- How Kafka organizes data into topics and partitions
- The relationship between partitions and scalability
- Why partition count affects consumer parallelism

### 🗂️ **Partitioning Strategies**
- Key-based partitioning for ordering guarantees
- Impact of key selection on partition distribution
- Trade-offs between ordering and parallelism

### 📈 **Operational Management**
- Creating and configuring topics for different use cases
- Monitoring partition health and consumer lag
- Scaling partitions and managing topic lifecycle

### 🔧 **Development Patterns**
- Implementing partition-aware producers and consumers
- Managing offsets for reliable message processing
- Building monitoring and alerting for partition metrics

## 🚀 Next Steps

Once you've mastered topic and partition management, you're ready for [Lesson 5: Schema Registry](../lesson_5/overview.md), where you'll learn to manage structured data with schema evolution and type safety.

---

*Understanding partitions and offsets is fundamental to building scalable, reliable Kafka applications. This knowledge will be essential for all advanced patterns you'll learn in subsequent lessons.*