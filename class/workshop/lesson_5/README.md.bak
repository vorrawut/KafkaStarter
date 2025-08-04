# Lesson 5 Workshop: Schema Registry & Evolution

## 🎯 Objective
Master schema management with Confluent Schema Registry, implementing Avro and Protobuf schemas with proper evolution strategies for production systems.

## 📋 Workshop Tasks

### Task 1: Schema Definition & Registration
Create and register schemas in `schemas/UserEventSchema.kt`

### Task 2: Avro Implementation
Implement Avro serialization in `avro/AvroUserEventProducer.kt` and `avro/AvroUserEventConsumer.kt`

### Task 3: Protobuf Implementation  
Implement Protobuf serialization in `protobuf/ProtobufUserEventProducer.kt` and `protobuf/ProtobufUserEventConsumer.kt`

### Task 4: Schema Evolution
Practice schema evolution strategies in `evolution/SchemaEvolutionManager.kt`

### Task 5: Performance Comparison
Compare serialization performance in `performance/SerializationBenchmark.kt`

## 🏗️ Schema Registry Architecture
```mermaid
graph TB
    subgraph "Schema Registry (:8081)"
        SR[Schema Registry Server]
        STORE[Schema Storage]
        COMPAT[Compatibility Checker]
        VERSIONS[Version Manager]
    end
    
    subgraph "Producers"
        PROD1[JSON Producer]
        PROD2[Avro Producer]
        PROD3[Protobuf Producer]
    end
    
    subgraph "Consumers"
        CONS1[JSON Consumer]
        CONS2[Avro Consumer]
        CONS3[Protobuf Consumer]
    end
    
    subgraph "Kafka Topics"
        T1[user-events-json]
        T2[user-events-avro]
        T3[user-events-protobuf]
    end
    
    PROD2 -->|Register Schema| SR
    PROD3 -->|Register Schema| SR
    
    PROD1 -->|Raw JSON| T1
    PROD2 -->|Schema ID + Binary| T2
    PROD3 -->|Schema ID + Binary| T3
    
    T1 --> CONS1
    T2 --> CONS2
    T3 --> CONS3
    
    CONS2 -->|Fetch Schema| SR
    CONS3 -->|Fetch Schema| SR
    
    style SR fill:#ff6b6b
    style T2 fill:#4ecdc4
    style T3 fill:#a8e6cf
```

## 🔄 Schema Evolution Flow
```mermaid
sequenceDiagram
    participant Dev as Developer
    participant SR as Schema Registry
    participant PROD as Producer
    participant CONS as Consumer
    
    Dev->>SR: Register Schema v1
    SR-->>Dev: Schema ID: 1
    
    PROD->>SR: Get Schema v1
    PROD->>PROD: Serialize with Schema ID 1
    PROD->>Kafka: Send [Schema ID 1][Binary Data]
    
    Kafka->>CONS: Deliver Message
    CONS->>SR: Fetch Schema ID 1
    CONS->>CONS: Deserialize with Schema v1
    
    Note over Dev,CONS: Schema Evolution
    Dev->>SR: Register Schema v2 (backward compatible)
    SR->>SR: Compatibility Check
    SR-->>Dev: Schema ID: 2
    
    PROD->>SR: Get Schema v2
    PROD->>PROD: Serialize with Schema ID 2
    PROD->>Kafka: Send [Schema ID 2][Binary Data]
    
    Kafka->>CONS: Deliver Message
    Note over CONS: Old consumer can still read v2 data!
    CONS->>SR: Fetch Schema ID 2
    CONS->>CONS: Deserialize with compatibility rules
```

## 🎯 Key Concepts

### **Schema Registry Benefits**
- **Type Safety**: Compile-time checking of data structures
- **Evolution**: Safe schema changes without breaking consumers
- **Efficiency**: Binary serialization reduces message size
- **Documentation**: Self-documenting data contracts
- **Governance**: Centralized schema management

### **Serialization Formats**

#### **JSON** (Human-readable)
```json
{
  "eventId": "evt-123",
  "eventType": "USER_CREATED",
  "userId": "user-456",
  "username": "john_doe",
  "email": "john@example.com",
  "timestamp": 1645123456789
}
```

#### **Avro** (Schema evolution focus)
```json
{
  "type": "record",
  "name": "UserEvent",
  "fields": [
    {"name": "eventId", "type": "string"},
    {"name": "eventType", "type": "string"},
    {"name": "userId", "type": "string"},
    {"name": "username", "type": "string"},
    {"name": "email", "type": "string"},
    {"name": "timestamp", "type": "long"}
  ]
}
```

#### **Protobuf** (Performance focus)
```protobuf
syntax = "proto3";

message UserEvent {
  string event_id = 1;
  string event_type = 2;
  string user_id = 3;
  string username = 4;
  string email = 5;
  int64 timestamp = 6;
}
```

