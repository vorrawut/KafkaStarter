package com.learning.KafkaStarter.service

import com.learning.KafkaStarter.model.PushNotification
import com.learning.KafkaStarter.model.NotificationTrigger
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.kafka.support.SendResult
import org.springframework.stereotype.Service
import java.util.*
import java.util.concurrent.CompletableFuture

@Service
class PushNotificationProducer {
    
    @Autowired
    private lateinit var kafkaTemplate: KafkaTemplate<String, PushNotification>
    
    private val logger = org.slf4j.LoggerFactory.getLogger(PushNotificationProducer::class.java)
    
    fun sendPushNotification(trigger: NotificationTrigger, deviceToken: String) {
        try {
            // Create push notification from trigger
            val pushNotification = createPushFromTrigger(trigger, deviceToken)
            
            // Send to push notifications topic
            val future: CompletableFuture<SendResult<String, PushNotification>> = kafkaTemplate.send(
                "push-notifications",
                pushNotification.deviceToken, // Use device token as key for partitioning
                pushNotification
            )
            
            future.whenComplete { result, throwable ->
                if (throwable == null) {
                    val metadata = result.recordMetadata
                    logger.debug("Push notification sent: notificationId=${pushNotification.notificationId}, " +
                        "deviceToken=${pushNotification.deviceToken.take(10)}..., " +
                        "partition=${metadata.partition()}, offset=${metadata.offset()}")
                } else {
                    logger.error("Failed to send push notification: " +
                        "notificationId=${pushNotification.notificationId}, " +
                        "deviceToken=${pushNotification.deviceToken.take(10)}...", throwable)
                }
            }
            
        } catch (e: Exception) {
            logger.error("Exception sending push notification for trigger: ${trigger.triggerId}, " +
                "deviceToken: ${deviceToken.take(10)}...", e)
            throw RuntimeException("Failed to send push notification", e)
        }
    }
    
    fun createPushFromTrigger(trigger: NotificationTrigger, deviceToken: String): PushNotification {
        // Generate title, body, and data payload based on event type
        val (title, body) = getPushContent(trigger.eventType, trigger.eventData)
        val data = getPushData(trigger.eventType, trigger.eventData)
        
        return PushNotification(
            notificationId = "push-${UUID.randomUUID()}",
            triggerId = trigger.triggerId,
            deviceToken = deviceToken,
            title = title,
            body = body,
            data = data,
            priority = trigger.priority,
            timestamp = System.currentTimeMillis()
        )
    }
    
    fun getPushContent(eventType: String, eventData: Map<String, Any>): Pair<String, String> {
        // Generate push notification title and body for different event types
        
        return when (eventType) {
            "ORDER_CONFIRMED" -> {
                val orderId = eventData["orderId"] ?: "Unknown"
                Pair(
                    "Order Confirmed! 🎉",
                    "Your order #$orderId has been confirmed and is being prepared."
                )
            }
            
            "ORDER_SHIPPED" -> {
                val orderId = eventData["orderId"] ?: "Unknown"
                Pair(
                    "Order Shipped! 📦",
                    "Your order #$orderId is on its way! Track your package for updates."
                )
            }
            
            "ORDER_DELIVERED" -> {
                val orderId = eventData["orderId"] ?: "Unknown"
                Pair(
                    "Order Delivered! ✅",
                    "Your order #$orderId has been delivered. Enjoy your purchase!"
                )
            }
            
            "ORDER_CANCELLED" -> {
                val orderId = eventData["orderId"] ?: "Unknown"
                Pair(
                    "Order Cancelled",
                    "Order #$orderId has been cancelled. Any payment will be refunded."
                )
            }
            
            "PAYMENT_RECEIVED" -> {
                val amount = eventData["amount"] ?: "Unknown"
                Pair(
                    "Payment Confirmed ✅",
                    "Your payment of $$amount has been processed successfully."
                )
            }
            
            "PAYMENT_FAILED" -> {
                Pair(
                    "Payment Failed ❌",
                    "We couldn't process your payment. Please update your payment method."
                )
            }
            
            "PAYMENT_REFUNDED" -> {
                val amount = eventData["amount"] ?: "Unknown"
                Pair(
                    "Refund Processed 💰",
                    "Your refund of $$amount has been processed and will appear soon."
                )
            }
            
            "ACCOUNT_CREATED" -> {
                Pair(
                    "Welcome! 🎉",
                    "Your account has been created successfully. Start exploring!"
                )
            }
            
            "ACCOUNT_VERIFIED" -> {
                Pair(
                    "Account Verified ✅",
                    "Your account is now verified. You have access to all features!"
                )
            }
            
            "SECURITY_ALERT" -> {
                val alertType = eventData["alertType"] ?: "Security Event"
                Pair(
                    "Security Alert ⚠️",
                    "$alertType detected. Secure your account immediately if this wasn't you."
                )
            }
            
            "PROMOTION_ALERT" -> {
                val discount = eventData["discount"] ?: "Special"
                Pair(
                    "Special Offer! 🏷️",
                    "$discount discount available! Don't miss out on this limited-time offer."
                )
            }
            
            "SUPPORT_TICKET_UPDATE" -> {
                val ticketId = eventData["ticketId"] ?: "Unknown"
                val status = eventData["status"] ?: "Updated"
                Pair(
                    "Support Update 💬",
                    "Your support ticket #$ticketId has been $status."
                )
            }
            
            "SYSTEM_ALERT" -> {
                Pair(
                    "System Notice 📢",
                    "Important system notification. Tap for details."
                )
            }
            
            else -> {
                Pair(
                    "New Notification 🔔",
                    "You have a new notification. Tap to view details."
                )
            }
        }
    }
    
