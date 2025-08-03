package com.learning.KafkaStarter.service

import com.learning.KafkaStarter.model.SmsNotification
import com.learning.KafkaStarter.model.NotificationTrigger
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.kafka.support.SendResult
import org.springframework.stereotype.Service
import java.util.*
import java.util.concurrent.CompletableFuture

@Service
class SmsNotificationProducer {
    
    @Autowired
    private lateinit var kafkaTemplate: KafkaTemplate<String, SmsNotification>
    
    private val logger = org.slf4j.LoggerFactory.getLogger(SmsNotificationProducer::class.java)
    
    fun sendSmsNotification(trigger: NotificationTrigger, recipientPhone: String) {
        try {
            // Create SMS notification from trigger
            val smsNotification = createSmsFromTrigger(trigger, recipientPhone)
            
            // Send to SMS notifications topic
            val future: CompletableFuture<SendResult<String, SmsNotification>> = kafkaTemplate.send(
                "sms-notifications",
                smsNotification.recipientPhone, // Use phone as key for partitioning
                smsNotification
            )
            
            future.whenComplete { result, throwable ->
                if (throwable == null) {
                    val metadata = result.recordMetadata
                    logger.debug("SMS notification sent: notificationId=${smsNotification.notificationId}, " +
                        "recipient=${smsNotification.recipientPhone}, " +
                        "partition=${metadata.partition()}, offset=${metadata.offset()}")
                } else {
                    logger.error("Failed to send SMS notification: " +
                        "notificationId=${smsNotification.notificationId}, " +
                        "recipient=${smsNotification.recipientPhone}", throwable)
                }
            }
            
        } catch (e: Exception) {
            logger.error("Exception sending SMS notification for trigger: ${trigger.triggerId}, " +
                "recipient: $recipientPhone", e)
            throw RuntimeException("Failed to send SMS notification", e)
        }
    }
    
    fun createSmsFromTrigger(trigger: NotificationTrigger, recipientPhone: String): SmsNotification {
        // Generate concise SMS message based on event type
        val message = getSmsMessage(trigger.eventType, trigger.eventData)
        
        return SmsNotification(
            notificationId = "sms-${UUID.randomUUID()}",
            triggerId = trigger.triggerId,
            recipientPhone = recipientPhone,
            message = message,
            priority = trigger.priority,
            timestamp = System.currentTimeMillis()
        )
    }
    
    fun getSmsMessage(eventType: String, eventData: Map<String, Any>): String {
        // Generate short, informative messages for different event types
        // SMS messages should be concise due to character limits
        
        return when (eventType) {
            "ORDER_CONFIRMED" -> {
                val orderId = eventData["orderId"] ?: "Unknown"
                "Order #$orderId confirmed! Thank you for your purchase. Delivery in 3-5 days."
            }
            
            "ORDER_SHIPPED" -> {
                val orderId = eventData["orderId"] ?: "Unknown"
                val trackingNumber = eventData["trackingNumber"] ?: "N/A"
                "Order #$orderId shipped! Track: $trackingNumber. Delivery in 2-3 days."
            }
            
            "ORDER_DELIVERED" -> {
                val orderId = eventData["orderId"] ?: "Unknown"
                "Order #$orderId delivered! Enjoy your purchase. Rate your experience?"
            }
            
            "ORDER_CANCELLED" -> {
                val orderId = eventData["orderId"] ?: "Unknown"
                "Order #$orderId cancelled. Refund processed if payment was made. Need help? Contact support."
            }
            
            "PAYMENT_FAILED" -> {
                val paymentId = eventData["paymentId"] ?: "Unknown"
                "Payment #$paymentId failed. Please update payment method or contact support."
            }
            
            "PAYMENT_REFUNDED" -> {
                val amount = eventData["amount"] ?: "Unknown"
                "Refund of $$amount processed. Funds will appear in 3-5 business days."
            }
            
            "SECURITY_ALERT" -> {
                val alertType = eventData["alertType"] ?: "Security Event"
                "SECURITY ALERT: $alertType detected on your account. If not you, change password immediately."
            }
            
            "PASSWORD_RESET" -> {
                "Password reset requested. Check your email for reset link. Expires in 1 hour."
            }
            
            "ACCOUNT_VERIFIED" -> {
                "Account verified successfully! You can now access all features. Welcome aboard!"
            }
            
            "SUPPORT_TICKET_UPDATE" -> {
                val ticketId = eventData["ticketId"] ?: "Unknown"
                val status = eventData["status"] ?: "Updated"
                "Support ticket #$ticketId: $status. Check email for details."
            }
            
            "SYSTEM_ALERT" -> {
                val message = eventData["message"] ?: "System maintenance"
                "SYSTEM ALERT: $message. Check email for more details."
            }
            
            else -> {
                "You have a new notification. Check your email for details."
            }
        }
    }
    
