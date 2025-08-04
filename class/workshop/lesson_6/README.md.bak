# Lesson 6 Workshop: Development Tools & Testing Framework

## 🎯 Objective
Master essential Kafka development tools, debugging techniques, testing strategies, and monitoring setups for productive and reliable Kafka application development.

## 📋 Workshop Tasks

### Task 1: Kafka CLI Mastery
Practice CLI operations in `cli/KafkaCliOperations.kt`

### Task 2: Testing Framework
Build comprehensive testing in `testing/KafkaTestFramework.kt`

### Task 3: Debugging Tools
Implement debugging utilities in `debugging/KafkaDebugger.kt`

### Task 4: Development Monitoring
Set up dev monitoring in `monitoring/DevMonitoring.kt`

### Task 5: Performance Profiling
Create profiling tools in `profiling/PerformanceProfiler.kt`

## 🛠️ Development Toolkit Architecture
```mermaid
graph TB
    subgraph "Development Environment"
        IDE[IntelliJ IDEA<br/>Kotlin Development]
        CLI[Kafka CLI Tools<br/>Topic/Consumer Management]
        UI[Kafka UI<br/>Visual Interface]
        DEBUG[Debug Tools<br/>Message Inspector]
    end
    
    subgraph "Testing Framework"
        UNIT[Unit Tests<br/>Business Logic]
        INTEGRATION[Integration Tests<br/>Embedded Kafka]
        CONTRACT[Contract Tests<br/>Schema Validation]
        PERFORMANCE[Performance Tests<br/>Load Testing]
    end
    
    subgraph "Monitoring & Observability"
        METRICS[Application Metrics<br/>Micrometer + Prometheus]
        LOGS[Structured Logging<br/>JSON + Correlation IDs]
        TRACES[Distributed Tracing<br/>Sleuth + Zipkin]
        HEALTH[Health Checks<br/>Actuator Endpoints]
    end
    
    subgraph "Kafka Infrastructure"
        BROKER[Kafka Brokers]
        ZK[Zookeeper]
        SR[Schema Registry]
        TOPICS[Topics & Partitions]
    end
    
    IDE --> CLI
    CLI --> BROKER
    UI --> BROKER
    DEBUG --> BROKER
    
    UNIT --> INTEGRATION
    INTEGRATION --> CONTRACT
    CONTRACT --> PERFORMANCE
    
    METRICS --> LOGS
    LOGS --> TRACES
    TRACES --> HEALTH
    
    INTEGRATION --> BROKER
    METRICS --> BROKER
    
    style CLI fill:#ff6b6b
    style INTEGRATION fill:#4ecdc4
    style METRICS fill:#a8e6cf
    style BROKER fill:#ffe66d
```

## 🔧 Kafka CLI Mastery

### Essential CLI Commands
```mermaid
graph TB
    subgraph "Topic Management"
        TC[kafka-topics<br/>Create, list, describe, delete]
        CONFIG[kafka-configs<br/>View and modify configurations]
        REASSIGN[kafka-reassign-partitions<br/>Partition reassignment]
    end
    
    subgraph "Producer/Consumer Operations"
        PROD[kafka-console-producer<br/>Send test messages]
        CONS[kafka-console-consumer<br/>Read messages]
        PERF[kafka-producer-perf-test<br/>Performance testing]
    end
    
    subgraph "Consumer Group Management"
        CG[kafka-consumer-groups<br/>List, describe, reset offsets]
        LOG[kafka-run-class GetOffsetShell<br/>Check offsets]
        RESET[kafka-consumer-groups --reset-offsets<br/>Offset management]
    end
    
    subgraph "Cluster Operations"
        BROKER[kafka-broker-api-versions<br/>Broker connectivity]
        LEADER[kafka-leader-election<br/>Leadership management]
        VERIFY[kafka-verifiable-producer/consumer<br/>End-to-end testing]
    end
    
    TC --> CONFIG
    CONFIG --> REASSIGN
    
    PROD --> CONS
    CONS --> PERF
    
    CG --> LOG
    LOG --> RESET
    
    BROKER --> LEADER
    LEADER --> VERIFY
    
    style TC fill:#ff6b6b
    style PROD fill:#4ecdc4
    style CG fill:#a8e6cf
    style BROKER fill:#ffe66d
```