    private fun getPushData(eventType: String, eventData: Map<String, Any>): Map<String, Any> {
        // Generate additional data payload for push notifications
        // This data can be used by mobile apps for deep linking and actions
        
        val baseData = mutableMapOf<String, Any>(
            "eventType" to eventType,
            "timestamp" to System.currentTimeMillis(),
            "source" to "kafka-notification-system"
        )
        
        // Add event-specific data
        when (eventType) {
            "ORDER_CONFIRMED", "ORDER_SHIPPED", "ORDER_DELIVERED", "ORDER_CANCELLED" -> {
                eventData["orderId"]?.let { baseData["orderId"] = it }
                baseData["deepLink"] = "app://orders/${eventData["orderId"]}"
                baseData["actionButton"] = "View Order"
            }
            
            "PAYMENT_RECEIVED", "PAYMENT_FAILED", "PAYMENT_REFUNDED" -> {
                eventData["paymentId"]?.let { baseData["paymentId"] = it }
                baseData["deepLink"] = "app://payments/${eventData["paymentId"]}"
                baseData["actionButton"] = "View Payment"
            }
            
            "ACCOUNT_CREATED", "ACCOUNT_VERIFIED" -> {
                baseData["deepLink"] = "app://profile"
                baseData["actionButton"] = "View Profile"
            }
            
            "SECURITY_ALERT" -> {
                baseData["deepLink"] = "app://security"
                baseData["actionButton"] = "Review Security"
                baseData["priority"] = "high"
            }
            
            "PROMOTION_ALERT" -> {
                eventData["promoCode"]?.let { baseData["promoCode"] = it }
                baseData["deepLink"] = "app://promotions"
                baseData["actionButton"] = "Shop Now"
            }
            
            "SUPPORT_TICKET_UPDATE" -> {
                eventData["ticketId"]?.let { baseData["ticketId"] = it }
                baseData["deepLink"] = "app://support/${eventData["ticketId"]}"
                baseData["actionButton"] = "View Ticket"
            }
            
            "SYSTEM_ALERT" -> {
                baseData["deepLink"] = "app://notifications"
                baseData["actionButton"] = "View Details"
            }
        }
        
        return baseData
    }
    
    fun validateDeviceToken(deviceToken: String): Boolean {
        // Basic device token validation
        // In production, validate according to push service requirements (FCM, APNS, etc.)
        
        return when {
            deviceToken.isBlank() -> false
            deviceToken.length < 10 -> false // Too short
            deviceToken.length > 500 -> false // Too long
            deviceToken.contains(Regex("[^a-zA-Z0-9_:-]")) -> false // Invalid characters
            else -> true
        }
    }
    
    fun sendBulkPushNotifications(notifications: List<PushNotification>) {
        if (notifications.isEmpty()) {
            logger.warn("Empty push notifications list provided for bulk send")
            return
        }
        
        try {
            val futures = mutableListOf<CompletableFuture<SendResult<String, PushNotification>>>()
            
            // Validate device tokens before sending
            val validNotifications = notifications.filter { notification ->
                if (validateDeviceToken(notification.deviceToken)) {
                    true
                } else {
                    logger.warn("Invalid device token: ${notification.deviceToken.take(10)}..., " +
                        "skipping push notification ${notification.notificationId}")
                    false
                }
            }
            
            // Send all valid notifications
            validNotifications.forEach { notification ->
                val future = kafkaTemplate.send(
                    "push-notifications",
                    notification.deviceToken,
                    notification
                )
                futures.add(future)
            }
            
            // Wait for all sends to complete
            val allFutures = CompletableFuture.allOf(*futures.toTypedArray())
            
            allFutures.whenComplete { _, throwable ->
                if (throwable == null) {
                    logger.info("Bulk push notifications sent successfully: " +
                        "requested=${notifications.size}, sent=${validNotifications.size}")
                } else {
                    logger.error("Bulk push notifications send failed", throwable)
                }
            }
            
        } catch (e: Exception) {
            logger.error("Exception during bulk push notifications send: count=${notifications.size}", e)
            throw RuntimeException("Failed to send bulk push notifications", e)
        }
    }
    
    fun sendHighPriorityPush(trigger: NotificationTrigger, deviceToken: String) {
        // Send high priority push notification with special handling
        try {
            val pushNotification = createPushFromTrigger(trigger, deviceToken).copy(
                priority = "HIGH",
                data = createPushFromTrigger(trigger, deviceToken).data + mapOf(
                    "highPriority" to true,
                    "sound" to "urgent_notification",
                    "vibration" to true,
                    "badge" to 1
                )
            )
            
            // Send to high priority push notifications topic
            val future = kafkaTemplate.send(
                "high-priority-push-notifications",
                pushNotification.deviceToken,
                pushNotification
            )
            
            future.whenComplete { result, throwable ->
                if (throwable == null) {
                    logger.info("High priority push notification sent: " +
                        "notificationId=${pushNotification.notificationId}")
                } else {
                    logger.error("Failed to send high priority push notification: " +
                        "notificationId=${pushNotification.notificationId}", throwable)
                }
            }
            
        } catch (e: Exception) {
            logger.error("Exception sending high priority push notification for trigger: ${trigger.triggerId}", e)
        }
    }
}