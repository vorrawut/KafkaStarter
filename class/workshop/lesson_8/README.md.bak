# Lesson 8 Workshop: Error Handling & Dead Letter Topics

## 🎯 Objective
Build robust, fault-tolerant Kafka applications with comprehensive error handling, retry strategies, and dead letter topic patterns for production resilience.

## 📋 Workshop Tasks

### Task 1: Error Classification
Implement error classification in `error/ErrorClassifier.kt`

### Task 2: Retry Strategies
Build retry mechanisms in `retry/RetryStrategyManager.kt`

### Task 3: Dead Letter Topic
Implement DLT handling in `dlt/DeadLetterTopicHandler.kt`

### Task 4: Circuit Breaker
Add circuit breaker pattern in `circuit/CircuitBreakerService.kt`

### Task 5: Error Monitoring
Monitor errors and recovery in `monitoring/ErrorMonitor.kt`

## 🏗️ Error Handling Architecture
```mermaid
graph TB
    subgraph "Message Processing Flow"
        MSG[Incoming Message] --> PROC[Process Message]
        PROC -->|Success| ACK[Acknowledge]
        PROC -->|Error| CLASSIFY[Classify Error]
        
        CLASSIFY -->|Retryable| RETRY[Retry Logic]
        CLASSIFY -->|Non-Retryable| DLT[Dead Letter Topic]
        CLASSIFY -->|Poison| SKIP[Skip & Log]
        
        RETRY -->|Max Retries| DLT
        RETRY -->|Retry Success| ACK
        RETRY -->|Backoff| WAIT[Exponential Backoff]
        WAIT --> PROC
        
        DLT --> DLTP[DLT Processing]
        DLTP -->|Manual Fix| REPLAY[Replay to Main Topic]
        DLTP -->|Permanent Failure| ARCHIVE[Archive Message]
    end
    
    subgraph "Circuit Breaker"
        CB[Circuit Breaker]
        CLOSED[Closed State<br/>Normal Operation]
        OPEN[Open State<br/>Fail Fast]
        HALF[Half-Open State<br/>Testing Recovery]
        
        CLOSED -->|Failure Threshold| OPEN
        OPEN -->|Timeout| HALF
        HALF -->|Success| CLOSED
        HALF -->|Failure| OPEN
    end
    
    PROC --> CB
    CB --> CLASSIFY
    
    style ACK fill:#4ecdc4
    style DLT fill:#ff6b6b
    style RETRY fill:#ffe66d
    style OPEN fill:#ff6b6b
    style CLOSED fill:#4ecdc4
```

## 🔄 Retry Strategy Flow
```mermaid
sequenceDiagram
    participant Consumer
    participant ErrorHandler
    participant RetryService
    participant DLT
    participant Monitor
    
    Consumer->>ErrorHandler: Processing Failed
    ErrorHandler->>ErrorHandler: Classify Error
    
    alt Retryable Error
        ErrorHandler->>RetryService: Schedule Retry
        RetryService->>RetryService: Exponential Backoff
        RetryService->>Consumer: Retry Processing
        
        alt Retry Success
            Consumer->>Monitor: Success After Retry
        else Max Retries Exceeded
            RetryService->>DLT: Send to Dead Letter Topic
            DLT->>Monitor: DLT Message Logged
        end
        
    else Non-Retryable Error
        ErrorHandler->>DLT: Send Directly to DLT
        DLT->>Monitor: Non-Retryable Error Logged
        
    else Poison Message
        ErrorHandler->>Monitor: Skip & Log Poison Message
    end
```

## 🎯 Key Concepts

### **Error Classification**
- **Retryable Errors**: Temporary failures (network issues, service unavailable)
- **Non-Retryable Errors**: Permanent failures (validation errors, malformed data)
- **Poison Messages**: Messages that consistently cause processing failures

### **Retry Strategies**

