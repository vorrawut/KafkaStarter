# Workshop: Observability & Monitoring

## 🎯 Objective
Implement comprehensive observability for production Kafka systems including metrics collection, distributed tracing, structured logging, alerting, and real-time dashboards for operational excellence.

## 📋 Workshop Tasks

### Task 1: Metrics Collection
Implement metrics in `metrics/KafkaMetricsCollector.kt`

### Task 2: Distributed Tracing
Build tracing in `tracing/DistributedTracer.kt`

### Task 3: Structured Logging
Create logging framework in `logging/StructuredLogger.kt`

### Task 4: Health Monitoring
Implement health checks in `health/SystemHealthMonitor.kt`

### Task 5: Alert Management
Build alerting in `alerting/AlertManager.kt`

## 🏗️ Observability Architecture
```mermaid
graph TB
    subgraph "Kafka Infrastructure"
        KAFKA[Kafka Brokers<br/>JMX metrics exposed]
        ZK[Zookeeper<br/>Health monitoring]
        SR[Schema Registry<br/>Schema metrics]
        CONNECT[Kafka Connect<br/>Connector metrics]
    end
    
    subgraph "Application Layer"
        PRODUCER[Producer Apps<br/>Custom metrics]
        CONSUMER[Consumer Apps<br/>Processing metrics]
        STREAMS[Streams Apps<br/>Topology metrics]
        REST[REST APIs<br/>HTTP metrics]
    end
    
    subgraph "Metrics Collection"
        JMX[JMX Exporter<br/>Kafka metrics]
        MICROMETER[Micrometer<br/>Application metrics]
        PROMETHEUS[Prometheus<br/>Metrics storage]
        CUSTOM[Custom Collectors<br/>Business metrics]
    end
    
    subgraph "Tracing Infrastructure"
        JAEGER[Jaeger<br/>Trace collection]
        ZIPKIN[Zipkin<br/>Trace visualization]
        SLEUTH[Spring Cloud Sleuth<br/>Auto-instrumentation]
        OTEL[OpenTelemetry<br/>Vendor-neutral tracing]
    end
    
    subgraph "Logging Pipeline"
        LOGBACK[Logback<br/>Structured logging]
        FLUENTD[Fluentd<br/>Log collection]
        ELASTICSEARCH[Elasticsearch<br/>Log storage]
        KIBANA[Kibana<br/>Log analysis]
    end
    
    subgraph "Monitoring & Alerting"
        GRAFANA[Grafana<br/>Dashboards]
        ALERT_MANAGER[AlertManager<br/>Alert routing]
        PAGERDUTY[PagerDuty<br/>Incident management]
        SLACK[Slack<br/>Team notifications]
    end
    
    KAFKA --> JMX
    PRODUCER --> MICROMETER
    CONSUMER --> MICROMETER
    STREAMS --> MICROMETER
    REST --> MICROMETER
    
    JMX --> PROMETHEUS
    MICROMETER --> PROMETHEUS
    CUSTOM --> PROMETHEUS
    
    PRODUCER --> SLEUTH
    CONSUMER --> SLEUTH
    SLEUTH --> JAEGER
    SLEUTH --> ZIPKIN
    
    PRODUCER --> LOGBACK
    CONSUMER --> LOGBACK
    LOGBACK --> FLUENTD
    FLUENTD --> ELASTICSEARCH
    
    PROMETHEUS --> GRAFANA
    PROMETHEUS --> ALERT_MANAGER
    ELASTICSEARCH --> KIBANA
    
    ALERT_MANAGER --> PAGERDUTY
    ALERT_MANAGER --> SLACK
    
    style PROMETHEUS fill:#ff6b6b
    style GRAFANA fill:#4ecdc4
    style JAEGER fill:#a8e6cf
    style ELASTICSEARCH fill:#ffe66d
```

## 📊 Comprehensive Metrics Strategy

