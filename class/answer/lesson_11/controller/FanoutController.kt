package com.learning.KafkaStarter.controller

import com.learning.KafkaStarter.model.NotificationTrigger
import com.learning.KafkaStarter.service.NotificationTriggerConsumer
import com.learning.KafkaStarter.service.NotificationFanoutService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.ResponseEntity
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.web.bind.annotation.*
import java.time.Instant
import java.util.*
import kotlin.random.Random

@RestController
@RequestMapping("/api/fanout")
class FanoutController {
    
    @Autowired
    private lateinit var kafkaTemplate: KafkaTemplate<String, NotificationTrigger>
    
    @Autowired
    private lateinit var notificationTriggerConsumer: NotificationTriggerConsumer
    
    @Autowired
    private lateinit var notificationFanoutService: NotificationFanoutService
    
    private val logger = org.slf4j.LoggerFactory.getLogger(FanoutController::class.java)
    
    @PostMapping("/trigger")
    fun triggerNotification(@RequestBody trigger: NotificationTrigger): ResponseEntity<Map<String, Any>> {
        return try {
            // Send notification trigger for fan-out processing
            kafkaTemplate.send("notification-triggers", trigger.userId, trigger)
            
            logger.info("Notification trigger sent: triggerId=${trigger.triggerId}, " +
                "eventType=${trigger.eventType}, userId=${trigger.userId}")
            
            ResponseEntity.ok(
                mapOf(
                    "success" to true,
                    "message" to "Notification trigger sent for fan-out processing",
                    "triggerId" to trigger.triggerId,
                    "eventType" to trigger.eventType,
                    "userId" to trigger.userId,
                    "priority" to trigger.priority,
                    "topic" to "notification-triggers",
                    "timestamp" to System.currentTimeMillis()
                )
            )
        } catch (e: Exception) {
            logger.error("Failed to send notification trigger: triggerId=${trigger.triggerId}", e)
            
            ResponseEntity.internalServerError().body(
                mapOf(
                    "success" to false,
                    "error" to (e.message ?: "Unknown error"),
                    "triggerId" to trigger.triggerId,
                    "timestamp" to System.currentTimeMillis()
                )
            )
        }
    }
    
    @PostMapping("/trigger/urgent")
    fun triggerUrgentNotification(@RequestBody trigger: NotificationTrigger): ResponseEntity<Map<String, Any>> {
        return try {
            // Send urgent notification trigger for expedited processing
            val urgentTrigger = trigger.copy(
                priority = "URGENT",
                metadata = trigger.metadata + mapOf(
                    "expedited" to true,
                    "urgentTimestamp" to System.currentTimeMillis()
                )
            )
            
            kafkaTemplate.send("urgent-notifications", urgentTrigger.userId, urgentTrigger)
            
            logger.info("URGENT notification trigger sent: triggerId=${urgentTrigger.triggerId}, " +
                "eventType=${urgentTrigger.eventType}, userId=${urgentTrigger.userId}")
            
            ResponseEntity.ok(
                mapOf(
                    "success" to true,
                    "message" to "URGENT notification trigger sent for expedited processing",
                    "triggerId" to urgentTrigger.triggerId,
                    "eventType" to urgentTrigger.eventType,
                    "userId" to urgentTrigger.userId,
                    "priority" to "URGENT",
                    "topic" to "urgent-notifications",
                    "expedited" to true,
                    "timestamp" to System.currentTimeMillis()
                )
            )
        } catch (e: Exception) {
            logger.error("Failed to send urgent notification trigger: triggerId=${trigger.triggerId}", e)
            
            ResponseEntity.internalServerError().body(
                mapOf(
                    "success" to false,
                    "error" to (e.message ?: "Unknown error"),
                    "triggerId" to trigger.triggerId,
                    "priority" to "URGENT",
                    "timestamp" to System.currentTimeMillis()
                )
            )
        }
    }
    
