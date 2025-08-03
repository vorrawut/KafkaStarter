# Lesson 3 Workshop: First Producer/Consumer - Hello Kafka!

## 🎯 Objective
Build your first Kafka application with Spring Boot and Kotlin, implementing both producer and consumer patterns with proper error handling and monitoring.

## 📋 Workshop Tasks

### Task 1: Configure Kafka Integration
Complete the Spring Kafka configuration in `config/KafkaConfig.kt`

### Task 2: Create Event Models
Define your event data structures in `model/UserEvent.kt`

### Task 3: Implement Producer
Build a Kafka producer service in `service/UserEventProducer.kt`

### Task 4: Implement Consumer
Build a Kafka consumer service in `service/UserEventConsumer.kt`

### Task 5: Create REST Controller
Build REST endpoints for testing in `controller/UserController.kt`

## 🧠 Key Concepts
- Spring Boot Kafka integration
- Producer/Consumer patterns
- JSON serialization/deserialization
- Error handling and retry logic
- Consumer groups and partitioning
- Event-driven architecture basics

## 🔄 Producer-Consumer Flow
```mermaid
sequenceDiagram
    participant Client as REST Client
    participant Controller as UserController
    participant Producer as UserEventProducer
    participant Kafka as Kafka Broker
    participant Consumer as UserEventConsumer
    participant DB as Database
    
    Client->>Controller: POST /api/users
    Controller->>Producer: publishUserEvent()
    Producer->>Kafka: Send UserEvent
    Controller-->>Client: 201 Created
    
    Kafka->>Consumer: Deliver UserEvent
    Consumer->>DB: Process & Store
    Consumer->>Kafka: Acknowledge
```

## 🏗️ Architecture Overview
```mermaid
graph TB
    subgraph "Your Spring Boot Application"
        REST[REST Controller<br/>:8090/api/users]
        PRODUCER[User Event Producer]
        CONSUMER[User Event Consumer]
        CONFIG[Kafka Configuration]
    end
    
    subgraph "Kafka Ecosystem"
        BROKER[Kafka Broker<br/>:9092]
        TOPIC[user-events Topic<br/>3 partitions]
    end
    
    subgraph "Data Flow"
        CLIENT[HTTP Client] --> REST
        REST --> PRODUCER
        PRODUCER --> BROKER
        BROKER --> TOPIC
        TOPIC --> CONSUMER
    end
    
    style BROKER fill:#ff6b6b
    style PRODUCER fill:#4ecdc4
    style CONSUMER fill:#a8e6cf
```

## ✅ Success Criteria
- [ ] Application starts without errors
- [ ] Can send events via REST API
- [ ] Events appear in Kafka topics (check Kafka UI)
- [ ] Consumer processes events successfully
- [ ] Proper error handling implemented
- [ ] Integration tests pass

## 🚀 Getting Started

### 1. Start Your Environment
```bash
# Make sure Kafka is running from Lesson 2
cd docker
docker-compose ps

# Start your Spring Boot application
./gradlew bootRun
```

### 2. Test the API
```bash
# Create a user event
curl -X POST http://localhost:8090/api/users \
  -H "Content-Type: application/json" \
  -d '{
    "userId": "123",
    "username": "john_doe",
    "email": "john@example.com",
    "eventType": "USER_CREATED"
  }'

# Check health
curl http://localhost:8090/actuator/health
```

### 3. Monitor Events
- **Kafka UI**: http://localhost:8080/ui/clusters/local/topics/user-events
- **Application Logs**: Check console for consumer processing
- **Metrics**: http://localhost:8090/actuator/metrics

## 🔍 Event Flow Verification
```mermaid
graph LR
    A[Send REST Request] --> B[Controller Receives]
    B --> C[Producer Publishes]
    C --> D[Event in Kafka]
    D --> E[Consumer Processes]
    E --> F[Event Acknowledged]
    
    G[Check Kafka UI] --> D
    H[Check Logs] --> E
    
    style D fill:#ff6b6b
    style C fill:#4ecdc4
    style E fill:#a8e6cf
```

## 🔧 Troubleshooting

### Common Issues:
- **Connection refused**: Ensure Kafka is running (`docker-compose ps`)
- **Serialization errors**: Check JSON format and model annotations
- **Consumer not receiving**: Verify topic name and consumer group configuration
- **Port conflicts**: Make sure port 8090 is available

### Debug Commands:
```bash
# Check topics
kafka-topics --list --bootstrap-server localhost:9092

# Monitor events in real-time
kafka-console-consumer --topic user-events --from-beginning --bootstrap-server localhost:9092

# Check consumer groups
kafka-consumer-groups --bootstrap-server localhost:9092 --list
```

## 📊 What You'll Learn
- **Event-Driven Thinking**: How to design systems around events
- **Kafka Integration**: Production-ready Spring Boot + Kafka patterns
- **Async Processing**: Benefits of decoupled, asynchronous systems
- **Error Handling**: Robust patterns for handling failures
- **Monitoring**: How to observe event-driven systems

## 🚀 Next Steps
Once you've successfully published and consumed your first events, move to [Lesson 4: Topics, Partitions & Offsets](../lesson_4/README.md) to dive deeper into Kafka's storage model!