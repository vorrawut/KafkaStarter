# Lesson 3 Workshop: Hello Kafka - Your First Producer & Consumer

## 🎯 What We Want to Build

A simple Spring Boot application that:
1. **Produces** user registration events to Kafka
2. **Consumes** these events and processes them
3. **Exposes REST API** to trigger event production
4. **Demonstrates** basic Kafka patterns with JSON messages

## 📋 Expected Result

By the end of this workshop:
- REST endpoint `/api/users/register` produces Kafka events
- Background consumer processes registration events
- Events are visible in Kafka UI
- Logs show end-to-end message flow

## 🚀 Step-by-Step Code Walkthrough

### Step 1: Configuration Setup

Create `src/main/resources/application.yml`:

```yaml
# TODO: Add Kafka broker configuration
# TODO: Add consumer group configuration  
# TODO: Add JSON serialization settings
```

**What to implement:**
- Kafka bootstrap servers pointing to localhost:9092
- Consumer group ID for our application
- JSON serializers/deserializers
- Consumer offset reset policy

### Step 2: Event Model

Create `src/main/kotlin/com/learning/KafkaStarter/model/UserRegisteredEvent.kt`:

```kotlin
// TODO: Create data class for user registration events
// TODO: Include userId, email, timestamp, source fields
// TODO: Add proper JSON annotations
```

**What to implement:**
- Data class representing user registration
- JSON serialization annotations
- Validation constraints

### Step 3: Kafka Producer Configuration

Create `src/main/kotlin/com/learning/KafkaStarter/config/KafkaProducerConfig.kt`:

```kotlin
// TODO: Configure KafkaTemplate bean
// TODO: Set up JSON serializers
// TODO: Add error handling configuration
```

**What to implement:**
- Producer factory configuration
- KafkaTemplate bean with proper serializers
- Error handling and retry configuration

### Step 4: Kafka Consumer Configuration

Create `src/main/kotlin/com/learning/KafkaStarter/config/KafkaConsumerConfig.kt`:

```kotlin
// TODO: Configure consumer factory
// TODO: Set up JSON deserializers  
// TODO: Configure error handling
```

**What to implement:**
- Consumer factory configuration
- JSON deserializers
- Error handling strategy

### Step 5: Event Producer Service

Create `src/main/kotlin/com/learning/KafkaStarter/service/UserEventProducer.kt`:

```kotlin
// TODO: Inject KafkaTemplate
// TODO: Implement publishUserRegistered method
// TODO: Add logging and error handling
```

**What to implement:**
- Service class using KafkaTemplate
- Method to publish user registration events
- Proper logging and exception handling

### Step 6: Event Consumer Service

Create `src/main/kotlin/com/learning/KafkaStarter/service/UserEventConsumer.kt`:

```kotlin
// TODO: Create @KafkaListener method
// TODO: Process incoming events
// TODO: Add error handling and logging
```

**What to implement:**
- @KafkaListener annotation with topic configuration
- Event processing logic
- Error handling for poison messages

### Step 7: REST Controller

Create `src/main/kotlin/com/learning/KafkaStarter/controller/UserController.kt`:

```kotlin
// TODO: Create REST endpoint for user registration
// TODO: Inject UserEventProducer
// TODO: Return appropriate HTTP responses
```

**What to implement:**
- POST endpoint to trigger user registration
- Input validation
- Response handling

### Step 8: Integration Test

Create `src/test/kotlin/com/learning/KafkaStarter/UserEventIntegrationTest.kt`:

```kotlin
// TODO: Set up embedded Kafka test
// TODO: Test producer/consumer integration
// TODO: Verify event processing
```

**What to implement:**
- Embedded Kafka test configuration
- Integration test for complete flow
- Assertions for event production and consumption

## 🔧 How to Run

### 1. Start Kafka Environment
```bash
cd docker
docker-compose up -d
```

### 2. Run the Application
```bash
./gradlew bootRun
```

### 3. Test the Endpoint
```bash
# Register a user (produces Kafka event)
curl -X POST http://localhost:8090/api/users/register \
  -H "Content-Type: application/json" \
  -d '{
    "userId": "user123",
    "email": "test@example.com",
    "source": "web"
  }'
```

### 4. Verify in Kafka UI
1. Open http://localhost:8080
2. Navigate to Topics → `user-registration`
3. Browse messages to see your event

### 5. Check Application Logs
```bash
# Look for producer and consumer logs
tail -f logs/application.log
```

## ✅ Success Criteria

- [ ] Application starts without errors
- [ ] POST to `/api/users/register` returns 200 OK
- [ ] Events appear in `user-registration` topic
- [ ] Consumer logs show event processing
- [ ] Integration tests pass

## 🔍 Debugging Tips

### Common Issues

1. **Kafka Connection Failed**
   - Verify Docker containers are running
   - Check port 9092 is available

2. **Serialization Errors**
   - Verify JSON annotations on event model
   - Check producer/consumer configurations match

3. **Consumer Not Receiving**
   - Verify topic name matches exactly
   - Check consumer group configuration
   - Review offset reset policy

### Useful Commands

```bash
# Check topic exists
docker exec kafka-starter-broker kafka-topics \
  --list --bootstrap-server localhost:9092

# Monitor topic messages
docker exec kafka-starter-broker kafka-console-consumer \
  --topic user-registration \
  --from-beginning \
  --bootstrap-server localhost:9092

# Check consumer groups
docker exec kafka-starter-broker kafka-consumer-groups \
  --bootstrap-server localhost:9092 \
  --list
```

## 🎯 What You're Learning

### Kafka Concepts
- **Topic**: Channel for related events (`user-registration`)
- **Producer**: Publishes events to topics
- **Consumer**: Subscribes to topics and processes events
- **Serialization**: Converting objects to/from bytes

### Spring Boot Integration
- **@KafkaListener**: Annotation-driven event consumption
- **KafkaTemplate**: Spring's Kafka producer abstraction
- **Configuration**: Type-safe configuration with @ConfigurationProperties

### Best Practices
- **Event-driven design**: Loose coupling between components
- **JSON serialization**: Human-readable, debuggable format
- **Error handling**: Graceful failure and retry strategies
- **Testing**: Integration testing with embedded Kafka

## 🚀 Next Steps

Once you complete this workshop:
1. Experiment with different event payloads
2. Try producing events from different endpoints
3. Add multiple consumers to see load balancing
4. Explore the Kafka UI to understand message flow

Ready for the next level? [Lesson 4: Topics, Partitions & Offsets](../lesson_4/concept.md) dives deeper into Kafka's core concepts.

---

*This is your first real Kafka application! The patterns you learn here will scale to production systems handling millions of events.*