    @PostMapping("/bulk-trigger")
    fun triggerBulkNotifications(
        @RequestBody triggers: List<NotificationTrigger>
    ): ResponseEntity<Map<String, Any>> {
        return try {
            // Send bulk notification triggers for batch fan-out processing
            val futures = triggers.map { trigger ->
                kafkaTemplate.send("notification-triggers", trigger.userId, trigger)
            }
            
            logger.info("Bulk notification triggers sent: count=${triggers.size}")
            
            ResponseEntity.ok(
                mapOf(
                    "success" to true,
                    "message" to "Bulk notification triggers sent for fan-out processing",
                    "count" to triggers.size,
                    "triggerIds" to triggers.map { it.triggerId },
                    "eventTypes" to triggers.map { it.eventType }.distinct(),
                    "userIds" to triggers.map { it.userId }.distinct(),
                    "priorities" to triggers.map { it.priority }.distinct(),
                    "topic" to "notification-triggers",
                    "timestamp" to System.currentTimeMillis()
                )
            )
        } catch (e: Exception) {
            logger.error("Failed to send bulk notification triggers: count=${triggers.size}", e)
            
            ResponseEntity.internalServerError().body(
                mapOf(
                    "success" to false,
                    "error" to (e.message ?: "Unknown error"),
                    "requestedCount" to triggers.size,
                    "timestamp" to System.currentTimeMillis()
                )
            )
        }
    }
    
    @GetMapping("/stats")
    fun getFanoutStats(): ResponseEntity<Map<String, Any>> {
        return try {
            // Get fan-out processing statistics
            val processingStats = notificationTriggerConsumer.getProcessingStatistics()
            val fanoutStats = notificationFanoutService.getFanoutStatistics()
            
            ResponseEntity.ok(
                mapOf(
                    "success" to true,
                    "processingStatistics" to processingStats,
                    "fanoutStatistics" to fanoutStats,
                    "topics" to mapOf(
                        "triggers" to "notification-triggers",
                        "urgent" to "urgent-notifications",
                        "email" to "email-notifications",
                        "sms" to "sms-notifications",
                        "push" to "push-notifications"
                    ),
                    "systemHealth" to getSystemHealth(),
                    "timestamp" to System.currentTimeMillis()
                )
            )
        } catch (e: Exception) {
            logger.error("Failed to get fan-out statistics", e)
            
            ResponseEntity.internalServerError().body(
                mapOf(
                    "success" to false,
                    "error" to (e.message ?: "Unknown error"),
                    "timestamp" to System.currentTimeMillis()
                )
            )
        }
    }
    
    @PostMapping("/reset-stats")
    fun resetStatistics(): ResponseEntity<Map<String, Any>> {
        return try {
            notificationTriggerConsumer.resetStatistics()
            notificationFanoutService.resetStatistics()
            
            ResponseEntity.ok(
                mapOf(
                    "success" to true,
                    "message" to "Fan-out statistics reset",
                    "timestamp" to System.currentTimeMillis()
                )
            )
        } catch (e: Exception) {
            ResponseEntity.internalServerError().body(
                mapOf(
                    "success" to false,
                    "error" to (e.message ?: "Unknown error"),
                    "timestamp" to System.currentTimeMillis()
                )
            )
        }
    }
    
