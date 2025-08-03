package com.learning.KafkaStarter.service

import com.learning.KafkaStarter.model.NotificationTrigger
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.util.concurrent.atomic.AtomicLong

@Service
class NotificationFanoutService {
    
    @Autowired
    private lateinit var emailNotificationProducer: EmailNotificationProducer
    
    @Autowired
    private lateinit var smsNotificationProducer: SmsNotificationProducer
    
    @Autowired
    private lateinit var pushNotificationProducer: PushNotificationProducer
    
    private val logger = org.slf4j.LoggerFactory.getLogger(NotificationFanoutService::class.java)
    
    // Fan-out statistics
    private val fanoutCount = AtomicLong(0)
    private val emailsSent = AtomicLong(0)
    private val smsSent = AtomicLong(0)
    private val pushSent = AtomicLong(0)
    private val fanoutErrors = AtomicLong(0)
    
    fun fanOutNotifications(trigger: NotificationTrigger) {
        fanoutCount.incrementAndGet()
        
        try {
            logger.info("Fanning out notifications for trigger: ${trigger.triggerId}, " +
                "eventType=${trigger.eventType}, userId=${trigger.userId}, priority=${trigger.priority}")
            
            // Determine which notification channels to use
            val channels = getNotificationChannels(trigger.userId, trigger.eventType)
            
            logger.debug("Selected channels for user ${trigger.userId}: $channels")
            
            // Get user contact information
            val userContacts = getUserContactInfo(trigger.userId)
            
            // Fan out to each enabled channel
            channels.forEach { channel ->
                when (channel) {
                    "EMAIL" -> {
                        if (shouldSendNotification(trigger, channel) && userContacts.containsKey("email")) {
                            try {
                                emailNotificationProducer.sendEmailNotification(
                                    trigger, 
                                    userContacts["email"] as String
                                )
                                emailsSent.incrementAndGet()
                                logger.debug("Email notification sent for trigger: ${trigger.triggerId}")
                            } catch (e: Exception) {
                                logger.error("Failed to send email for trigger: ${trigger.triggerId}", e)
                            }
                        }
                    }
                    
                    "SMS" -> {
                        if (shouldSendNotification(trigger, channel) && userContacts.containsKey("phone")) {
                            try {
                                smsNotificationProducer.sendSmsNotification(
                                    trigger,
                                    userContacts["phone"] as String
                                )
                                smsSent.incrementAndGet()
                                logger.debug("SMS notification sent for trigger: ${trigger.triggerId}")
                            } catch (e: Exception) {
                                logger.error("Failed to send SMS for trigger: ${trigger.triggerId}", e)
                            }
                        }
                    }
                    
                    "PUSH" -> {
                        if (shouldSendNotification(trigger, channel) && userContacts.containsKey("deviceToken")) {
                            try {
                                pushNotificationProducer.sendPushNotification(
                                    trigger,
                                    userContacts["deviceToken"] as String
                                )
                                pushSent.incrementAndGet()
                                logger.debug("Push notification sent for trigger: ${trigger.triggerId}")
                            } catch (e: Exception) {
                                logger.error("Failed to send push notification for trigger: ${trigger.triggerId}", e)
                            }
                        }
                    }
                    
                    else -> {
                        logger.warn("Unknown notification channel: $channel")
                    }
                }
            }
            
            logger.info("Fan-out completed for trigger: ${trigger.triggerId}, channels: $channels")
            
        } catch (e: Exception) {
            fanoutErrors.incrementAndGet()
            logger.error("Fan-out failed for trigger: ${trigger.triggerId}", e)
        }
    }
    
    fun getNotificationChannels(userId: String, eventType: String): Set<String> {
        // Determine which notification channels to use based on user preferences and event type
        
        // Get user preferences (simulated)
        val userPreferences = getUserPreferences(userId)
        
        // Get event-specific channel requirements
        val eventChannels = getEventTypeChannels(eventType)
        
        // Combine preferences with event requirements
        val enabledChannels = mutableSetOf<String>()
        
        // Email channel logic
        if (userPreferences.getOrDefault("emailEnabled", true) as Boolean) {
            if (eventChannels.contains("EMAIL") || isEmailRequiredEvent(eventType)) {
                enabledChannels.add("EMAIL")
            }
        }
        
        // SMS channel logic
        if (userPreferences.getOrDefault("smsEnabled", false) as Boolean) {
            if (eventChannels.contains("SMS") || isSmsRequiredEvent(eventType)) {
                enabledChannels.add("SMS")
            }
        }
        
        // Push notification logic
        if (userPreferences.getOrDefault("pushEnabled", true) as Boolean) {
            if (eventChannels.contains("PUSH") || isPushRequiredEvent(eventType)) {
                enabledChannels.add("PUSH")
            }
        }
        
        // For urgent events, ensure at least one channel is enabled
        if (isUrgentEvent(eventType) && enabledChannels.isEmpty()) {
            enabledChannels.add("EMAIL") // Default to email for urgent events
        }
        
        return enabledChannels
    }
    
