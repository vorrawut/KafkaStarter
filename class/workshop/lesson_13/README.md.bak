# Lesson 13 Workshop: Request-Reply Patterns with Kafka

## 🎯 Objective
Master request-reply patterns over Kafka for synchronous communication needs, implementing correlation-based responses, timeout handling, and high-performance request-response systems using asynchronous messaging.

## 📋 Workshop Tasks

### Task 1: Request-Reply Infrastructure
Implement request-reply in `requestreply/RequestReplyInfrastructure.kt`

### Task 2: Correlation Management
Build correlation handling in `correlation/CorrelationManager.kt`

### Task 3: Async Response Handling
Create response handling in `response/AsyncResponseHandler.kt`

### Task 4: Timeout Management
Implement timeouts in `timeout/TimeoutManager.kt`

### Task 5: Performance Optimization
Optimize performance in `optimization/RequestReplyOptimizer.kt`

## 🏗️ Request-Reply Architecture
```mermaid
graph TB
    subgraph "Client Applications"
        WEB[Web Application<br/>Synchronous API calls]
        MOBILE[Mobile App<br/>Real-time queries]
        API[External API<br/>Third-party integration]
        BATCH[Batch Jobs<br/>Bulk processing]
    end
    
    subgraph "Request-Reply Gateway"
        GATEWAY[Request-Reply Gateway<br/>Correlation management]
        CORRELATION[Correlation Store<br/>Redis/Memory]
        TIMEOUT[Timeout Handler<br/>Cleanup expired requests]
        RESPONSE_CACHE[Response Cache<br/>Performance optimization]
    end
    
    subgraph "Kafka Infrastructure"
        REQUEST_TOPIC[Request Topics<br/>user-query-requests<br/>order-lookup-requests<br/>analytics-requests]
        REPLY_TOPIC[Reply Topics<br/>user-query-responses<br/>order-lookup-responses<br/>analytics-responses]
    end
    
    subgraph "Backend Services"
        USER_SVC[User Service<br/>User data queries]
        ORDER_SVC[Order Service<br/>Order lookups]
        ANALYTICS_SVC[Analytics Service<br/>Complex analytics]
        INVENTORY_SVC[Inventory Service<br/>Stock queries]
    end
    
    subgraph "Response Processing"
        RESPONSE_ROUTER[Response Router<br/>Correlation-based routing]
        RESULT_AGGREGATOR[Result Aggregator<br/>Multi-service responses]
        ERROR_HANDLER[Error Handler<br/>Failure management]
    end
    
    WEB --> GATEWAY
    MOBILE --> GATEWAY
    API --> GATEWAY
    BATCH --> GATEWAY
    
    GATEWAY --> CORRELATION
    GATEWAY --> TIMEOUT
    GATEWAY --> RESPONSE_CACHE
    
    GATEWAY --> REQUEST_TOPIC
    REQUEST_TOPIC --> USER_SVC
    REQUEST_TOPIC --> ORDER_SVC
    REQUEST_TOPIC --> ANALYTICS_SVC
    REQUEST_TOPIC --> INVENTORY_SVC
    
    USER_SVC --> REPLY_TOPIC
    ORDER_SVC --> REPLY_TOPIC
    ANALYTICS_SVC --> REPLY_TOPIC
    INVENTORY_SVC --> REPLY_TOPIC
    
    REPLY_TOPIC --> RESPONSE_ROUTER
    RESPONSE_ROUTER --> RESULT_AGGREGATOR
    RESPONSE_ROUTER --> ERROR_HANDLER
    
    RESULT_AGGREGATOR --> GATEWAY
    ERROR_HANDLER --> GATEWAY
    
    style GATEWAY fill:#ff6b6b
    style CORRELATION fill:#4ecdc4
    style REQUEST_TOPIC fill:#a8e6cf
    style REPLY_TOPIC fill:#ffe66d
```