    @PostMapping("/generate-test-triggers")
    fun generateTestTriggers(
        @RequestParam(defaultValue = "5") count: Int,
        @RequestParam(defaultValue = "MIXED") eventType: String,
        @RequestParam(defaultValue = "NORMAL") priority: String
    ): ResponseEntity<Map<String, Any>> {
        return try {
            // Generate test notification triggers
            val testTriggers = generateTestNotificationTriggers(count, eventType, priority)
            
            // Send triggers for processing
            testTriggers.forEach { trigger ->
                kafkaTemplate.send("notification-triggers", trigger.userId, trigger)
            }
            
            logger.info("Generated and sent test triggers: count=$count, eventType=$eventType, priority=$priority")
            
            ResponseEntity.ok(
                mapOf(
                    "success" to true,
                    "message" to "Test notification triggers generated and sent",
                    "count" to count,
                    "eventType" to eventType,
                    "priority" to priority,
                    "generatedTriggers" to testTriggers.map {
                        mapOf(
                            "triggerId" to it.triggerId,
                            "eventType" to it.eventType,
                            "userId" to it.userId,
                            "priority" to it.priority
                        )
                    },
                    "expectedChannels" to getExpectedChannels(testTriggers),
                    "timestamp" to System.currentTimeMillis()
                )
            )
        } catch (e: Exception) {
            logger.error("Failed to generate test triggers: count=$count", e)
            
            ResponseEntity.internalServerError().body(
                mapOf(
                    "success" to false,
                    "error" to (e.message ?: "Unknown error"),
                    "requestedCount" to count,
                    "timestamp" to System.currentTimeMillis()
                )
            )
        }
    }
    
    @PostMapping("/simulate-order-flow")
    fun simulateOrderFlow(@RequestParam userId: String): ResponseEntity<Map<String, Any>> {
        return try {
            // Simulate complete order flow with multiple notifications
            val orderId = "order-${Random.nextInt(10000, 99999)}"
            val orderValue = Random.nextDouble(25.0, 500.0)
            
            val orderFlowTriggers = listOf(
                // Order confirmed
                NotificationTrigger(
                    triggerId = "trigger-order-confirmed-${UUID.randomUUID()}",
                    eventType = "ORDER_CONFIRMED",
                    userId = userId,
                    eventData = mapOf(
                        "orderId" to orderId,
                        "orderValue" to orderValue,
                        "estimatedDelivery" to "3-5 business days"
                    ),
                    priority = "NORMAL"
                ),
                
                // Order shipped (after delay simulation)
                NotificationTrigger(
                    triggerId = "trigger-order-shipped-${UUID.randomUUID()}",
                    eventType = "ORDER_SHIPPED",
                    userId = userId,
                    eventData = mapOf(
                        "orderId" to orderId,
                        "trackingNumber" to "TRK${Random.nextInt(100000, 999999)}",
                        "carrier" to "Express Delivery"
                    ),
                    priority = "NORMAL"
                ),
                
                // Order delivered
                NotificationTrigger(
                    triggerId = "trigger-order-delivered-${UUID.randomUUID()}",
                    eventType = "ORDER_DELIVERED",
                    userId = userId,
                    eventData = mapOf(
                        "orderId" to orderId,
                        "deliveryTime" to System.currentTimeMillis()
                    ),
                    priority = "NORMAL"
                )
            )
            
            // Send order flow triggers
            orderFlowTriggers.forEach { trigger ->
                kafkaTemplate.send("notification-triggers", trigger.userId, trigger)
            }
            
            logger.info("Order flow simulation triggered for user: $userId, order: $orderId")
            
            ResponseEntity.ok(
                mapOf(
                    "success" to true,
                    "message" to "Order flow simulation triggered",
                    "userId" to userId,
                    "orderId" to orderId,
                    "orderValue" to orderValue,
                    "triggersGenerated" to orderFlowTriggers.size,
                    "triggers" to orderFlowTriggers.map { 
                        mapOf(
                            "triggerId" to it.triggerId,
                            "eventType" to it.eventType
                        )
                    },
                    "timestamp" to System.currentTimeMillis()
                )
            )
        } catch (e: Exception) {
            logger.error("Failed to simulate order flow for user: $userId", e)
            
            ResponseEntity.internalServerError().body(
                mapOf(
                    "success" to false,
                    "error" to (e.message ?: "Unknown error"),
                    "userId" to userId,
                    "timestamp" to System.currentTimeMillis()
                )
            )
        }
    }
    