### Three Pillars of Observability
```mermaid
graph TB
    subgraph "Metrics (What's Happening)"
        BUSINESS[Business Metrics<br/>Orders/sec, Revenue/hour]
        APPLICATION[Application Metrics<br/>Latency, Throughput, Errors]
        INFRASTRUCTURE[Infrastructure Metrics<br/>CPU, Memory, Disk, Network]
        KAFKA_METRICS[Kafka Metrics<br/>Producer rate, Consumer lag, Partition count]
    end
    
    subgraph "Logs (What Happened)"
        STRUCTURED[Structured Logs<br/>JSON format with context]
        AUDIT[Audit Logs<br/>Security and compliance]
        ERROR[Error Logs<br/>Exceptions and failures]
        CORRELATION[Correlation IDs<br/>Request tracing]
    end
    
    subgraph "Traces (How It Happened)"
        DISTRIBUTED[Distributed Traces<br/>Cross-service calls]
        SPANS[Spans<br/>Individual operations]
        CONTEXT[Trace Context<br/>Baggage and tags]
        SAMPLING[Sampling Strategy<br/>Performance optimization]
    end
    
    subgraph "Dashboards & Alerts"
        REAL_TIME[Real-time Dashboards<br/>Live system status]
        ALERTS[Intelligent Alerts<br/>Threshold-based notifications]
        RUNBOOKS[Automated Runbooks<br/>Self-healing systems]
        INCIDENTS[Incident Management<br/>Response coordination]
    end
    
    BUSINESS --> REAL_TIME
    APPLICATION --> ALERTS
    INFRASTRUCTURE --> ALERTS
    KAFKA_METRICS --> REAL_TIME
    
    STRUCTURED --> INCIDENTS
    AUDIT --> INCIDENTS
    ERROR --> ALERTS
    
    DISTRIBUTED --> REAL_TIME
    SPANS --> INCIDENTS
    
    REAL_TIME --> RUNBOOKS
    ALERTS --> INCIDENTS
    
    style BUSINESS fill:#ff6b6b
    style STRUCTURED fill:#4ecdc4
    style DISTRIBUTED fill:#a8e6cf
    style REAL_TIME fill:#ffe66d
```

## 📈 Kafka-Specific Metrics

### Producer Metrics
```kotlin
@Component
class KafkaProducerMetrics {
    
    private val meterRegistry: MeterRegistry
    private val producerMetrics = ConcurrentHashMap<String, Timer>()
    
    @EventListener
    fun handleProducerSendStart(event: ProducerSendStartEvent) {
        val timer = Timer.start(meterRegistry)
        producerMetrics[event.correlationId] = timer
        
        // Track send attempts
        meterRegistry.counter(
            "kafka.producer.send.attempts",
            "topic", event.topic,
            "partition", event.partition?.toString() ?: "unknown"
        ).increment()
    }
    
    @EventListener
    fun handleProducerSendSuccess(event: ProducerSendSuccessEvent) {
        val timer = producerMetrics.remove(event.correlationId)
        timer?.stop(Timer.Sample.start(meterRegistry).stop(
            Timer.builder("kafka.producer.send.duration")
                .tag("topic", event.topic)
                .tag("status", "success")
                .register(meterRegistry)
        ))
        
        // Track batch size
        meterRegistry.gauge(
            "kafka.producer.batch.size",
            Tags.of("topic", event.topic),
            event.batchSize
        )
        
        // Track record size
        meterRegistry.summary(
            "kafka.producer.record.size",
            "topic", event.topic
        ).record(event.recordSize.toDouble())
    }
    
    @EventListener
    fun handleProducerSendFailure(event: ProducerSendFailureEvent) {
        val timer = producerMetrics.remove(event.correlationId)
        timer?.stop(Timer.Sample.start(meterRegistry).stop(
            Timer.builder("kafka.producer.send.duration")
                .tag("topic", event.topic)
                .tag("status", "failure")
                .tag("error", event.exception.javaClass.simpleName)
                .register(meterRegistry)
        ))
        
        // Track error types
        meterRegistry.counter(
            "kafka.producer.errors",
            "topic", event.topic,
            "error.type", event.exception.javaClass.simpleName
        ).increment()
    }
}
```