## 🔄 Request-Reply Flow
```mermaid
sequenceDiagram
    participant Client
    participant Gateway as Request-Reply Gateway
    participant Correlation as Correlation Store
    participant RequestTopic as Request Topic
    participant Service as Backend Service
    participant ResponseTopic as Response Topic
    participant ResponseRouter as Response Router
    
    Client->>Gateway: API Request (GET /users/123/profile)
    Gateway->>Gateway: Generate Correlation ID
    Gateway->>Correlation: Store Request Context
    
    Gateway->>RequestTopic: Publish Request + Correlation ID
    Gateway->>Gateway: Start Timeout Timer
    Gateway-->>Client: Return Future/Promise
    
    RequestTopic->>Service: Request Message
    Service->>Service: Process Request
    Service->>ResponseTopic: Publish Response + Correlation ID
    
    ResponseTopic->>ResponseRouter: Response Message
    ResponseRouter->>Correlation: Lookup Request Context
    ResponseRouter->>Gateway: Complete Request with Response
    
    Gateway->>Correlation: Remove Request Context
    Gateway-->>Client: Return Response Data
    
    Note over Gateway,Client: Request-Reply Complete
    
    alt Timeout Scenario
        Gateway->>Gateway: Timeout Expired
        Gateway->>Correlation: Remove Request Context
        Gateway-->>Client: Return Timeout Error
    end
```

## 🎯 Key Concepts

### **Request-Reply Benefits**
- **Synchronous Interface**: Traditional request-response semantics over async messaging
- **Scalability**: Leverage Kafka's horizontal scaling capabilities
- **Reliability**: Built-in retry and fault tolerance
- **Performance**: High-throughput request processing

### **Correlation Strategies**

#### **UUID-Based Correlation**
```mermaid
graph LR
    REQUEST[Request<br/>correlationId: uuid-123] --> KAFKA[Kafka Topic]
    KAFKA --> SERVICE[Backend Service]
    SERVICE --> RESPONSE[Response<br/>correlationId: uuid-123]
    RESPONSE --> ROUTER[Response Router]
    ROUTER --> MATCH[Match by UUID]
    
    style REQUEST fill:#ff6b6b
    style RESPONSE fill:#4ecdc4
    style MATCH fill:#a8e6cf
```

#### **Structured Correlation**
```kotlin
data class CorrelationId(
    val requestId: String,          // Unique request identifier
    val clientId: String,           // Client application identifier
    val sessionId: String?,         // User session identifier
    val timestamp: Long,            // Request timestamp
    val timeoutMs: Long,            // Request timeout
    val replyTopic: String,         // Where to send response
    val retryCount: Int = 0         // Number of retries
) {
    fun toCorrelationString(): String {
        return "$requestId:$clientId:${sessionId ?: "none"}:$timestamp:$timeoutMs:$replyTopic:$retryCount"
    }
    
    companion object {
        fun fromString(correlationString: String): CorrelationId {
            val parts = correlationString.split(":")
            return CorrelationId(
                requestId = parts[0],
                clientId = parts[1],
                sessionId = parts[2].takeIf { it != "none" },
                timestamp = parts[3].toLong(),
                timeoutMs = parts[4].toLong(),
                replyTopic = parts[5],
                retryCount = parts.getOrElse(6) { "0" }.toInt()
            )
        }
    }
}
```

## ⚙️ Request-Reply Infrastructure