### CLI Automation Scripts
```bash
#!/bin/bash
# Topic management helper
create_topic() {
    local topic_name=$1
    local partitions=${2:-3}
    local replication=${3:-1}
    
    kafka-topics --create \
        --topic "$topic_name" \
        --partitions "$partitions" \
        --replication-factor "$replication" \
        --bootstrap-server localhost:9092
}

# Consumer group monitoring
monitor_consumer_lag() {
    local group_id=$1
    
    while true; do
        echo "=== Consumer Group Lag: $(date) ==="
        kafka-consumer-groups --bootstrap-server localhost:9092 \
            --group "$group_id" --describe
        echo ""
        sleep 10
    done
}

# Performance testing
test_producer_performance() {
    local topic=$1
    local num_records=${2:-10000}
    local record_size=${3:-1024}
    
    kafka-producer-perf-test \
        --topic "$topic" \
        --num-records "$num_records" \
        --record-size "$record_size" \
        --throughput -1 \
        --producer-props bootstrap.servers=localhost:9092
}
```

## 🧪 Testing Framework

### Testing Strategy Pyramid
```mermaid
graph TB
    subgraph "Testing Pyramid"
        E2E[End-to-End Tests<br/>Full system integration<br/>Slow, expensive, comprehensive]
        INTEGRATION[Integration Tests<br/>Kafka + Spring Boot<br/>Medium speed, realistic]
        UNIT[Unit Tests<br/>Business logic only<br/>Fast, isolated, focused]
    end
    
    subgraph "Kafka-Specific Testing"
        EMBEDDED[Embedded Kafka<br/>@EmbeddedKafka annotation]
        TESTCONTAINERS[TestContainers<br/>Docker-based testing]
        TOPOLOGY[Topology Testing<br/>Kafka Streams validation]
        SCHEMA[Schema Testing<br/>Avro/Protobuf validation]
    end
    
    subgraph "Test Data Management"
        FIXTURES[Test Fixtures<br/>Predefined test data]
        BUILDERS[Test Builders<br/>Fluent test data creation]
        FACTORIES[Test Factories<br/>Random test data generation]
    end
    
    UNIT --> INTEGRATION
    INTEGRATION --> E2E
    
    INTEGRATION --> EMBEDDED
    INTEGRATION --> TESTCONTAINERS
    
    EMBEDDED --> TOPOLOGY
    TESTCONTAINERS --> SCHEMA
    
    TOPOLOGY --> FIXTURES
    SCHEMA --> BUILDERS
    FIXTURES --> FACTORIES
    
    style E2E fill:#ff6b6b
    style INTEGRATION fill:#4ecdc4
    style UNIT fill:#a8e6cf
    style EMBEDDED fill:#ffe66d
```

### Test Implementation Patterns
```kotlin
@SpringBootTest
@EmbeddedKafka(partitions = 1, topics = ["test-topic"])
class KafkaIntegrationTest {
    
    @Autowired
    private lateinit var kafkaTemplate: KafkaTemplate<String, Any>
    
    @Autowired
    private lateinit var userEventService: UserEventService
    
    @Test
    fun `should process user registration event end-to-end`() {
        // Given
        val testEvent = UserEvent(
            eventId = "test-123",
            eventType = "USER_REGISTERED",
            userId = "user-456",
            username = "testuser",
            email = "test@example.com"
        )
        
        // When
        kafkaTemplate.send("user-events", testEvent.userId, testEvent)
        
        // Then
        await().atMost(5, SECONDS).untilAsserted {
            val processedEvent = userEventService.getProcessedEvent(testEvent.eventId)
            assertThat(processedEvent).isNotNull
            assertThat(processedEvent.status).isEqualTo("PROCESSED")
        }
    }
}
```

## 🔍 Debugging Tools

### Message Flow Debugging
```mermaid
sequenceDiagram
    participant Dev as Developer
    participant Debug as Debug Tool
    participant Producer as Producer App
    participant Kafka as Kafka Broker
    participant Consumer as Consumer App
    
    Dev->>Debug: Start message tracing
    Debug->>Producer: Intercept outgoing messages
    Producer->>Kafka: Send message with trace ID
    Debug->>Kafka: Monitor topic for traced messages
    Kafka->>Consumer: Deliver message
    Debug->>Consumer: Intercept incoming messages
    Consumer->>Debug: Processing result
    Debug->>Dev: Complete message flow trace
    
    Note over Debug: Correlation ID tracking
    Note over Debug: Timing measurements
    Note over Debug: Error detection
```