### Consumer Metrics
```kotlin
@Component
class KafkaConsumerMetrics {
    
    @Autowired
    private lateinit var meterRegistry: MeterRegistry
    
    @Scheduled(fixedDelay = 30000) // Every 30 seconds
    fun collectConsumerLagMetrics() {
        AdminClient.create(adminConfig).use { adminClient ->
            try {
                val consumerGroups = adminClient.listConsumerGroups().all().get()
                
                consumerGroups.forEach { group ->
                    if (group.state() == ConsumerGroupState.STABLE) {
                        val groupDescription = adminClient.describeConsumerGroups(listOf(group.groupId()))
                            .all().get()[group.groupId()]
                        
                        val assignments = groupDescription?.members()?.flatMap { 
                            it.assignment().topicPartitions() 
                        } ?: emptyList()
                        
                        val endOffsets = adminClient.listOffsets(
                            assignments.associateWith { OffsetSpec.latest() }
                        ).all().get()
                        
                        val groupOffsets = adminClient.listConsumerGroupOffsets(group.groupId())
                            .partitionsToOffsetAndMetadata().get()
                        
                        assignments.forEach { tp ->
                            val endOffset = endOffsets[tp]?.offset() ?: 0
                            val currentOffset = groupOffsets[tp]?.offset() ?: 0
                            val lag = endOffset - currentOffset
                            
                            meterRegistry.gauge(
                                "kafka.consumer.lag",
                                Tags.of(
                                    "group", group.groupId(),
                                    "topic", tp.topic(),
                                    "partition", tp.partition().toString()
                                ),
                                lag.toDouble()
                            )
                        }
                    }
                }
            } catch (e: Exception) {
                logger.error("Failed to collect consumer lag metrics", e)
            }
        }
    }
    
    @KafkaListener(topics = [".*"], topicPattern = ".*")
    fun recordProcessingMetrics(
        @Payload message: Any,
        @Header(KafkaHeaders.RECEIVED_TOPIC) topic: String,
        @Header(KafkaHeaders.RECEIVED_PARTITION_ID) partition: Int,
        @Header(KafkaHeaders.OFFSET) offset: Long
    ) {
        // Record message processing
        meterRegistry.counter(
            "kafka.consumer.messages.processed",
            "topic", topic,
            "partition", partition.toString()
        ).increment()
        
        // Record processing time
        val processingTimer = Timer.start(meterRegistry)
        // Processing happens in actual listener
        processingTimer.stop(
            Timer.builder("kafka.consumer.processing.duration")
                .tag("topic", topic)
                .register(meterRegistry)
        )
    }
}
```

## 🔍 Distributed Tracing Implementation