### Request-Reply Gateway
```kotlin
@Component
class RequestReplyGateway {
    
    @Autowired
    private lateinit var kafkaTemplate: KafkaTemplate<String, Any>
    
    @Autowired
    private lateinit var correlationManager: CorrelationManager
    
    @Autowired
    private lateinit var timeoutManager: TimeoutManager
    
    fun <T> sendRequest(
        requestTopic: String,
        request: Any,
        responseType: Class<T>,
        timeoutMs: Long = 30000
    ): CompletableFuture<T> {
        
        val correlationId = generateCorrelationId()
        val future = CompletableFuture<T>()
        
        try {
            // Store request context
            val requestContext = RequestContext(
                correlationId = correlationId,
                requestTopic = requestTopic,
                responseType = responseType,
                future = future,
                timeoutMs = timeoutMs,
                startTime = System.currentTimeMillis()
            )
            
            correlationManager.storeRequestContext(correlationId, requestContext)
            
            // Prepare request message
            val requestMessage = RequestMessage(
                correlationId = correlationId,
                payload = request,
                timestamp = System.currentTimeMillis(),
                replyTopic = determineReplyTopic(requestTopic),
                timeoutMs = timeoutMs
            )
            
            // Send request
            kafkaTemplate.send(requestTopic, correlationId, requestMessage)
                .whenComplete { sendResult, throwable ->
                    if (throwable != null) {
                        correlationManager.removeRequestContext(correlationId)
                        future.completeExceptionally(
                            RequestSendException("Failed to send request", throwable)
                        )
                    } else {
                        // Start timeout monitoring
                        timeoutManager.scheduleTimeout(correlationId, timeoutMs)
                    }
                }
            
        } catch (e: Exception) {
            future.completeExceptionally(e)
        }
        
        return future
    }
    
    fun <T> sendBatchRequest(
        requestTopic: String,
        requests: List<Any>,
        responseType: Class<T>,
        timeoutMs: Long = 30000
    ): CompletableFuture<List<T>> {
        
        val batchId = UUID.randomUUID().toString()
        val futures = requests.mapIndexed { index, request ->
            val correlationId = "$batchId-$index"
            sendRequest(requestTopic, request, responseType, timeoutMs)
        }
        
        return CompletableFuture.allOf(*futures.toTypedArray())
            .thenApply { futures.map { it.get() } }
    }
    
    private fun generateCorrelationId(): String {
        return UUID.randomUUID().toString()
    }
    
    private fun determineReplyTopic(requestTopic: String): String {
        return requestTopic.replace("-requests", "-responses")
    }
}
```

### Response Handler
```kotlin
@Component
class AsyncResponseHandler {
    
    @Autowired
    private lateinit var correlationManager: CorrelationManager
    
    @Autowired
    private lateinit var timeoutManager: TimeoutManager
    
    @KafkaListener(topics = [
        "user-query-responses",
        "order-lookup-responses", 
        "analytics-responses",
        "inventory-query-responses"
    ])
    fun handleResponse(
        @Payload responseMessage: ResponseMessage,
        @Header(KafkaHeaders.RECEIVED_MESSAGE_KEY) correlationId: String,
        @Header(KafkaHeaders.RECEIVED_TOPIC) topic: String
    ) {
        try {
            logger.debug("Received response for correlation ID: $correlationId")
            
            val requestContext = correlationManager.getRequestContext(correlationId)
            if (requestContext == null) {
                logger.warn("No request context found for correlation ID: $correlationId")
                return
            }
            
            // Cancel timeout
            timeoutManager.cancelTimeout(correlationId)
            
            // Process response
            when {
                responseMessage.isSuccess -> {
                    completeRequestWithSuccess(requestContext, responseMessage)
                }
                responseMessage.isError -> {
                    completeRequestWithError(requestContext, responseMessage)
                }
                else -> {
                    completeRequestWithError(
                        requestContext, 
                        ResponseMessage.error(correlationId, "Invalid response format")
                    )
                }
            }
            
            // Clean up
            correlationManager.removeRequestContext(correlationId)
            
        } catch (e: Exception) {
            logger.error("Failed to handle response for correlation ID: $correlationId", e)
        }
    }
    
    private fun completeRequestWithSuccess(
        requestContext: RequestContext<*>,
        responseMessage: ResponseMessage
    ) {
        try {
            val responseData = when (requestContext.responseType) {
                String::class.java -> responseMessage.payload.toString()
                else -> objectMapper.convertValue(responseMessage.payload, requestContext.responseType)
            }
            
            @Suppress("UNCHECKED_CAST")
            val future = requestContext.future as CompletableFuture<Any>
            future.complete(responseData)
            
            // Record metrics
            recordResponseMetrics(requestContext, "success")
            
        } catch (e: Exception) {
            logger.error("Failed to deserialize response", e)
            completeRequestWithError(
                requestContext,
                ResponseMessage.error(requestContext.correlationId, "Response deserialization failed")
            )
        }
    }
    
    private fun completeRequestWithError(
        requestContext: RequestContext<*>,
        responseMessage: ResponseMessage
    ) {
        val exception = RequestReplyException(
            correlationId = requestContext.correlationId,
            errorCode = responseMessage.errorCode ?: "UNKNOWN_ERROR",
            errorMessage = responseMessage.errorMessage ?: "Unknown error occurred"
        )
        
        requestContext.future.completeExceptionally(exception)
        
        // Record metrics
        recordResponseMetrics(requestContext, "error")
    }
    
    private fun recordResponseMetrics(requestContext: RequestContext<*>, status: String) {
        val duration = System.currentTimeMillis() - requestContext.startTime
        
        meterRegistry.timer(
            "request.reply.duration",
            "topic", requestContext.requestTopic,
            "status", status
        ).record(duration, TimeUnit.MILLISECONDS)
        
        meterRegistry.counter(
            "request.reply.total",
            "topic", requestContext.requestTopic,
            "status", status
        ).increment()
    }
}
```

