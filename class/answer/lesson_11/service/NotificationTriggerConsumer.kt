package com.learning.KafkaStarter.service

import com.learning.KafkaStarter.model.NotificationTrigger
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.kafka.support.KafkaHeaders
import org.springframework.messaging.handler.annotation.Header
import org.springframework.messaging.handler.annotation.Payload
import org.springframework.stereotype.Service
import java.util.concurrent.atomic.AtomicLong

@Service
class NotificationTriggerConsumer {
    
    @Autowired
    private lateinit var notificationFanoutService: NotificationFanoutService
    
    private val logger = org.slf4j.LoggerFactory.getLogger(NotificationTriggerConsumer::class.java)
    
    // Processing statistics
    private val triggersProcessed = AtomicLong(0)
    private val urgentTriggersProcessed = AtomicLong(0)
    private val processedSuccessfully = AtomicLong(0)
    private val processingErrors = AtomicLong(0)
    
    @KafkaListener(topics = ["notification-triggers"])
    fun processNotificationTrigger(
        @Payload trigger: NotificationTrigger,
        @Header(KafkaHeaders.RECEIVED_PARTITION_ID) partition: Int,
        @Header(KafkaHeaders.OFFSET) offset: Long
    ) {
        val startTime = System.currentTimeMillis()
        triggersProcessed.incrementAndGet()
        
        try {
            logger.info("Processing notification trigger: triggerId=${trigger.triggerId}, " +
                "eventType=${trigger.eventType}, userId=${trigger.userId}, " +
                "priority=${trigger.priority}, partition=$partition, offset=$offset")
            
            // Validate trigger before processing
            if (!isValidTrigger(trigger)) {
                logger.warn("Invalid notification trigger: ${trigger.triggerId}")
                processingErrors.incrementAndGet()
                return
            }
            
            // Process notification trigger and fan out to multiple channels
            notificationFanoutService.fanOutNotifications(trigger)
            
            processedSuccessfully.incrementAndGet()
            val processingTime = System.currentTimeMillis() - startTime
            
            logger.info("Notification trigger processed successfully: " +
                "triggerId=${trigger.triggerId}, processingTime=${processingTime}ms")
            
        } catch (e: Exception) {
            processingErrors.incrementAndGet()
            logger.error("Failed to process notification trigger: triggerId=${trigger.triggerId}, " +
                "partition=$partition, offset=$offset", e)
            
            // In production, you might want to:
            // - Send to dead letter topic
            // - Alert monitoring systems
            // - Store failure for retry
        }
    }
    
    @KafkaListener(topics = ["urgent-notifications"])
    fun processUrgentNotification(
        @Payload trigger: NotificationTrigger,
        @Header(KafkaHeaders.RECEIVED_TOPIC) topic: String
    ) {
        val startTime = System.currentTimeMillis()
        urgentTriggersProcessed.incrementAndGet()
        
        try {
            logger.info("Processing URGENT notification trigger: triggerId=${trigger.triggerId}, " +
                "eventType=${trigger.eventType}, userId=${trigger.userId}, topic=$topic")
            
            // Urgent notifications get expedited processing
            if (!isValidTrigger(trigger)) {
                logger.error("Invalid URGENT notification trigger: ${trigger.triggerId}")
                processingErrors.incrementAndGet()
                return
            }
            
            // Process with urgent priority - bypass normal throttling
            val urgentTrigger = trigger.copy(
                priority = "URGENT",
                metadata = trigger.metadata + mapOf(
                    "expedited" to true,
                    "urgentProcessingTime" to System.currentTimeMillis()
                )
            )
            
            notificationFanoutService.fanOutNotifications(urgentTrigger)
            
            processedSuccessfully.incrementAndGet()
            val processingTime = System.currentTimeMillis() - startTime
            
            logger.info("URGENT notification trigger processed: " +
                "triggerId=${trigger.triggerId}, processingTime=${processingTime}ms")
            
        } catch (e: Exception) {
            processingErrors.incrementAndGet()
            logger.error("Failed to process URGENT notification trigger: " +
                "triggerId=${trigger.triggerId}, topic=$topic", e)
            
            // Critical: urgent notifications failures should be escalated immediately
            logger.error("URGENT NOTIFICATION FAILURE - ESCALATE TO OPERATIONS TEAM")
        }
    }
    