### Tracing Configuration
```kotlin
@Configuration
class TracingConfiguration {
    
    @Bean
    fun jaegerTracer(): io.jaegertracing.Configuration {
        return io.jaegertracing.Configuration("kafka-starter")
            .withSampler(
                io.jaegertracing.Configuration.SamplerConfiguration()
                    .withType("const")
                    .withParam(1) // Sample all traces in development
            )
            .withReporter(
                io.jaegertracing.Configuration.ReporterConfiguration()
                    .withLogSpans(true)
                    .withFlushInterval(1000)
                    .withMaxQueueSize(10000)
                    .withSender(
                        io.jaegertracing.Configuration.SenderConfiguration()
                            .withAgentHost("localhost")
                            .withAgentPort(6831)
                    )
            )
    }
    
    @Bean
    fun kafkaTracingInterceptor(): KafkaTracingInterceptor {
        return KafkaTracingInterceptor()
    }
}

@Component
class KafkaTracingInterceptor : ProducerInterceptor<String, Any>, ConsumerInterceptor<String, Any> {
    
    private val tracer = GlobalTracer.get()
    
    override fun onSend(record: ProducerRecord<String, Any>): ProducerRecord<String, Any> {
        val span = tracer.activeSpan()
        
        if (span != null) {
            // Inject trace context into Kafka headers
            val headers = record.headers()
            tracer.inject(
                span.context(),
                Format.Builtin.TEXT_MAP,
                HeadersTextMapExtract(headers)
            )
            
            // Add span tags
            span.setTag("kafka.topic", record.topic())
            span.setTag("kafka.partition", record.partition()?.toString() ?: "unknown")
            span.setTag("kafka.key", record.key() ?: "null")
            span.setTag("component", "kafka-producer")
        }
        
        return record
    }
    
    override fun onConsume(records: ConsumerRecords<String, Any>): ConsumerRecords<String, Any> {
        records.forEach { record ->
            // Extract trace context from Kafka headers
            val spanContext = tracer.extract(
                Format.Builtin.TEXT_MAP,
                HeadersTextMapExtract(record.headers())
            )
            
            // Create child span for message processing
            val spanBuilder = tracer.buildSpan("kafka-message-processing")
                .withTag("kafka.topic", record.topic())
                .withTag("kafka.partition", record.partition().toString())
                .withTag("kafka.offset", record.offset().toString())
                .withTag("component", "kafka-consumer")
            
            if (spanContext != null) {
                spanBuilder.asChildOf(spanContext)
            }
            
            val span = spanBuilder.start()
            tracer.activateSpan(span).use {
                // Span will be active during message processing
            }
        }
        
        return records
    }
}
```

### Cross-Service Tracing
```kotlin
@Service
class OrderProcessingService {
    
    @Autowired
    private lateinit var kafkaTemplate: KafkaTemplate<String, Any>
    
    @Traced(operationName = "order-processing")
    fun processOrder(order: Order): OrderResult {
        val span = tracer.activeSpan()
        span?.setTag("order.id", order.id)
        span?.setTag("order.amount", order.totalAmount.toString())
        span?.setTag("customer.id", order.customerId)
        
        return try {
            // Step 1: Validate order
            val validationSpan = tracer.buildSpan("validate-order")
                .asChildOf(span)
                .start()
            
            tracer.activateSpan(validationSpan).use {
                validateOrder(order)
                validationSpan.setTag("validation.result", "success")
            }
            
            // Step 2: Process payment
            val paymentSpan = tracer.buildSpan("process-payment")
                .asChildOf(span)
                .start()
            
            val paymentResult = tracer.activateSpan(paymentSpan).use {
                val result = paymentService.processPayment(order)
                paymentSpan.setTag("payment.result", result.status.name)
                paymentSpan.setTag("payment.amount", result.amount.toString())
                result
            }
            
            // Step 3: Publish order event
            val eventSpan = tracer.buildSpan("publish-order-event")
                .asChildOf(span)
                .start()
            
            tracer.activateSpan(eventSpan).use {
                val orderEvent = OrderProcessedEvent(
                    orderId = order.id,
                    status = OrderStatus.PROCESSED,
                    traceId = span?.context()?.toTraceId() ?: "unknown"
                )
                
                kafkaTemplate.send("order-events", order.id, orderEvent)
                eventSpan.setTag("event.published", true)
            }
            
            OrderResult.success(order.id)
            
        } catch (e: Exception) {
            span?.setTag("error", true)
            span?.setTag("error.message", e.message ?: "Unknown error")
            span?.log(mapOf("event" to "error", "error.object" to e))
            
            OrderResult.failure(order.id, e.message ?: "Processing failed")
        }
    }
}
```

## 📝 Structured Logging Framework

