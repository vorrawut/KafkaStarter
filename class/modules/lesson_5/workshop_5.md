# Lesson 5 Workshop: Schema Registry with Avro & Protobuf

## 🎯 What We Want to Build

A comprehensive schema management system demonstrating:
1. **Avro schema evolution** with backward/forward compatibility
2. **Protobuf integration** for high-performance serialization
3. **Schema Registry management** with REST API
4. **Type-safe producers and consumers** using generated classes
5. **Schema validation and compatibility testing**
6. **Performance comparison** between JSON, Avro, and Protobuf

## 📋 Expected Result

By the end of this workshop:
- Schema Registry integrated with Kafka producers/consumers
- Multiple data formats (JSON, Avro, Protobuf) in same application
- Schema evolution scenarios with compatibility validation
- REST API for schema management and testing
- Performance metrics comparing serialization formats
- Integration tests covering schema evolution scenarios

## 🚀 Step-by-Step Code Walkthrough

### Step 1: Avro Schema Definitions

Create `src/main/resources/avro/user-events.avsc`:

```json
// TODO: Define Avro schema for UserRegistered event
// TODO: Include fields: userId (string), email (string), timestamp (long), source (string with default)
// TODO: Add proper documentation for each field
// TODO: Consider evolution strategy for future fields
```

**What to implement:**
- Complete Avro schema definition with proper types
- Default values for optional fields
- Documentation for each field
- Namespace and logical types

### Step 2: Protobuf Schema Definitions

Create `src/main/proto/user_events.proto`:

```protobuf
// TODO: Define Protobuf schema for UserRegistered event
// TODO: Include proper field numbers and types
// TODO: Add enums for structured data
// TODO: Consider field number reservation for evolution
```

**What to implement:**
- Protobuf message definitions
- Enum types for categorical data
- Proper field numbering strategy
- Import statements for common types

### Step 3: Schema Registry Configuration

Create `src/main/kotlin/com/learning/KafkaStarter/config/SchemaRegistryConfig.kt`:

```kotlin
// TODO: Configure Schema Registry client
// TODO: Set up Avro serializers and deserializers
// TODO: Configure Protobuf serializers and deserializers
// TODO: Add schema registry health checks
```

**What to implement:**
- Schema Registry client configuration
- Avro producer/consumer factories
- Protobuf producer/consumer factories
- Connection pooling and timeouts

### Step 4: Multi-Format Producer Service

Create `src/main/kotlin/com/learning/KafkaStarter/service/SchemaAwareProducer.kt`:

```kotlin
// TODO: Create producer supporting multiple formats
// TODO: Implement Avro message production
// TODO: Implement Protobuf message production
// TODO: Add format selection and routing logic
```

**What to implement:**
- Multi-format message publishing
- Schema validation before sending
- Performance metrics collection
- Error handling for schema failures

### Step 5: Schema Evolution Service

Create `src/main/kotlin/com/learning/KafkaStarter/service/SchemaEvolutionService.kt`:

```kotlin
// TODO: Implement schema evolution testing
// TODO: Add backward compatibility validation
// TODO: Add forward compatibility validation
// TODO: Create schema version management
```

**What to implement:**
- Schema compatibility checking
- Version management and rollback
- Evolution testing scenarios
- Schema diff and comparison tools

### Step 6: Multi-Format Consumer Service

Create `src/main/kotlin/com/learning/KafkaStarter/service/SchemaAwareConsumer.kt`:

```kotlin
// TODO: Create consumer handling multiple formats
// TODO: Implement Avro message consumption
// TODO: Implement Protobuf message consumption  
// TODO: Add automatic format detection
```

**What to implement:**
- Multi-format message consumption
- Automatic schema resolution
- Type-safe deserialization
- Format-specific error handling

### Step 7: Schema Management REST API

Create `src/main/kotlin/com/learning/KafkaStarter/controller/SchemaController.kt`:

```kotlin
// TODO: Create REST endpoints for schema management
// TODO: Implement schema registration endpoints
// TODO: Add compatibility testing endpoints
// TODO: Include schema retrieval and listing
```

**What to implement:**
- Schema CRUD operations via REST
- Compatibility testing endpoints
- Schema evolution simulation
- Format conversion utilities

### Step 8: Performance Benchmarking Service