    fun shouldSendNotification(trigger: NotificationTrigger, channel: String): Boolean {
        // Apply business rules to determine if notification should be sent
        
        // Check priority-based rules
        when (trigger.priority) {
            "URGENT" -> {
                // Always send urgent notifications
                return true
            }
            "HIGH" -> {
                // Send high priority to all enabled channels
                return true
            }
            "NORMAL" -> {
                // Apply normal business rules
                return when (channel) {
                    "EMAIL" -> true // Always send emails for normal priority
                    "SMS" -> isEventWorthySms(trigger.eventType)
                    "PUSH" -> isEventWorthyPush(trigger.eventType)
                    else -> false
                }
            }
            "LOW" -> {
                // Limited channels for low priority
                return channel == "EMAIL" && !isQuietHour()
            }
            else -> return false
        }
    }
    
    private fun getUserPreferences(userId: String): Map<String, Any> {
        // Simulate user preferences lookup
        // In a real system, this would query a database or service
        
        return when {
            userId.contains("VIP") -> mapOf(
                "emailEnabled" to true,
                "smsEnabled" to true,
                "pushEnabled" to true,
                "quietHours" to false
            )
            userId.contains("PREMIUM") -> mapOf(
                "emailEnabled" to true,
                "smsEnabled" to true,
                "pushEnabled" to true,
                "quietHours" to true
            )
            userId.endsWith("_NOSMS") -> mapOf(
                "emailEnabled" to true,
                "smsEnabled" to false,
                "pushEnabled" to true
            )
            else -> mapOf(
                "emailEnabled" to true,
                "smsEnabled" to false,
                "pushEnabled" to true,
                "quietHours" to true
            )
        }
    }
    
    private fun getUserContactInfo(userId: String): Map<String, Any> {
        // Simulate user contact information lookup
        return mapOf(
            "email" to "${userId}@example.com",
            "phone" to "+1-555-${userId.hashCode().toString().takeLast(7)}",
            "deviceToken" to "device_token_${userId}_${System.currentTimeMillis()}"
        )
    }
    
    private fun getEventTypeChannels(eventType: String): Set<String> {
        // Default channels for different event types
        return when (eventType) {
            "ORDER_CONFIRMED", "ORDER_SHIPPED", "ORDER_DELIVERED" -> setOf("EMAIL", "PUSH")
            "ORDER_CANCELLED", "PAYMENT_FAILED" -> setOf("EMAIL", "SMS", "PUSH")
            "PAYMENT_RECEIVED", "PAYMENT_REFUNDED" -> setOf("EMAIL", "PUSH")
            "SECURITY_ALERT", "SYSTEM_ALERT" -> setOf("EMAIL", "SMS", "PUSH")
            "ACCOUNT_CREATED", "ACCOUNT_VERIFIED" -> setOf("EMAIL")
            "PASSWORD_RESET" -> setOf("EMAIL", "SMS")
            "MAINTENANCE_NOTICE" -> setOf("EMAIL")
            "PROMOTION_ALERT" -> setOf("EMAIL", "PUSH")
            else -> setOf("EMAIL")
        }
    }
    
    private fun isEmailRequiredEvent(eventType: String): Boolean {
        // Events that always require email notification
        return eventType in setOf(
            "ACCOUNT_CREATED", "PASSWORD_RESET", "SECURITY_ALERT",
            "PAYMENT_RECEIVED", "ORDER_CONFIRMED"
        )
    }
    
    private fun isSmsRequiredEvent(eventType: String): Boolean {
        // Events that require SMS notification
        return eventType in setOf(
            "SECURITY_ALERT", "PAYMENT_FAILED", "ORDER_CANCELLED"
        )
    }
    
    private fun isPushRequiredEvent(eventType: String): Boolean {
        // Events that require push notification
        return eventType in setOf(
            "ORDER_SHIPPED", "ORDER_DELIVERED", "SYSTEM_ALERT"
        )
    }
    
    private fun isUrgentEvent(eventType: String): Boolean {
        return eventType in setOf(
            "SECURITY_ALERT", "SYSTEM_ALERT", "PAYMENT_FAILED"
        )
    }
    
    private fun isEventWorthySms(eventType: String): Boolean {
        return eventType in setOf(
            "ORDER_SHIPPED", "ORDER_DELIVERED", "PAYMENT_FAILED",
            "SECURITY_ALERT", "PASSWORD_RESET"
        )
    }
    
    private fun isEventWorthyPush(eventType: String): Boolean {
        return eventType in setOf(
            "ORDER_CONFIRMED", "ORDER_SHIPPED", "ORDER_DELIVERED", 
            "PAYMENT_RECEIVED", "SECURITY_ALERT", "PROMOTION_ALERT"
        )
    }
    
    private fun isQuietHour(): Boolean {
        // Check if it's during quiet hours (simplified)
        val hour = java.time.LocalDateTime.now().hour
        return hour in 22..6 // 10 PM to 6 AM
    }
    
    // Statistics methods
    fun getFanoutStatistics(): Map<String, Any> {
        return mapOf(
            "totalFanouts" to fanoutCount.get(),
            "emailsSent" to emailsSent.get(),
            "smsSent" to smsSent.get(),
            "pushSent" to pushSent.get(),
            "fanoutErrors" to fanoutErrors.get(),
            "averageChannelsPerFanout" to if (fanoutCount.get() > 0) {
                (emailsSent.get() + smsSent.get() + pushSent.get()).toDouble() / fanoutCount.get()
            } else 0.0
        )
    }
    
    fun resetStatistics() {
        fanoutCount.set(0)
        emailsSent.set(0)
        smsSent.set(0)
        pushSent.set(0)
        fanoutErrors.set(0)
        logger.info("Fan-out statistics reset")
    }
}