## ⚡ Performance Comparison
```mermaid
graph TB
    subgraph "Serialization Performance"
        JSON[JSON<br/>Size: 150 bytes<br/>Speed: Fast<br/>CPU: Low]
        AVRO[Avro<br/>Size: 45 bytes<br/>Speed: Very Fast<br/>CPU: Low]
        PROTO[Protobuf<br/>Size: 42 bytes<br/>Speed: Fastest<br/>CPU: Very Low]
    end
    
    subgraph "Schema Evolution Support"
        JSON_EV[JSON<br/>Manual handling<br/>No built-in compatibility]
        AVRO_EV[Avro<br/>Excellent evolution<br/>Multiple compatibility modes]
        PROTO_EV[Protobuf<br/>Good evolution<br/>Field number-based]
    end
    
    subgraph "Use Cases"
        JSON_UC[JSON<br/>• Development<br/>• Debugging<br/>• Simple APIs]
        AVRO_UC[Avro<br/>• Data lakes<br/>• ETL pipelines<br/>• Analytics]
        PROTO_UC[Protobuf<br/>• High throughput<br/>• Microservices<br/>• Real-time systems]
    end
    
    style AVRO fill:#4ecdc4
    style PROTO fill:#a8e6cf
    style JSON fill:#ffe66d
```

## 🔄 Schema Evolution Strategies

### **Backward Compatibility** (Consumers can read new data)
```mermaid
graph LR
    V1[Schema v1<br/>Fields: A, B] --> V2[Schema v2<br/>Fields: A, B, C]
    V2 --> OLD[Old Consumer<br/>Reads A, B<br/>Ignores C]
    
    style V2 fill:#4ecdc4
    style OLD fill:#a8e6cf
```

### **Forward Compatibility** (Consumers can read old data)
```mermaid
graph LR
    V1[Schema v1<br/>Fields: A, B, C] --> V2[Schema v2<br/>Fields: A, B]
    V2 --> NEW[New Consumer<br/>Expects A, B<br/>C has default value]
    
    style V2 fill:#4ecdc4
    style NEW fill:#a8e6cf
```

### **Full Compatibility** (Both directions work)
```mermaid
graph TB
    V1[Schema v1] <--> V2[Schema v2]
    V2 <--> V3[Schema v3]
    
    OLD[Old Consumers] --> V1
    NEW[New Consumers] --> V3
    
    style V2 fill:#4ecdc4
```

## ✅ Success Criteria
- [ ] Schema Registry accessible and healthy
- [ ] Can register and retrieve schemas
- [ ] Avro producer/consumer working with binary serialization
- [ ] Protobuf producer/consumer working with binary serialization
- [ ] Schema evolution scenarios tested
- [ ] Performance benchmarks completed
- [ ] Compatibility rules understood and applied

## 🚀 Getting Started

### 1. Verify Schema Registry
```bash
# Check Schema Registry health
curl http://localhost:8081/subjects

# List all schemas
curl http://localhost:8081/subjects
```

### 2. Register Your First Schema
```bash
# Register Avro schema
curl -X POST http://localhost:8081/subjects/user-events-avro-value/versions \
  -H "Content-Type: application/vnd.schemaregistry.v1+json" \
  -d '{"schema": "{\"type\":\"record\",\"name\":\"UserEvent\",\"fields\":[{\"name\":\"eventId\",\"type\":\"string\"}]}"}'
```

### 3. Test Serialization Performance
```bash
# Run performance benchmark
./gradlew test --tests SerializationBenchmarkTest

# Compare message sizes
./gradlew bootRun --args="--benchmark.mode=size"
```

## 🔧 Schema Evolution Examples

### Adding Optional Fields (Backward Compatible)
```kotlin
// Schema v1
data class UserEventV1(
    val eventId: String,
    val userId: String,
    val eventType: String
)

// Schema v2 - Added optional field with default
data class UserEventV2(
    val eventId: String,
    val userId: String,
    val eventType: String,
    val metadata: Map<String, String> = emptyMap() // NEW FIELD
)
```

### Removing Fields (Forward Compatible)
```kotlin
// Schema v1
data class UserEventV1(
    val eventId: String,
    val userId: String,
    val eventType: String,
    val deprecatedField: String // TO BE REMOVED
)

// Schema v2 - Field removed
data class UserEventV2(
    val eventId: String,
    val userId: String,
    val eventType: String
    // deprecatedField removed
)
```

## 🔍 Monitoring & Debugging

### Schema Registry Metrics
- **Schema registration rate**
- **Compatibility check failures**
- **Schema fetch latency**
- **Storage usage**

### Debug Tools
```bash
# View schema by ID
curl http://localhost:8081/schemas/ids/1

# Check compatibility
curl -X POST http://localhost:8081/compatibility/subjects/user-events-avro-value/versions/latest \
  -H "Content-Type: application/vnd.schemaregistry.v1+json" \
  -d '{"schema": "..."}'

# List schema versions
curl http://localhost:8081/subjects/user-events-avro-value/versions
```

## 🚀 Next Steps
Schema management mastered? Time to explore development tools! Move to [Lesson 6: Development Tools](../lesson_6/README.md) to learn debugging, testing, and monitoring techniques for Kafka applications.