## ⏱️ Timeout Management

### Timeout Handler
```kotlin
@Component
class TimeoutManager {
    
    private val timeoutTasks = ConcurrentHashMap<String, ScheduledFuture<*>>()
    private val executorService = Executors.newScheduledThreadPool(10)
    
    @Autowired
    private lateinit var correlationManager: CorrelationManager
    
    fun scheduleTimeout(correlationId: String, timeoutMs: Long) {
        val timeoutTask = executorService.schedule({
            handleTimeout(correlationId)
        }, timeoutMs, TimeUnit.MILLISECONDS)
        
        timeoutTasks[correlationId] = timeoutTask
    }
    
    fun cancelTimeout(correlationId: String) {
        timeoutTasks.remove(correlationId)?.cancel(false)
    }
    
    private fun handleTimeout(correlationId: String) {
        logger.warn("Request timeout for correlation ID: $correlationId")
        
        val requestContext = correlationManager.getRequestContext(correlationId)
        if (requestContext != null) {
            val timeoutException = RequestTimeoutException(
                correlationId = correlationId,
                timeoutMs = requestContext.timeoutMs,
                message = "Request timed out after ${requestContext.timeoutMs}ms"
            )
            
            requestContext.future.completeExceptionally(timeoutException)
            correlationManager.removeRequestContext(correlationId)
            
            // Record timeout metrics
            meterRegistry.counter(
                "request.reply.timeout",
                "topic", requestContext.requestTopic
            ).increment()
        }
        
        // Clean up
        timeoutTasks.remove(correlationId)
    }
    
    @PreDestroy
    fun cleanup() {
        timeoutTasks.values.forEach { it.cancel(false) }
        executorService.shutdown()
    }
}
```

## 🎮 High-Level API Integration

### REST Controller with Request-Reply
```kotlin
@RestController
@RequestMapping("/api/users")
class UserQueryController {
    
    @Autowired
    private lateinit var requestReplyGateway: RequestReplyGateway
    
    @GetMapping("/{userId}/profile")
    fun getUserProfile(@PathVariable userId: String): CompletableFuture<ResponseEntity<UserProfile>> {
        
        val profileQuery = UserProfileQuery(
            userId = userId,
            includePreferences = true,
            includeActivity = false
        )
        
        return requestReplyGateway.sendRequest(
            requestTopic = "user-profile-requests",
            request = profileQuery,
            responseType = UserProfile::class.java,
            timeoutMs = 5000
        ).thenApply { profile ->
            ResponseEntity.ok(profile)
        }.exceptionally { throwable ->
            when (throwable.cause) {
                is RequestTimeoutException -> {
                    ResponseEntity.status(HttpStatus.REQUEST_TIMEOUT)
                        .body(null)
                }
                is RequestReplyException -> {
                    ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(null)
                }
                else -> {
                    ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(null)
                }
            }
        }
    }
    
    @GetMapping("/{userId}/orders")
    fun getUserOrders(
        @PathVariable userId: String,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int
    ): CompletableFuture<ResponseEntity<PagedResponse<OrderSummary>>> {
        
        val orderQuery = UserOrderQuery(
            userId = userId,
            page = page,
            size = size,
            includeDetails = false
        )
        
        return requestReplyGateway.sendRequest(
            requestTopic = "user-order-requests",
            request = orderQuery,
            responseType = object : TypeReference<PagedResponse<OrderSummary>>() {}.type,
            timeoutMs = 10000
        ).thenApply { orders ->
            ResponseEntity.ok(orders)
        }.exceptionally { throwable ->
            logger.error("Failed to retrieve user orders", throwable)
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null)
        }
    }
    
    @PostMapping("/batch-lookup")
    fun batchUserLookup(@RequestBody userIds: List<String>): CompletableFuture<ResponseEntity<List<UserSummary>>> {
        
        val batchQuery = UserBatchQuery(
            userIds = userIds,
            fields = listOf("id", "name", "email", "status")
        )
        
        return requestReplyGateway.sendRequest(
            requestTopic = "user-batch-requests",
            request = batchQuery,
            responseType = object : TypeReference<List<UserSummary>>() {}.type,
            timeoutMs = 15000
        ).thenApply { users ->
            ResponseEntity.ok(users)
        }.exceptionally { throwable ->
            logger.error("Failed to perform batch user lookup", throwable)
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null)
        }
    }
}
```