    @KafkaListener(topics = ["bulk-notifications"])
    fun processBulkNotificationTrigger(
        @Payload trigger: NotificationTrigger,
        @Header(KafkaHeaders.RECEIVED_TOPIC) topic: String
    ) {
        try {
            logger.debug("Processing bulk notification trigger: triggerId=${trigger.triggerId}")
            
            // Bulk notifications can be processed with relaxed validation
            if (isValidTrigger(trigger)) {
                // Process with batch priority for efficiency
                val batchTrigger = trigger.copy(
                    metadata = trigger.metadata + mapOf(
                        "batchProcessing" to true,
                        "batchProcessingTime" to System.currentTimeMillis()
                    )
                )
                
                notificationFanoutService.fanOutNotifications(batchTrigger)
                processedSuccessfully.incrementAndGet()
            } else {
                logger.debug("Skipping invalid bulk notification trigger: ${trigger.triggerId}")
            }
            
        } catch (e: Exception) {
            logger.warn("Failed to process bulk notification trigger: " +
                "triggerId=${trigger.triggerId}", e)
            // Bulk processing errors are less critical
        }
    }
    
    private fun isValidTrigger(trigger: NotificationTrigger): Boolean {
        // Validate trigger data
        if (trigger.triggerId.isBlank() || trigger.userId.isBlank()) {
            logger.warn("Missing required fields: triggerId or userId")
            return false
        }
        
        // Validate event type
        val validEventTypes = setOf(
            "ORDER_CONFIRMED", "ORDER_SHIPPED", "ORDER_DELIVERED", "ORDER_CANCELLED",
            "PAYMENT_RECEIVED", "PAYMENT_FAILED", "PAYMENT_REFUNDED",
            "ACCOUNT_CREATED", "ACCOUNT_VERIFIED", "PASSWORD_RESET",
            "SECURITY_ALERT", "MAINTENANCE_NOTICE", "PROMOTION_ALERT",
            "SUPPORT_TICKET_UPDATE", "SYSTEM_ALERT"
        )
        
        if (trigger.eventType !in validEventTypes) {
            logger.warn("Invalid event type: ${trigger.eventType}")
            return false
        }
        
        // Validate timestamp (not too old)
        val now = System.currentTimeMillis()
        val maxAge = 24 * 60 * 60 * 1000L // 24 hours
        
        if (trigger.timestamp < (now - maxAge)) {
            logger.warn("Trigger too old: ${trigger.timestamp}, current: $now")
            return false
        }
        
        return true
    }
    
    // Statistics methods
    fun getProcessingStatistics(): Map<String, Any> {
        val total = triggersProcessed.get() + urgentTriggersProcessed.get()
        val successful = processedSuccessfully.get()
        val errors = processingErrors.get()
        
        return mapOf(
            "totalTriggersProcessed" to total,
            "normalTriggersProcessed" to triggersProcessed.get(),
            "urgentTriggersProcessed" to urgentTriggersProcessed.get(),
            "processedSuccessfully" to successful,
            "processingErrors" to errors,
            "successRate" to if (total > 0) (successful.toDouble() / total * 100) else 0.0,
            "errorRate" to if (total > 0) (errors.toDouble() / total * 100) else 0.0
        )
    }
    
    fun resetStatistics() {
        triggersProcessed.set(0)
        urgentTriggersProcessed.set(0)
        processedSuccessfully.set(0)
        processingErrors.set(0)
        logger.info("Notification trigger processing statistics reset")
    }
}