### Debug Tool Implementation
```kotlin
@Component
class KafkaMessageTracer {
    
    private val messageTraces = ConcurrentHashMap<String, MessageTrace>()
    
    fun startTrace(correlationId: String): MessageTrace {
        val trace = MessageTrace(
            correlationId = correlationId,
            startTime = System.currentTimeMillis(),
            checkpoints = mutableListOf()
        )
        messageTraces[correlationId] = trace
        return trace
    }
    
    fun addCheckpoint(correlationId: String, checkpoint: String, metadata: Map<String, Any> = emptyMap()) {
        messageTraces[correlationId]?.let { trace ->
            trace.checkpoints.add(
                TraceCheckpoint(
                    timestamp = System.currentTimeMillis(),
                    checkpoint = checkpoint,
                    metadata = metadata
                )
            )
        }
    }
    
    fun completeTrace(correlationId: String): MessageTrace? {
        return messageTraces.remove(correlationId)?.apply {
            endTime = System.currentTimeMillis()
            duration = endTime - startTime
        }
    }
    
    fun getActiveTraces(): List<MessageTrace> {
        return messageTraces.values.toList()
    }
}
```

## ✅ Success Criteria
- [ ] Can efficiently use Kafka CLI tools for all common operations
- [ ] Comprehensive testing framework with unit, integration, and E2E tests
- [ ] Debugging tools provide clear visibility into message flows
- [ ] Development monitoring shows real-time application metrics
- [ ] Performance profiling identifies bottlenecks and optimization opportunities
- [ ] Error scenarios can be reproduced and debugged systematically
- [ ] Code coverage meets team standards (>80% for critical paths)

## 🚀 Getting Started

### 1. Set Up Testing Framework
```kotlin
@TestConfiguration
class KafkaTestConfig {
    
    @Bean
    @Primary
    fun testKafkaTemplate(): KafkaTemplate<String, Any> {
        val producerFactory = DefaultKafkaProducerFactory<String, Any>(
            mapOf(
                ProducerConfig.BOOTSTRAP_SERVERS_CONFIG to "localhost:9092",
                ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG to StringSerializer::class.java,
                ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG to JsonSerializer::class.java,
                ProducerConfig.ACKS_CONFIG to "all",
                ProducerConfig.RETRIES_CONFIG to 0,
                ProducerConfig.BATCH_SIZE_CONFIG to 1
            )
        )
        
        return KafkaTemplate(producerFactory)
    }
    
    @Bean
    fun testListener(): CountDownLatch {
        return CountDownLatch(1)
    }
}
```

### 2. Practice CLI Operations
```bash
# Create development topic
kafka-topics --create --topic dev-events \
  --partitions 3 --replication-factor 1 \
  --bootstrap-server localhost:9092

# Send test messages
echo "test-key:test-message" | kafka-console-producer \
  --topic dev-events --bootstrap-server localhost:9092 \
  --property "parse.key=true" --property "key.separator=:"

# Monitor messages
kafka-console-consumer --topic dev-events \
  --from-beginning --bootstrap-server localhost:9092 \
  --property print.key=true

# Check consumer group
kafka-consumer-groups --bootstrap-server localhost:9092 \
  --group dev-group --describe
```

### 3. Set Up Development Monitoring
```yaml
# application-dev.yml
management:
  endpoints:
    web:
      exposure:
        include: "*"
  endpoint:
    health:
      show-details: always
    metrics:
      enabled: true
  metrics:
    export:
      prometheus:
        enabled: true
    tags:
      application: kafka-starter
      environment: development

logging:
  level:
    org.springframework.kafka: DEBUG
    org.apache.kafka: INFO
  pattern:
    console: "%d{yyyy-MM-dd HH:mm:ss} [%thread] %-5level [%X{correlationId}] %logger{36} - %msg%n"
```

## 📊 Performance Profiling