## ✅ Success Criteria
- [ ] Request-reply infrastructure handles synchronous communication over Kafka
- [ ] Correlation management correctly matches requests with responses
- [ ] Timeout handling prevents hanging requests
- [ ] Performance optimization achieves target throughput (>1000 req/sec)
- [ ] Error handling provides meaningful feedback to clients
- [ ] Batch requests work efficiently for bulk operations
- [ ] Integration with REST APIs is seamless

## 🚀 Getting Started

### 1. Configure Request-Reply Topics
```bash
# Create request-reply topic pairs
kafka-topics --create --topic user-profile-requests --partitions 6 --replication-factor 1 --bootstrap-server localhost:9092
kafka-topics --create --topic user-profile-responses --partitions 6 --replication-factor 1 --bootstrap-server localhost:9092

kafka-topics --create --topic order-lookup-requests --partitions 3 --replication-factor 1 --bootstrap-server localhost:9092
kafka-topics --create --topic order-lookup-responses --partitions 3 --replication-factor 1 --bootstrap-server localhost:9092

kafka-topics --create --topic analytics-requests --partitions 6 --replication-factor 1 --bootstrap-server localhost:9092
kafka-topics --create --topic analytics-responses --partitions 6 --replication-factor 1 --bootstrap-server localhost:9092
```

### 2. Test Request-Reply
```bash
# Send user profile request
curl http://localhost:8090/api/users/USER-123/profile

# Send batch user lookup
curl -X POST http://localhost:8090/api/users/batch-lookup \
  -H "Content-Type: application/json" \
  -d '["USER-123", "USER-456", "USER-789"]'

# Monitor request-reply metrics
curl http://localhost:8090/actuator/metrics/request.reply.duration
```

### 3. Monitor Performance
```bash
# Check correlation store size
curl http://localhost:8090/api/debug/correlation-store/size

# View active timeouts
curl http://localhost:8090/api/debug/timeouts/active

# Monitor request-reply topics
kafka-console-consumer --topic user-profile-requests --from-beginning --bootstrap-server localhost:9092
kafka-console-consumer --topic user-profile-responses --from-beginning --bootstrap-server localhost:9092
```

## 🎯 Best Practices

### Request-Reply Design
- **Use appropriate timeouts** based on expected response times
- **Implement idempotent operations** to handle retries safely
- **Design for failure** with proper error handling and fallbacks
- **Monitor correlation store** to prevent memory leaks

### Performance Optimization
- **Use connection pooling** for Kafka producers/consumers
- **Implement response caching** for frequently requested data
- **Batch similar requests** to reduce overhead
- **Tune partition counts** based on expected load

### Error Handling
- **Distinguish timeout vs processing errors** for appropriate responses
- **Implement circuit breakers** for failing services
- **Use dead letter topics** for unprocessable requests
- **Provide meaningful error messages** to API consumers

## 🔍 Troubleshooting

### Common Issues
1. **Memory leaks from orphaned correlations** - Implement proper cleanup
2. **High timeout rates** - Adjust timeout values and check service health
3. **Poor response times** - Optimize consumer processing and partition distribution
4. **Lost responses** - Verify topic configuration and consumer group setup

### Debug Commands
```bash
# Check correlation store contents
redis-cli KEYS "correlation:*"

# Monitor request-reply latency
curl 'http://localhost:9090/api/v1/query?query=histogram_quantile(0.95,rate(request_reply_duration_bucket[5m]))'

# View timeout statistics
curl http://localhost:8090/api/debug/timeout-stats
```

## 🚀 Next Steps
Request-reply mastered? Time for advanced state management! Move to [Lesson 16: Local State Stores & Fault Tolerance](../lesson_16/README.md) to learn persistent state in stream processing.