#### **1. Exponential Backoff**
```mermaid
graph LR
    R1[Retry 1<br/>Wait: 1s] --> R2[Retry 2<br/>Wait: 2s]
    R2 --> R3[Retry 3<br/>Wait: 4s]
    R3 --> R4[Retry 4<br/>Wait: 8s]
    R4 --> R5[Retry 5<br/>Wait: 16s]
    R5 --> DLT[Dead Letter Topic]
    
    style DLT fill:#ff6b6b
```

#### **2. Fixed Interval**
```mermaid
graph LR
    R1[Retry 1<br/>Wait: 5s] --> R2[Retry 2<br/>Wait: 5s]
    R2 --> R3[Retry 3<br/>Wait: 5s]
    R3 --> DLT[Dead Letter Topic]
    
    style DLT fill:#ff6b6b
```

#### **3. Linear Backoff**
```mermaid
graph LR
    R1[Retry 1<br/>Wait: 2s] --> R2[Retry 2<br/>Wait: 4s]
    R2 --> R3[Retry 3<br/>Wait: 6s]
    R3 --> R4[Retry 4<br/>Wait: 8s]
    R4 --> DLT[Dead Letter Topic]
    
    style DLT fill:#ff6b6b
```

### **Dead Letter Topic Patterns**

#### **Single DLT Pattern**
```mermaid
graph TB
    MAIN[main-topic] -->|Processing Failure| DLT[main-topic-dlt]
    DLT --> MANUAL[Manual Investigation]
    MANUAL --> REPLAY[Replay to Main Topic]
    
    style DLT fill:#ff6b6b
    style REPLAY fill:#4ecdc4
```

#### **Multi-Level DLT Pattern**
```mermaid
graph TB
    MAIN[main-topic] -->|Retryable Failure| RETRY[main-topic-retry]
    RETRY -->|Retry Exhausted| DLT[main-topic-dlt]
    MAIN -->|Non-Retryable| DLT
    DLT -->|Investigation| ARCHIVE[main-topic-archive]
    
    style RETRY fill:#ffe66d
    style DLT fill:#ff6b6b
    style ARCHIVE fill:#a8e6cf
```

## ⚙️ Circuit Breaker Pattern

### Circuit Breaker States
```mermaid
stateDiagram-v2
    [*] --> Closed
    Closed --> Open: Failure threshold exceeded
    Open --> HalfOpen: Timeout period elapsed
    HalfOpen --> Closed: Success threshold met
    HalfOpen --> Open: Failure detected
    
    Closed: Normal Operation<br/>All requests processed
    Open: Fail Fast<br/>Reject all requests
    HalfOpen: Testing Recovery<br/>Limited requests allowed
```

### Implementation Example
```kotlin
@Component
class PaymentServiceCircuitBreaker {
    
    private var state = CircuitBreakerState.CLOSED
    private var failureCount = 0
    private var lastFailureTime = 0L
    private val failureThreshold = 5
    private val recoveryTimeout = 60000L // 1 minute
    
    fun processPayment(payment: Payment): PaymentResult {
        when (state) {
            CircuitBreakerState.OPEN -> {
                if (System.currentTimeMillis() - lastFailureTime > recoveryTimeout) {
                    state = CircuitBreakerState.HALF_OPEN
                } else {
                    throw CircuitBreakerOpenException("Payment service unavailable")
                }
            }
            CircuitBreakerState.HALF_OPEN -> {
                // Limited requests allowed
            }
            CircuitBreakerState.CLOSED -> {
                // Normal operation
            }
        }
        
        return try {
            val result = paymentService.processPayment(payment)
            onSuccess()
            result
        } catch (e: Exception) {
            onFailure()
            throw e
        }
    }
}
```

## ✅ Success Criteria
- [ ] Error classification correctly identifies retryable vs non-retryable errors
- [ ] Retry mechanism with exponential backoff is working
- [ ] Dead letter topic receives failed messages after max retries
- [ ] Circuit breaker protects downstream services from cascading failures
- [ ] Error monitoring provides visibility into failure patterns
- [ ] Messages can be replayed from DLT back to main topic
- [ ] Poison message detection and handling works correctly