    private fun generateTestNotificationTriggers(count: Int, eventType: String, priority: String): List<NotificationTrigger> {
        val eventTypes = when (eventType) {
            "ORDER" -> listOf("ORDER_CONFIRMED", "ORDER_SHIPPED", "ORDER_DELIVERED", "ORDER_CANCELLED")
            "PAYMENT" -> listOf("PAYMENT_RECEIVED", "PAYMENT_FAILED", "PAYMENT_REFUNDED")
            "ACCOUNT" -> listOf("ACCOUNT_CREATED", "ACCOUNT_VERIFIED", "PASSWORD_RESET")
            "SECURITY" -> listOf("SECURITY_ALERT", "SYSTEM_ALERT")
            "MIXED" -> listOf(
                "ORDER_CONFIRMED", "ORDER_SHIPPED", "PAYMENT_RECEIVED", 
                "ACCOUNT_CREATED", "SECURITY_ALERT", "PROMOTION_ALERT"
            )
            else -> listOf(eventType)
        }
        
        val userTypes = listOf("", "VIP_", "PREMIUM_", "customer_", "user_")
        
        return (1..count).map { index ->
            val selectedEventType = eventTypes.random()
            val userId = "${userTypes.random()}user_$index"
            
            val eventData = when (selectedEventType) {
                "ORDER_CONFIRMED", "ORDER_SHIPPED", "ORDER_DELIVERED", "ORDER_CANCELLED" -> mapOf(
                    "orderId" to "order-${Random.nextInt(10000, 99999)}",
                    "orderValue" to Random.nextDouble(10.0, 1000.0)
                )
                "PAYMENT_RECEIVED", "PAYMENT_FAILED", "PAYMENT_REFUNDED" -> mapOf(
                    "paymentId" to "payment-${Random.nextInt(10000, 99999)}",
                    "amount" to Random.nextDouble(10.0, 500.0)
                )
                "SECURITY_ALERT" -> mapOf(
                    "alertType" to listOf("Login from new device", "Password change", "Suspicious activity").random(),
                    "ipAddress" to "192.168.1.${Random.nextInt(1, 254)}"
                )
                else -> mapOf(
                    "testData" to true,
                    "generatedAt" to System.currentTimeMillis()
                )
            }
            
            NotificationTrigger(
                triggerId = "test-trigger-$index-${UUID.randomUUID()}",
                eventType = selectedEventType,
                userId = userId,
                eventData = eventData,
                timestamp = Instant.now().toEpochMilli(),
                priority = priority,
                metadata = mapOf(
                    "testGenerated" to true,
                    "batchIndex" to index
                )
            )
        }
    }
    
    private fun getExpectedChannels(triggers: List<NotificationTrigger>): Map<String, Any> {
        val channelCounts = mutableMapOf<String, Int>()
        
        triggers.forEach { trigger ->
            val channels = notificationFanoutService.getNotificationChannels(trigger.userId, trigger.eventType)
            channels.forEach { channel ->
                channelCounts[channel] = channelCounts.getOrDefault(channel, 0) + 1
            }
        }
        
        return mapOf(
            "expectedChannelDistribution" to channelCounts,
            "totalExpectedNotifications" to channelCounts.values.sum(),
            "uniqueChannels" to channelCounts.keys
        )
    }
    
    private fun getSystemHealth(): Map<String, Any> {
        val processingStats = notificationTriggerConsumer.getProcessingStatistics()
        val fanoutStats = notificationFanoutService.getFanoutStatistics()
        
        val totalProcessed = processingStats["totalTriggersProcessed"] as Long
        val successRate = processingStats["successRate"] as Double
        val totalFanouts = fanoutStats["totalFanouts"] as Long
        
        val health = when {
            successRate >= 95.0 -> "HEALTHY"
            successRate >= 85.0 -> "WARNING"
            else -> "CRITICAL"
        }
        
        return mapOf(
            "overallHealth" to health,
            "totalProcessed" to totalProcessed,
            "successRate" to successRate,
            "totalFanouts" to totalFanouts,
            "lastHealthCheck" to System.currentTimeMillis()
        )
    }
}