Create `src/main/kotlin/com/learning/KafkaStarter/service/SerializationBenchmarkService.kt`:

```kotlin
// TODO: Implement serialization performance testing
// TODO: Compare JSON, Avro, and Protobuf performance
// TODO: Measure payload sizes and throughput
// TODO: Generate performance reports
```

**What to implement:**
- Serialization/deserialization benchmarks
- Payload size comparison
- Throughput measurement
- Performance reporting and visualization

### Step 9: Configuration and Properties

Update `src/main/resources/application.yml`:

```yaml
# TODO: Add Schema Registry configurations
# TODO: Configure Avro serializer settings
# TODO: Configure Protobuf serializer settings
# TODO: Set up monitoring and health checks
```

**What to implement:**
- Schema Registry URL and authentication
- Serializer-specific configurations
- Compatibility level settings
- Monitoring and metrics configuration

### Step 10: Integration Tests

Create `src/test/kotlin/com/learning/KafkaStarter/SchemaEvolutionIntegrationTest.kt`:

```kotlin
// TODO: Create integration tests for schema evolution
// TODO: Test backward compatibility scenarios
// TODO: Test forward compatibility scenarios
// TODO: Validate performance across formats
```

**What to implement:**
- Schema evolution test scenarios
- Compatibility validation tests
- Performance benchmark tests
- Error handling and recovery tests

## 🔧 How to Run

### 1. Start Kafka Environment with Schema Registry
```bash
cd docker
docker-compose up -d

# Verify Schema Registry is running
curl http://localhost:8081/subjects
```

### 2. Generate Avro Classes
```bash
# Add Avro code generation to build
./gradlew generateAvroJava

# Verify generated classes
ls build/generated-main-avro-java/
```

### 3. Generate Protobuf Classes
```bash
# Add Protobuf code generation to build
./gradlew generateProto

# Verify generated classes
ls build/generated/source/proto/main/java/
```

### 4. Build and Run Application
```bash
./gradlew bootRun
```

### 5. Register Schemas
```bash
# Register Avro schema
curl -X POST http://localhost:8090/api/schemas/avro/user-events \
  -H "Content-Type: application/json" \
  -d '{
    "schema": {
      "type": "record",
      "name": "UserRegistered",
      "fields": [
        {"name": "userId", "type": "string"},
        {"name": "email", "type": "string"},
        {"name": "timestamp", "type": "long"},
        {"name": "source", "type": "string", "default": "api"}
      ]
    }
  }'

# Register Protobuf schema
curl -X POST http://localhost:8090/api/schemas/protobuf/user-events \
  -H "Content-Type: application/json" \
  -d '{
    "schemaType": "PROTOBUF",
    "schema": "..."
  }'
```

### 6. Test Multi-Format Production
```bash
# Send Avro message
curl -X POST http://localhost:8090/api/messages/avro \
  -H "Content-Type: application/json" \
  -d '{
    "topic": "user-events-avro",
    "userId": "user123",
    "email": "test@example.com",
    "source": "workshop"
  }'

# Send Protobuf message
curl -X POST http://localhost:8090/api/messages/protobuf \
  -H "Content-Type: application/json" \
  -d '{
    "topic": "user-events-protobuf", 
    "userId": "user456",
    "email": "proto@example.com",
    "source": "workshop"
  }'

# Send JSON message for comparison
curl -X POST http://localhost:8090/api/messages/json \
  -H "Content-Type: application/json" \
  -d '{
    "topic": "user-events-json",
    "userId": "user789", 
    "email": "json@example.com",
    "source": "workshop"
  }'
```

### 7. Test Schema Evolution
```bash
# Test backward compatibility
curl -X POST http://localhost:8090/api/schemas/test-compatibility \
  -H "Content-Type: application/json" \
  -d '{
    "subject": "user-events-value",
    "newSchema": {
      "type": "record",
      "name": "UserRegistered", 
      "fields": [
        {"name": "userId", "type": "string"},
        {"name": "email", "type": "string"},
        {"name": "timestamp", "type": "long"},
        {"name": "source", "type": "string", "default": "api"},
        {"name": "firstName", "type": ["null", "string"], "default": null}
      ]
    }
  }'
```