## 🚀 Getting Started

### 1. Configure Error Handling
```kotlin
@Configuration
class ErrorHandlingConfig {
    
    @Bean
    fun errorHandlingContainerFactory(): ConcurrentKafkaListenerContainerFactory<String, OrderEvent> {
        val factory = ConcurrentKafkaListenerContainerFactory<String, OrderEvent>()
        factory.consumerFactory = consumerFactory()
        
        // Configure error handler
        val errorHandler = SeekToCurrentErrorHandler(
            DeadLetterPublishingRecoverer(kafkaTemplate()) { record, _ ->
                TopicPartition("order-events-dlt", record.partition())
            }
        ).apply {
            setRetryTemplate(retryTemplate())
        }
        
        factory.setCommonErrorHandler(errorHandler)
        return factory
    }
    
    @Bean
    fun retryTemplate(): RetryTemplate {
        return RetryTemplate.builder()
            .maxAttempts(3)
            .exponentialBackoff(1000, 2.0, 10000)
            .retryOn(RetryableException::class.java)
            .build()
    }
}
```

### 2. Test Error Scenarios
```bash
# Send valid message
curl -X POST http://localhost:8090/api/orders \
  -H "Content-Type: application/json" \
  -d '{"orderId": "123", "amount": 100.0}'

# Send invalid message (triggers non-retryable error)
curl -X POST http://localhost:8090/api/orders \
  -H "Content-Type: application/json" \
  -d '{"orderId": "", "amount": -100.0}'

# Simulate service failure (triggers retryable error)
curl -X POST http://localhost:8090/admin/simulate-failure \
  -d '{"service": "payment", "duration": 30000}'
```

### 3. Monitor Error Topics
```bash
# Monitor main topic
kafka-console-consumer --topic order-events --from-beginning --bootstrap-server localhost:9092

# Monitor dead letter topic
kafka-console-consumer --topic order-events-dlt --from-beginning --bootstrap-server localhost:9092

# Monitor retry topic
kafka-console-consumer --topic order-events-retry --from-beginning --bootstrap-server localhost:9092
```

## 🔧 Advanced Error Handling Patterns

### Custom Error Classification
```kotlin
@Service
class PaymentErrorClassifier {
    
    fun classifyError(exception: Exception): ErrorType {
        return when (exception) {
            is SocketTimeoutException -> ErrorType.RETRYABLE
            is ConnectException -> ErrorType.RETRYABLE
            is ServiceUnavailableException -> ErrorType.RETRYABLE
            
            is ValidationException -> ErrorType.NON_RETRYABLE
            is IllegalArgumentException -> ErrorType.NON_RETRYABLE
            is JsonParseException -> ErrorType.POISON_MESSAGE
            
            is PaymentDeclinedException -> ErrorType.BUSINESS_ERROR
            is InsufficientFundsException -> ErrorType.BUSINESS_ERROR
            
            else -> ErrorType.UNKNOWN
        }
    }
}
```

### DLT Message Enrichment
```kotlin
@Service
class DeadLetterEnrichmentService {
    
    fun enrichDLTMessage(
        originalMessage: OrderEvent,
        exception: Exception,
        attemptCount: Int
    ): DeadLetterMessage {
        return DeadLetterMessage(
            originalMessage = originalMessage,
            errorDetails = ErrorDetails(
                errorType = exception.javaClass.simpleName,
                errorMessage = exception.message ?: "Unknown error",
                stackTrace = exception.stackTraceToString(),
                firstFailureTime = System.currentTimeMillis(),
                attemptCount = attemptCount,
                lastAttemptTime = System.currentTimeMillis()
            ),
            metadata = mapOf(
                "originalTopic" to "order-events",
                "consumerGroup" to "order-processors",
                "processingHost" to InetAddress.getLocalHost().hostName
            )
        )
    }
}
```

