# Lesson 11 Workshop: Fan-out Pattern & Notification Systems

## 🎯 Objective
Master the fan-out messaging pattern for building scalable notification systems that deliver messages across multiple channels (email, SMS, push notifications, webhooks) with reliable delivery and failure handling.

## 📋 Workshop Tasks

### Task 1: Fan-out Event Publisher
Implement fan-out publisher in `fanout/NotificationEventPublisher.kt`

### Task 2: Multi-Channel Notification
Build notification channels in `channels/NotificationChannelManager.kt`

### Task 3: Delivery Tracking
Implement delivery tracking in `tracking/DeliveryTracker.kt`

### Task 4: Channel Routing
Create routing logic in `routing/ChannelRouter.kt`

### Task 5: Failure Recovery
Handle failures in `recovery/FailureRecoveryService.kt`

## 🏗️ Fan-out Notification Architecture
```mermaid
graph TB
    subgraph "Event Sources"
        ORDER[Order Events<br/>order-placed, order-shipped]
        USER[User Events<br/>user-registered, password-reset]
        PAYMENT[Payment Events<br/>payment-successful, payment-failed]
        SYSTEM[System Events<br/>maintenance, alerts]
    end
    
    subgraph "Fan-out Engine"
        DISPATCHER[Event Dispatcher<br/>Routes to channels]
        TEMPLATE[Template Engine<br/>Message formatting]
        PREFERENCES[User Preferences<br/>Channel selection]
        RULES[Business Rules<br/>Event filtering]
    end
    
    subgraph "Notification Channels"
        EMAIL[Email Service<br/>SMTP delivery]
        SMS[SMS Service<br/>Twilio/AWS SNS]
        PUSH[Push Notifications<br/>Firebase/APNs]
        WEBHOOK[Webhooks<br/>HTTP callbacks]
        SLACK[Slack Integration<br/>Team notifications]
        IN_APP[In-App Notifications<br/>Real-time UI]
    end
    
    subgraph "Delivery Tracking"
        TRACKER[Delivery Tracker<br/>Status monitoring]
        RETRY[Retry Service<br/>Failed deliveries]
        ANALYTICS[Analytics<br/>Delivery metrics]
        AUDIT[Audit Log<br/>Compliance tracking]
    end
    
    ORDER --> DISPATCHER
    USER --> DISPATCHER
    PAYMENT --> DISPATCHER
    SYSTEM --> DISPATCHER
    
    DISPATCHER --> TEMPLATE
    DISPATCHER --> PREFERENCES
    DISPATCHER --> RULES
    
    TEMPLATE --> EMAIL
    TEMPLATE --> SMS
    TEMPLATE --> PUSH
    TEMPLATE --> WEBHOOK
    TEMPLATE --> SLACK
    TEMPLATE --> IN_APP
    
    EMAIL --> TRACKER
    SMS --> TRACKER
    PUSH --> TRACKER
    WEBHOOK --> TRACKER
    
    TRACKER --> RETRY
    TRACKER --> ANALYTICS
    TRACKER --> AUDIT
    
    style DISPATCHER fill:#ff6b6b
    style EMAIL fill:#4ecdc4
    style SMS fill:#a8e6cf
    style PUSH fill:#ffe66d
    style TRACKER fill:#ffa8e6
```

## 🌟 Fan-out Pattern Flow
```mermaid
sequenceDiagram
    participant Source as Event Source
    participant Fanout as Fan-out Engine
    participant Email as Email Channel
    participant SMS as SMS Channel
    participant Push as Push Channel
    participant Tracker as Delivery Tracker
    
    Source->>Fanout: Order Shipped Event
    Fanout->>Fanout: Load User Preferences
    Fanout->>Fanout: Apply Business Rules
    Fanout->>Fanout: Generate Templates
    
    par Parallel Delivery
        Fanout->>Email: Email Notification
        Fanout->>SMS: SMS Notification  
        Fanout->>Push: Push Notification
    end
    
    Email->>Tracker: Delivery Success
    SMS->>Tracker: Delivery Failed
    Push->>Tracker: Delivery Success
    
    Tracker->>SMS: Schedule Retry
    SMS->>Tracker: Retry Success
    
    Note over Tracker: All channels delivered successfully
```