### 8. Run Performance Benchmarks
```bash
# Compare serialization performance
curl -X POST http://localhost:8090/api/benchmark/serialization \
  -H "Content-Type: application/json" \
  -d '{
    "messageCount": 10000,
    "formats": ["JSON", "AVRO", "PROTOBUF"]
  }'

# View benchmark results
curl http://localhost:8090/api/benchmark/results
```

### 9. Monitor Schema Registry
```bash
# List all registered schemas
curl http://localhost:8081/subjects

# Get schema versions
curl http://localhost:8081/subjects/user-events-value/versions

# Check compatibility settings
curl http://localhost:8081/config/user-events-value
```

### 10. View in Kafka UI
1. Open http://localhost:8080
2. Navigate to Topics → user-events-avro
3. View Avro-serialized messages with schema information
4. Compare payload sizes across different formats

## ✅ Success Criteria

- [ ] Schema Registry successfully integrated with producers/consumers
- [ ] Avro messages produced and consumed with proper schema evolution
- [ ] Protobuf messages demonstrate high-performance serialization
- [ ] Schema compatibility testing works for evolution scenarios
- [ ] Performance benchmarks show format differences
- [ ] Multi-format consumers handle different message types correctly

## 🔍 Debugging Tips

### Common Issues

1. **Schema Registry Connection Failures**
   ```bash
   # Check Schema Registry health
   curl http://localhost:8081/
   
   # Verify configuration
   curl http://localhost:8081/config
   ```

2. **Schema Compatibility Errors**
   ```bash
   # Test schema compatibility before registration
   curl -X POST http://localhost:8081/compatibility/subjects/user-events-value/versions/latest \
     -H "Content-Type: application/vnd.schemaregistry.v1+json" \
     -d '{"schema": "..."}'
   ```

3. **Serialization Failures**
   ```bash
   # Check producer logs for schema validation errors
   tail -f logs/application.log | grep "schema"
   
   # Verify schema is registered
   curl http://localhost:8081/subjects/user-events-value/versions/latest
   ```

### Useful Monitoring Commands

```bash
# Watch Schema Registry subjects
watch -n 5 'curl -s http://localhost:8081/subjects'

# Monitor serialization metrics
curl http://localhost:8090/actuator/metrics/kafka.producer.serialization.time

# Check consumer lag with schema info
docker exec kafka-starter-broker kafka-console-consumer \
  --topic user-events-avro \
  --from-beginning \
  --bootstrap-server localhost:9092 \
  --property print.schema.ids=true
```

## 📊 Learning Validation

### Understanding Check Questions

1. **Evolution**: What's the difference between backward and forward compatibility?
2. **Performance**: Why are binary formats (Avro/Protobuf) faster than JSON?
3. **Schema Design**: When would you use Avro vs Protobuf?
4. **Compatibility**: What changes break schema compatibility?
5. **Production**: How do you handle schema evolution in a live system?

### Hands-On Challenges

1. **Create schema evolution path** with 3 versions showing different compatibility types
2. **Implement custom serializer** for a complex nested data structure  
3. **Build schema migration tool** for updating all messages to new schema version
4. **Design schema strategy** for a multi-service architecture
5. **Optimize performance** by choosing the right serialization format per use case

## 🎯 Key Learning Outcomes

After completing this workshop, you'll understand:

### 🏗️ **Schema Management**
- How Schema Registry provides centralized schema governance
- Different compatibility strategies and their trade-offs
- Schema evolution patterns for production systems

### 🚀 **Performance Optimization**
- Binary serialization advantages over text formats
- When to choose Avro vs Protobuf vs JSON
- Performance tuning for high-throughput scenarios

### 🛡️ **Type Safety**
- Compile-time validation with generated classes
- Runtime schema validation and error handling
- Contract-first development with schema definitions

### 🔧 **Production Patterns**
- Schema versioning and lifecycle management
- Gradual rollout strategies for schema changes
- Monitoring and alerting for schema-related issues

## 🚀 Next Steps

Once you've mastered structured data with Schema Registry, you're ready for [Lesson 6: Development Tools](../lesson_6/concept.md), where you'll learn debugging, testing, and monitoring tools for production Kafka applications.

---

*Schema Registry transforms Kafka into a robust data platform with type safety and evolution capabilities. The structured data patterns you've learned are essential for enterprise-grade event-driven architectures.*