### Performance Metrics Dashboard
```mermaid
graph TB
    subgraph "Application Metrics"
        THROUGHPUT[Message Throughput<br/>messages/second]
        LATENCY[Processing Latency<br/>95th percentile]
        ERROR_RATE[Error Rate<br/>errors/minute]
        MEMORY[Memory Usage<br/>heap utilization]
    end
    
    subgraph "Kafka Metrics"
        PRODUCER_RATE[Producer Rate<br/>records/second]
        CONSUMER_LAG[Consumer Lag<br/>messages behind]
        BATCH_SIZE[Batch Size<br/>records/batch]
        COMMIT_RATE[Commit Rate<br/>commits/second]
    end
    
    subgraph "System Metrics"
        CPU[CPU Usage<br/>percentage]
        DISK_IO[Disk I/O<br/>read/write rates]
        NETWORK[Network I/O<br/>bytes in/out]
        GC[Garbage Collection<br/>pause times]
    end
    
    subgraph "Business Metrics"
        ORDER_RATE[Order Processing Rate<br/>orders/minute]
        USER_EVENTS[User Event Rate<br/>events/minute]
        REVENUE[Revenue Impact<br/>from processing delays]
        SLA[SLA Compliance<br/>% within targets]
    end
    
    THROUGHPUT --> PRODUCER_RATE
    LATENCY --> CONSUMER_LAG
    ERROR_RATE --> COMMIT_RATE
    
    CPU --> THROUGHPUT
    MEMORY --> LATENCY
    DISK_IO --> BATCH_SIZE
    
    ORDER_RATE --> THROUGHPUT
    USER_EVENTS --> PRODUCER_RATE
    
    style THROUGHPUT fill:#4ecdc4
    style LATENCY fill:#ffe66d
    style ERROR_RATE fill:#ff6b6b
    style SLA fill:#a8e6cf
```

### Automated Performance Testing
```kotlin
@Component
class PerformanceProfiler {
    
    fun profileProducerPerformance(
        topicName: String,
        messageCount: Int,
        messageSize: Int
    ): ProducerPerformanceResult {
        
        val startTime = System.currentTimeMillis()
        val futures = mutableListOf<CompletableFuture<SendResult<String, ByteArray>>>()
        
        // Generate test data
        val testMessage = ByteArray(messageSize) { it.toByte() }
        
        // Send messages and collect futures
        repeat(messageCount) { index ->
            val future = kafkaTemplate.send(topicName, "key-$index", testMessage)
            futures.add(future)
        }
        
        // Wait for all sends to complete
        CompletableFuture.allOf(*futures.toTypedArray()).get()
        
        val endTime = System.currentTimeMillis()
        val duration = endTime - startTime
        
        return ProducerPerformanceResult(
            messageCount = messageCount,
            messageSize = messageSize,
            durationMs = duration,
            throughputMsgsPerSec = (messageCount * 1000.0) / duration,
            throughputMBPerSec = (messageCount * messageSize * 1000.0) / (duration * 1024 * 1024)
        )
    }
}
```

## 🎯 Best Practices

### Development Workflow
- **Use version control** for all configuration and test data
- **Automate repetitive tasks** with scripts and aliases
- **Test early and often** with embedded Kafka for fast feedback
- **Monitor continuously** even in development environments

### Testing Strategy
- **Start with unit tests** for business logic
- **Use integration tests** for Kafka interactions
- **Add contract tests** for schema validation
- **Include performance tests** for throughput requirements

### Debugging Approach
- **Use correlation IDs** to trace messages across services
- **Log structured data** with consistent formatting
- **Monitor key metrics** to identify issues quickly
- **Reproduce issues** in controlled test environments

## 🔍 Troubleshooting

### Common Development Issues
1. **Slow tests** - Use @DirtiesContext sparingly, prefer test slices
2. **Flaky tests** - Add proper wait conditions and timeouts
3. **Memory leaks** - Monitor test execution and clean up resources
4. **Port conflicts** - Use random ports or test containers

### Debug Commands
```bash
# Check if Kafka is running
kafka-broker-api-versions --bootstrap-server localhost:9092

# Debug consumer issues
kafka-console-consumer --topic your-topic \
  --bootstrap-server localhost:9092 \
  --property print.headers=true \
  --property print.timestamp=true

# Monitor JVM metrics
jcmd <pid> VM.info
jstack <pid>
```

## 🚀 Next Steps
Development tools mastered? Time to scale with consumer groups! Move to [Lesson 7: Consumer Groups & Load Balancing](../lesson_7/README.md) to learn parallel processing patterns.