## 🎯 Key Concepts

### **Fan-out Pattern Benefits**
- **Parallel Processing**: Multiple channels processed simultaneously
- **Channel Independence**: Failures in one channel don't affect others
- **Scalability**: Easy to add new notification channels
- **User Choice**: Respect user preferences and opt-outs

### **Notification Event Models**

#### **Base Notification Event**
```kotlin
data class NotificationEvent(
    val eventId: String,
    val eventType: NotificationEventType,
    val recipientId: String,
    val templateId: String,
    val payload: Map<String, Any>,
    val priority: NotificationPriority = NotificationPriority.NORMAL,
    val scheduledTime: Instant? = null,
    val expiryTime: Instant? = null,
    val correlationId: String,
    val metadata: Map<String, String> = emptyMap()
)

enum class NotificationEventType {
    ORDER_CONFIRMATION,
    ORDER_SHIPPED,
    ORDER_DELIVERED,
    PAYMENT_SUCCESSFUL,
    PAYMENT_FAILED,
    USER_WELCOME,
    PASSWORD_RESET,
    SECURITY_ALERT,
    MAINTENANCE_NOTICE,
    PROMOTIONAL_OFFER
}

enum class NotificationPriority {
    LOW,
    NORMAL, 
    HIGH,
    URGENT
}
```

#### **Channel-Specific Messages**
```kotlin
data class EmailNotification(
    val recipientEmail: String,
    val subject: String,
    val htmlBody: String,
    val textBody: String,
    val attachments: List<Attachment> = emptyList(),
    val replyTo: String? = null,
    val headers: Map<String, String> = emptyMap()
)

data class SMSNotification(
    val recipientPhone: String,
    val message: String,
    val countryCode: String,
    val shortCode: String? = null
)

data class PushNotification(
    val deviceTokens: List<String>,
    val title: String,
    val body: String,
    val badge: Int? = null,
    val sound: String? = null,
    val data: Map<String, String> = emptyMap()
)

data class WebhookNotification(
    val url: String,
    val method: String = "POST",
    val headers: Map<String, String> = emptyMap(),
    val payload: Any,
    val timeout: Duration = Duration.ofSeconds(30)
)
```

### **User Preference Management**
```mermaid
graph TB
    subgraph "User Preferences"
        GLOBAL[Global Settings<br/>Enable/Disable notifications]
        CHANNEL[Channel Preferences<br/>Email, SMS, Push enabled]
        EVENT[Event Preferences<br/>Which events to receive]
        TIMING[Timing Preferences<br/>Quiet hours, frequency]
    end
    
    subgraph "Preference Rules"
        OPTOUT[Opt-out Lists<br/>Unsubscribed users]
        GDPR[GDPR Compliance<br/>Consent management]
        FREQUENCY[Frequency Limits<br/>Anti-spam protection]
        TIMEZONE[Timezone Awareness<br/>Local delivery times]
    end
    
    subgraph "Dynamic Routing"
        ROUTER[Channel Router]
        FILTER[Preference Filter]
        SCHEDULER[Delivery Scheduler]
    end
    
    GLOBAL --> ROUTER
    CHANNEL --> ROUTER
    EVENT --> FILTER
    TIMING --> SCHEDULER
    
    OPTOUT --> FILTER
    GDPR --> FILTER
    FREQUENCY --> SCHEDULER
    TIMEZONE --> SCHEDULER
    
    style ROUTER fill:#4ecdc4
    style FILTER fill:#ff6b6b
    style SCHEDULER fill:#ffe66d
```

## ⚙️ Fan-out Engine Implementation