### Logging Configuration
```kotlin
@Configuration
class LoggingConfiguration {
    
    @Bean
    fun structuredLogger(): StructuredLogger {
        return StructuredLogger()
    }
    
    @Bean
    fun logbackEncoderCustomizer(): LogbackEncoderCustomizer {
        return LogbackEncoderCustomizer { encoder ->
            if (encoder is JsonEncoder) {
                encoder.includeContext = true
                encoder.includeMdc = true
                encoder.includeStructuredArguments = true
            }
        }
    }
}

@Component
class StructuredLogger {
    
    private val logger = LoggerFactory.getLogger(StructuredLogger::class.java)
    
    fun logOrderEvent(
        level: Level,
        event: String,
        orderId: String,
        customerId: String?,
        details: Map<String, Any> = emptyMap()
    ) {
        MDC.put("event.type", event)
        MDC.put("order.id", orderId)
        MDC.put("customer.id", customerId ?: "unknown")
        MDC.put("timestamp", Instant.now().toString())
        
        val logData = mapOf(
            "event" to event,
            "orderId" to orderId,
            "customerId" to customerId,
            "details" to details,
            "traceId" to getCurrentTraceId(),
            "spanId" to getCurrentSpanId()
        )
        
        when (level) {
            Level.INFO -> logger.info(marker("ORDER_EVENT"), "{}", logData)
            Level.WARN -> logger.warn(marker("ORDER_EVENT"), "{}", logData)
            Level.ERROR -> logger.error(marker("ORDER_EVENT"), "{}", logData)
            else -> logger.debug(marker("ORDER_EVENT"), "{}", logData)
        }
        
        MDC.clear()
    }
    
    fun logKafkaEvent(
        level: Level,
        operation: String,
        topic: String,
        partition: Int?,
        offset: Long?,
        key: String?,
        error: Throwable? = null
    ) {
        MDC.put("operation", operation)
        MDC.put("topic", topic)
        MDC.put("partition", partition?.toString() ?: "unknown")
        MDC.put("offset", offset?.toString() ?: "unknown")
        MDC.put("message.key", key ?: "null")
        
        val logData = mutableMapOf<String, Any>(
            "operation" to operation,
            "topic" to topic,
            "partition" to (partition ?: -1),
            "offset" to (offset ?: -1),
            "messageKey" to (key ?: "null"),
            "traceId" to getCurrentTraceId(),
            "timestamp" to Instant.now()
        )
        
        if (error != null) {
            logData["error"] = mapOf(
                "type" to error.javaClass.simpleName,
                "message" to (error.message ?: "No error message"),
                "stackTrace" to error.stackTraceToString()
            )
        }
        
        when (level) {
            Level.ERROR -> logger.error(marker("KAFKA_EVENT"), "{}", logData, error)
            Level.WARN -> logger.warn(marker("KAFKA_EVENT"), "{}", logData)
            Level.INFO -> logger.info(marker("KAFKA_EVENT"), "{}", logData)
            else -> logger.debug(marker("KAFKA_EVENT"), "{}", logData)
        }
        
        MDC.clear()
    }
    
    private fun getCurrentTraceId(): String {
        return tracer.activeSpan()?.context()?.toTraceId() ?: "no-trace"
    }
    
    private fun getCurrentSpanId(): String {
        return tracer.activeSpan()?.context()?.toSpanId() ?: "no-span"
    }
    
    private fun marker(name: String): Marker {
        return MarkerFactory.getMarker(name)
    }
}
```

## ✅ Success Criteria
- [ ] Comprehensive metrics collection for all Kafka components
- [ ] Distributed tracing works across microservices and Kafka
- [ ] Structured logging provides actionable insights
- [ ] Health monitoring detects issues before they impact users
- [ ] Alerting system notifies teams of critical issues
- [ ] Dashboards provide real-time visibility into system health
- [ ] Performance impact of observability is minimal (&lt;5% overhead)

## 🚀 Getting Started