    fun validatePhoneNumber(phoneNumber: String): Boolean {
        // Basic phone number validation
        // In production, use a proper phone number validation library
        
        if (phoneNumber.isBlank()) return false
        
        // Remove common formatting characters
        val cleanPhone = phoneNumber.replace(Regex("[^\\d+]"), "")
        
        // Basic validation patterns
        return when {
            // US/Canada format: +1XXXXXXXXXX
            cleanPhone.matches(Regex("^\\+1\\d{10}$")) -> true
            // International format: +XXXXXXXXXXXX (minimum 10 digits)
            cleanPhone.matches(Regex("^\\+\\d{10,15}$")) -> true
            // Domestic US format: XXXXXXXXXX
            cleanPhone.matches(Regex("^\\d{10}$")) -> true
            else -> false
        }
    }
    
    fun optimizeSmsMessage(message: String, maxLength: Int = 160): String {
        // Optimize SMS message for character limits
        return if (message.length <= maxLength) {
            message
        } else {
            // Truncate and add ellipsis
            message.take(maxLength - 3) + "..."
        }
    }
    
    fun sendBulkSmsNotifications(notifications: List<SmsNotification>) {
        if (notifications.isEmpty()) {
            logger.warn("Empty SMS notifications list provided for bulk send")
            return
        }
        
        try {
            val futures = mutableListOf<CompletableFuture<SendResult<String, SmsNotification>>>()
            
            // Validate phone numbers before sending
            val validNotifications = notifications.filter { notification ->
                if (validatePhoneNumber(notification.recipientPhone)) {
                    true
                } else {
                    logger.warn("Invalid phone number: ${notification.recipientPhone}, " +
                        "skipping SMS ${notification.notificationId}")
                    false
                }
            }
            
            // Send all valid notifications
            validNotifications.forEach { notification ->
                val future = kafkaTemplate.send(
                    "sms-notifications",
                    notification.recipientPhone,
                    notification
                )
                futures.add(future)
            }
            
            // Wait for all sends to complete
            val allFutures = CompletableFuture.allOf(*futures.toTypedArray())
            
            allFutures.whenComplete { _, throwable ->
                if (throwable == null) {
                    logger.info("Bulk SMS notifications sent successfully: " +
                        "requested=${notifications.size}, sent=${validNotifications.size}")
                } else {
                    logger.error("Bulk SMS notifications send failed", throwable)
                }
            }
            
        } catch (e: Exception) {
            logger.error("Exception during bulk SMS notifications send: count=${notifications.size}", e)
            throw RuntimeException("Failed to send bulk SMS notifications", e)
        }
    }
    
    fun estimateSmsCost(notifications: List<SmsNotification>): Map<String, Any> {
        // Estimate SMS costs for bulk notifications
        // In production, integrate with SMS service provider pricing
        
        val baseCostPerSms = 0.05 // $0.05 per SMS
        val internationalMultiplier = 2.0
        
        var domesticCount = 0
        var internationalCount = 0
        
        notifications.forEach { notification ->
            if (notification.recipientPhone.startsWith("+1")) {
                domesticCount++
            } else {
                internationalCount++
            }
        }
        
        val domesticCost = domesticCount * baseCostPerSms
        val internationalCost = internationalCount * baseCostPerSms * internationalMultiplier
        val totalCost = domesticCost + internationalCost
        
        return mapOf(
            "totalNotifications" to notifications.size,
            "domesticCount" to domesticCount,
            "internationalCount" to internationalCount,
            "domesticCost" to domesticCost,
            "internationalCost" to internationalCost,
            "totalEstimatedCost" to totalCost,
            "currency" to "USD"
        )
    }
}