### Notification Event Publisher
```kotlin
@Component
class NotificationEventPublisher {
    
    @Autowired
    private lateinit var kafkaTemplate: KafkaTemplate<String, NotificationEvent>
    
    @Autowired
    private lateinit var userPreferenceService: UserPreferenceService
    
    @Autowired
    private lateinit var templateService: TemplateService
    
    fun publishNotification(event: NotificationEvent): CompletableFuture<Void> {
        logger.info("Publishing notification event: ${event.eventId}")
        
        return CompletableFuture.runAsync {
            try {
                // Load user preferences
                val preferences = userPreferenceService.getUserPreferences(event.recipientId)
                
                // Apply business rules and filters
                if (shouldSendNotification(event, preferences)) {
                    
                    // Get enabled channels for this user and event type
                    val enabledChannels = getEnabledChannels(event, preferences)
                    
                    // Fan out to each enabled channel
                    enabledChannels.forEach { channel ->
                        val channelEvent = enrichEventForChannel(event, channel)
                        val topicName = "notifications-${channel.name.lowercase()}"
                        
                        kafkaTemplate.send(topicName, event.recipientId, channelEvent)
                            .whenComplete { result, throwable ->
                                if (throwable != null) {
                                    logger.error("Failed to send to $channel for event ${event.eventId}", throwable)
                                } else {
                                    logger.debug("Successfully sent to $channel for event ${event.eventId}")
                                }
                            }
                    }
                    
                    // Publish to delivery tracking
                    publishDeliveryTracking(event, enabledChannels)
                } else {
                    logger.info("Notification filtered out: ${event.eventId}")
                }
                
            } catch (e: Exception) {
                logger.error("Failed to publish notification: ${event.eventId}", e)
                throw e
            }
        }
    }
    
    private fun shouldSendNotification(
        event: NotificationEvent, 
        preferences: UserPreferences
    ): Boolean {
        // Check global notification settings
        if (!preferences.notificationsEnabled) return false
        
        // Check event type preferences
        if (!preferences.enabledEventTypes.contains(event.eventType)) return false
        
        // Check quiet hours
        if (isInQuietHours(preferences)) return false
        
        // Check frequency limits
        if (exceedsFrequencyLimit(event, preferences)) return false
        
        return true
    }
    
    private fun getEnabledChannels(
        event: NotificationEvent,
        preferences: UserPreferences
    ): List<NotificationChannel> {
        return NotificationChannel.values().filter { channel ->
            preferences.enabledChannels.contains(channel) &&
            isChannelAvailableForEvent(channel, event.eventType)
        }
    }
}
```

### Multi-Channel Delivery System
```kotlin
@Component
class EmailNotificationConsumer {
    
    @KafkaListener(topics = ["notifications-email"])
    fun processEmailNotification(
        @Payload event: NotificationEvent,
        @Header(KafkaHeaders.RECEIVED_PARTITION_ID) partition: Int
    ) {
        try {
            logger.info("Processing email notification: ${event.eventId}")
            
            // Generate email content from template
            val emailContent = templateService.generateEmailContent(event)
            
            // Send email
            val deliveryResult = emailService.sendEmail(
                recipient = emailContent.recipientEmail,
                subject = emailContent.subject,
                htmlBody = emailContent.htmlBody,
                textBody = emailContent.textBody
            )
            
            // Track delivery
            deliveryTracker.recordDelivery(
                eventId = event.eventId,
                channel = NotificationChannel.EMAIL,
                status = if (deliveryResult.success) DeliveryStatus.DELIVERED else DeliveryStatus.FAILED,
                providerId = deliveryResult.messageId,
                error = deliveryResult.error
            )
            
        } catch (e: Exception) {
            logger.error("Failed to process email notification: ${event.eventId}", e)
            
            deliveryTracker.recordDelivery(
                eventId = event.eventId,
                channel = NotificationChannel.EMAIL,
                status = DeliveryStatus.FAILED,
                error = e.message
            )
        }
    }
}

@Component
class SMSNotificationConsumer {
    
    @KafkaListener(topics = ["notifications-sms"])
    fun processSMSNotification(
        @Payload event: NotificationEvent
    ) {
        try {
            logger.info("Processing SMS notification: ${event.eventId}")
            
            // Generate SMS content
            val smsContent = templateService.generateSMSContent(event)
            
            // Send SMS
            val deliveryResult = smsService.sendSMS(
                recipient = smsContent.recipientPhone,
                message = smsContent.message,
                countryCode = smsContent.countryCode
            )
            
            // Track delivery
            deliveryTracker.recordDelivery(
                eventId = event.eventId,
                channel = NotificationChannel.SMS,
                status = if (deliveryResult.success) DeliveryStatus.DELIVERED else DeliveryStatus.FAILED,
                providerId = deliveryResult.messageId,
                error = deliveryResult.error
            )
            
        } catch (e: Exception) {
            logger.error("Failed to process SMS notification: ${event.eventId}", e)
            
            deliveryTracker.recordDelivery(
                eventId = event.eventId,
                channel = NotificationChannel.SMS,
                status = DeliveryStatus.FAILED,
                error = e.message
            )
        }
    }
}
```