### 1. Configure Observability Stack
```yaml
# docker-compose-observability.yml
version: '3.8'
services:
  prometheus:
    image: prom/prometheus:latest
    ports:
      - "9090:9090"
    volumes:
      - ./prometheus.yml:/etc/prometheus/prometheus.yml
      
  grafana:
    image: grafana/grafana:latest
    ports:
      - "3000:3000"
    environment:
      - GF_SECURITY_ADMIN_PASSWORD=admin
    volumes:
      - grafana-storage:/var/lib/grafana
      
  jaeger:
    image: jaegertracing/all-in-one:latest
    ports:
      - "16686:16686"
      - "6831:6831/udp"
    environment:
      - COLLECTOR_ZIPKIN_HTTP_PORT=9411
      
  elasticsearch:
    image: elasticsearch:7.10.0
    ports:
      - "9200:9200"
    environment:
      - discovery.type=single-node
      - "ES_JAVA_OPTS=-Xms512m -Xmx512m"
      
  kibana:
    image: kibana:7.10.0
    ports:
      - "5601:5601"
    depends_on:
      - elasticsearch
```

### 2. Test Observability
```bash
# Start observability stack
docker-compose -f docker-compose-observability.yml up -d

# Generate test load
curl -X POST http://localhost:8090/api/orders/generate-load \
  -d '{"orderCount": 1000, "ratePerSecond": 50}'

# Check metrics
curl http://localhost:9090/api/v1/query?query=kafka_producer_send_total

# View traces
open http://localhost:16686

# Check logs
curl http://localhost:9200/logs-*/_search?q=level:ERROR
```

### 3. Configure Dashboards
```bash
# Import Kafka dashboard
curl -X POST http://admin:admin@localhost:3000/api/dashboards/db \
  -H "Content-Type: application/json" \
  -d @kafka-dashboard.json

# Import application metrics dashboard
curl -X POST http://admin:admin@localhost:3000/api/dashboards/db \
  -H "Content-Type: application/json" \
  -d @application-dashboard.json
```

## 🎯 Best Practices

### Metrics Strategy
- **Focus on business metrics** first (orders/sec, revenue/hour)
- **Use RED method** (Rate, Errors, Duration) for services
- **Use USE method** (Utilization, Saturation, Errors) for resources
- **Implement SLIs and SLOs** for service level objectives

### Logging Strategy
- **Use structured logging** with consistent format
- **Include correlation IDs** for request tracing
- **Log at appropriate levels** to reduce noise
- **Implement log sampling** for high-volume systems

### Tracing Strategy
- **Sample traces intelligently** to balance coverage and performance
- **Use semantic tags** for meaningful searchability
- **Implement baggage** for cross-cutting concerns
- **Monitor trace latency** to ensure performance

## 🔍 Troubleshooting

### Common Issues
1. **High cardinality metrics** - Limit tag values and use sampling
2. **Trace sampling gaps** - Adjust sampling rates for critical paths
3. **Log volume explosion** - Implement log levels and filtering
4. **Performance impact** - Monitor observability overhead

### Debug Commands
```bash
# Check Prometheus targets
curl http://localhost:9090/api/v1/targets

# Query specific metrics
curl 'http://localhost:9090/api/v1/query?query=kafka_consumer_lag_max'

# Search traces by service
curl 'http://localhost:16686/api/traces?service=order-service&limit=10'

# Check Elasticsearch indices
curl http://localhost:9200/_cat/indices?v
```

## 🚀 Next Steps
Congratulations! You've completed the comprehensive Kafka Mastery Curriculum! You now have the skills to:

✅ **Design** event-driven architectures from scratch  
✅ **Build** production-ready Kafka applications  
✅ **Scale** systems to handle millions of events  
✅ **Secure** clusters with enterprise-grade security  
✅ **Monitor** and operate Kafka in production  
✅ **Deploy** with confidence using modern DevOps practices  

## 🎓 **CURRICULUM COMPLETE!**

You've mastered all 20 lessons of Kafka expertise. Time to build amazing systems! 🚀