## 📊 Error Monitoring & Alerting

### Key Error Metrics
```mermaid
graph TB
    subgraph "Error Rate Metrics"
        ER1[Total Error Rate<br/>errors/minute]
        ER2[Error Rate by Type<br/>retryable vs non-retryable]
        ER3[DLT Message Rate<br/>messages/hour]
        ER4[Retry Success Rate<br/>percentage]
    end
    
    subgraph "Performance Metrics"
        PM1[Average Retry Attempts<br/>before success]
        PM2[Circuit Breaker State<br/>open/closed/half-open]
        PM3[Processing Latency<br/>with retries]
        PM4[DLT Processing Lag<br/>time to investigation]
    end
    
    subgraph "Business Metrics"
        BM1[Revenue Impact<br/>from failed orders]
        BM2[Customer Impact<br/>failed transactions]
        BM3[SLA Compliance<br/>error rate vs target]
        BM4[Recovery Time<br/>from failure to fix]
    end
    
    style ER1 fill:#ff6b6b
    style PM2 fill:#ffe66d
    style BM1 fill:#ff6b6b
```

### Health Check with Error Rates
```kotlin
@Component
class ErrorHandlingHealthIndicator : HealthIndicator {
    
    override fun health(): Health {
        val errorRateThreshold = 5.0 // 5% error rate threshold
        val currentErrorRate = calculateErrorRate()
        val dltMessageCount = getDLTMessageCount()
        
        return when {
            currentErrorRate > errorRateThreshold -> {
                Health.down()
                    .withDetail("reason", "High error rate detected")
                    .withDetail("errorRate", currentErrorRate)
                    .withDetail("threshold", errorRateThreshold)
                    .build()
            }
            dltMessageCount > 100 -> {
                Health.degraded()
                    .withDetail("reason", "High DLT message count")
                    .withDetail("dltMessageCount", dltMessageCount)
                    .build()
            }
            else -> {
                Health.up()
                    .withDetail("errorRate", currentErrorRate)
                    .withDetail("dltMessageCount", dltMessageCount)
                    .build()
            }
        }
    }
}
```

## 🎯 Best Practices

### Error Handling Design
- **Classify errors properly** - distinguish between temporary and permanent failures
- **Implement idempotent processing** - handle duplicate messages gracefully
- **Use structured logging** - include correlation IDs and context
- **Monitor error patterns** - identify systemic issues early

### Retry Strategy Selection
- **Exponential backoff** - for network-related failures
- **Fixed interval** - for predictable service outages
- **No retry** - for validation and business logic errors
- **Circuit breaker** - for protecting downstream services

### Dead Letter Topic Management
- **Separate DLT per topic** - easier management and monitoring
- **Enrich DLT messages** - include error context and debugging info
- **Automate replay** - when possible, automatically retry DLT messages
- **Archive old messages** - prevent DLT from growing indefinitely

## 🔍 Troubleshooting

### Common Issues
1. **Infinite retry loops** - Check error classification logic
2. **DLT overflow** - Implement DLT message archival
3. **Circuit breaker stuck open** - Verify recovery conditions
4. **Lost messages** - Ensure proper offset management during retries

### Debug Commands
```bash
# Check DLT message count
kafka-run-class kafka.tools.GetOffsetShell \
  --broker-list localhost:9092 \
  --topic order-events-dlt

# Analyze error patterns
kafka-console-consumer --topic order-events-dlt \
  --from-beginning --bootstrap-server localhost:9092 \
  --property print.headers=true

# Monitor consumer group lag including DLT consumers
kafka-consumer-groups --bootstrap-server localhost:9092 \
  --group dlt-processors --describe
```

## 🚀 Next Steps
Error handling mastered? Time to ensure exactly-once processing! Move to [Lesson 9: Manual Acknowledgment & Idempotent Consumers](../lesson_9/README.md) to learn precise message processing control.