## 📊 Delivery Tracking & Analytics

### Delivery Status Monitoring
```mermaid
graph TB
    subgraph "Delivery States"
        QUEUED[Queued<br/>Waiting to send]
        SENT[Sent<br/>Delivered to provider]
        DELIVERED[Delivered<br/>Confirmed by recipient]
        FAILED[Failed<br/>Delivery unsuccessful]
        RETRYING[Retrying<br/>Attempting retry]
        EXPIRED[Expired<br/>TTL exceeded]
    end
    
    subgraph "Tracking Metrics"
        DELIVERY_RATE[Delivery Rate<br/>% successful deliveries]
        CHANNEL_PERF[Channel Performance<br/>Speed and reliability]
        FAILURE_RATE[Failure Rate<br/>% failed deliveries]
        RETRY_SUCCESS[Retry Success Rate<br/>Recovery effectiveness]
    end
    
    subgraph "Business Metrics"
        ENGAGEMENT[User Engagement<br/>Open/click rates]
        CONVERSION[Conversion Rate<br/>Action completion]
        UNSUBSCRIBE[Unsubscribe Rate<br/>User opt-outs]
        REVENUE[Revenue Impact<br/>Notification ROI]
    end
    
    QUEUED --> SENT
    SENT --> DELIVERED
    SENT --> FAILED
    FAILED --> RETRYING
    RETRYING --> DELIVERED
    RETRYING --> EXPIRED
    
    DELIVERED --> DELIVERY_RATE
    FAILED --> FAILURE_RATE
    RETRYING --> RETRY_SUCCESS
    
    DELIVERED --> ENGAGEMENT
    ENGAGEMENT --> CONVERSION
    CONVERSION --> REVENUE
    
    style DELIVERED fill:#4ecdc4
    style FAILED fill:#ff6b6b
    style EXPIRED fill:#ff6b6b
    style ENGAGEMENT fill:#a8e6cf
```

### Delivery Tracker Implementation
```kotlin
@Component
class DeliveryTracker {
    
    @Autowired
    private lateinit var deliveryRepository: DeliveryRepository
    
    @Autowired
    private lateinit var metricsRegistry: MeterRegistry
    
    fun recordDelivery(
        eventId: String,
        channel: NotificationChannel,
        status: DeliveryStatus,
        providerId: String? = null,
        error: String? = null,
        metadata: Map<String, String> = emptyMap()
    ) {
        val delivery = DeliveryRecord(
            id = generateDeliveryId(),
            eventId = eventId,
            channel = channel,
            status = status,
            providerId = providerId,
            error = error,
            attemptCount = 1,
            createdAt = Instant.now(),
            updatedAt = Instant.now(),
            metadata = metadata
        )
        
        deliveryRepository.save(delivery)
        
        // Record metrics
        metricsRegistry.counter(
            "notification.delivery",
            "channel", channel.name,
            "status", status.name
        ).increment()
        
        logger.info("Recorded delivery: $eventId -> $channel -> $status")
    }
    
    fun getDeliveryStatus(eventId: String): List<DeliveryRecord> {
        return deliveryRepository.findByEventId(eventId)
    }
    
    fun getDeliveryMetrics(
        timeRange: TimeRange,
        channels: List<NotificationChannel>? = null
    ): DeliveryMetrics {
        val deliveries = deliveryRepository.findByTimeRange(timeRange, channels)
        
        val totalDeliveries = deliveries.size
        val successfulDeliveries = deliveries.count { it.status == DeliveryStatus.DELIVERED }
        val failedDeliveries = deliveries.count { it.status == DeliveryStatus.FAILED }
        
        val channelMetrics = deliveries.groupBy { it.channel }
            .mapValues { (channel, channelDeliveries) ->
                ChannelMetrics(
                    channel = channel,
                    totalSent = channelDeliveries.size,
                    successful = channelDeliveries.count { it.status == DeliveryStatus.DELIVERED },
                    failed = channelDeliveries.count { it.status == DeliveryStatus.FAILED },
                    averageDeliveryTime = calculateAverageDeliveryTime(channelDeliveries)
                )
            }
        
        return DeliveryMetrics(
            timeRange = timeRange,
            totalDeliveries = totalDeliveries,
            successfulDeliveries = successfulDeliveries,
            failedDeliveries = failedDeliveries,
            overallDeliveryRate = if (totalDeliveries > 0) successfulDeliveries.toDouble() / totalDeliveries else 0.0,
            channelMetrics = channelMetrics.values.toList()
        )
    }
}
```

## ✅ Success Criteria
- [ ] Fan-out pattern correctly distributes events to multiple channels
- [ ] User preferences properly filter notifications
- [ ] All notification channels (email, SMS, push) working correctly
- [ ] Delivery tracking provides visibility into success/failure rates
- [ ] Failure recovery handles retries and dead letter scenarios
- [ ] Performance handles high-volume notification bursts (>10k/min)
- [ ] Business rules prevent spam and respect user preferences

## 🚀 Getting Started

### 1. Configure Notification Topics
```bash
# Create notification topics for each channel
kafka-topics --create --topic notifications-email --partitions 6 --replication-factor 1 --bootstrap-server localhost:9092
kafka-topics --create --topic notifications-sms --partitions 3 --replication-factor 1 --bootstrap-server localhost:9092  
kafka-topics --create --topic notifications-push --partitions 6 --replication-factor 1 --bootstrap-server localhost:9092
kafka-topics --create --topic notifications-webhook --partitions 3 --replication-factor 1 --bootstrap-server localhost:9092
kafka-topics --create --topic notification-delivery-tracking --partitions 3 --replication-factor 1 --bootstrap-server localhost:9092
```

### 2. Test Fan-out Notification
```bash
# Send order confirmation event
curl -X POST http://localhost:8090/api/notifications/send \
  -H "Content-Type: application/json" \
  -d '{
    "eventType": "ORDER_CONFIRMATION",
    "recipientId": "user-123",
    "templateId": "order-confirmation-v1",
    "payload": {
      "orderId": "ORD-456",
      "amount": "99.99",
      "items": ["Kafka T-Shirt", "Spring Boot Mug"]
    },
    "priority": "HIGH"
  }'
```

### 3. Monitor Delivery Metrics
```bash
# Check delivery status
curl http://localhost:8090/api/notifications/delivery/user-123

# View channel metrics
curl http://localhost:8090/api/notifications/metrics/channels

# Monitor notification topics
kafka-console-consumer --topic notifications-email --from-beginning --bootstrap-server localhost:9092
```

## 🎯 Best Practices

### Fan-out Design
- **Use separate topics** for each notification channel
- **Implement user preferences** to respect opt-outs
- **Apply rate limiting** to prevent spam
- **Use templates** for consistent messaging

### Channel Management
- **Handle channel failures** independently
- **Implement fallback channels** for critical notifications
- **Monitor delivery rates** and optimize underperforming channels
- **Respect channel-specific limits** (SMS character limits, etc.)

### Performance Optimization
- **Batch similar notifications** when possible
- **Use channel-specific partitioning** for parallel processing
- **Implement caching** for user preferences and templates
- **Monitor and alert** on delivery delays

## 🔍 Troubleshooting

### Common Issues
1. **High delivery failures** - Check channel service health and configuration
2. **Notification delays** - Monitor Kafka consumer lag and processing time
3. **User complaints about spam** - Review frequency limits and preferences
4. **Template rendering errors** - Validate template syntax and data

### Debug Commands
```bash
# Check notification consumer lag
kafka-consumer-groups --bootstrap-server localhost:9092 --group notification-email-group --describe

# Monitor delivery tracking
kafka-console-consumer --topic notification-delivery-tracking --from-beginning --bootstrap-server localhost:9092

# Check template rendering
curl http://localhost:8090/api/templates/render/order-confirmation-v1 -d '{"orderId":"123"}'
```

## 🚀 Next Steps
Fan-out notifications mastered? Time to integrate with REST APIs! Move to [Lesson 12: Hybrid REST + Kafka Architecture](../lesson_12/README.md) to learn seamless integration patterns.