package com.learning.KafkaStarter.service

import com.learning.KafkaStarter.model.EmailNotification
import com.learning.KafkaStarter.model.NotificationTrigger
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.kafka.support.SendResult
import org.springframework.stereotype.Service
import java.util.*
import java.util.concurrent.CompletableFuture

@Service
class EmailNotificationProducer {
    
    @Autowired
    private lateinit var kafkaTemplate: KafkaTemplate<String, EmailNotification>
    
    private val logger = org.slf4j.LoggerFactory.getLogger(EmailNotificationProducer::class.java)
    
    fun sendEmailNotification(trigger: NotificationTrigger, recipientEmail: String) {
        try {
            // Create email notification from trigger
            val emailNotification = createEmailFromTrigger(trigger, recipientEmail)
            
            // Send to email notifications topic
            val future: CompletableFuture<SendResult<String, EmailNotification>> = kafkaTemplate.send(
                "email-notifications",
                emailNotification.recipientEmail, // Use email as key for partitioning
                emailNotification
            )
            
            future.whenComplete { result, throwable ->
                if (throwable == null) {
                    val metadata = result.recordMetadata
                    logger.debug("Email notification sent: notificationId=${emailNotification.notificationId}, " +
                        "recipient=${emailNotification.recipientEmail}, " +
                        "partition=${metadata.partition()}, offset=${metadata.offset()}")
                } else {
                    logger.error("Failed to send email notification: " +
                        "notificationId=${emailNotification.notificationId}, " +
                        "recipient=${emailNotification.recipientEmail}", throwable)
                }
            }
            
        } catch (e: Exception) {
            logger.error("Exception sending email notification for trigger: ${trigger.triggerId}, " +
                "recipient: $recipientEmail", e)
            throw RuntimeException("Failed to send email notification", e)
        }
    }
    
    fun createEmailFromTrigger(trigger: NotificationTrigger, recipientEmail: String): EmailNotification {
        // Generate subject and body based on event type
        val (subject, body) = getEmailContent(trigger.eventType, trigger.eventData)
        val templateName = getEmailTemplate(trigger.eventType)
        
        return EmailNotification(
            notificationId = "email-${UUID.randomUUID()}",
            triggerId = trigger.triggerId,
            recipientEmail = recipientEmail,
            subject = subject,
            body = body,
            templateName = templateName,
            priority = trigger.priority,
            timestamp = System.currentTimeMillis()
        )
    }
    
    fun getEmailTemplate(eventType: String): String {
        // Return appropriate template name for different event types
        return when (eventType) {
            "ORDER_CONFIRMED" -> "order-confirmation"
            "ORDER_SHIPPED" -> "order-shipped"
            "ORDER_DELIVERED" -> "order-delivered"
            "ORDER_CANCELLED" -> "order-cancelled"
            "PAYMENT_RECEIVED" -> "payment-confirmation"
            "PAYMENT_FAILED" -> "payment-failed"
            "PAYMENT_REFUNDED" -> "payment-refund"
            "ACCOUNT_CREATED" -> "welcome-email"
            "ACCOUNT_VERIFIED" -> "account-verified"
            "PASSWORD_RESET" -> "password-reset"
            "SECURITY_ALERT" -> "security-alert"
            "MAINTENANCE_NOTICE" -> "maintenance-notice"
            "PROMOTION_ALERT" -> "promotion-email"
            "SUPPORT_TICKET_UPDATE" -> "support-update"
            "SYSTEM_ALERT" -> "system-alert"
            else -> "generic-notification"
        }
    }
    
    private fun getEmailContent(eventType: String, eventData: Map<String, Any>): Pair<String, String> {
        // Generate subject and body for different event types
        return when (eventType) {
            "ORDER_CONFIRMED" -> {
                val orderId = eventData["orderId"] ?: "Unknown"
                val orderValue = eventData["orderValue"] ?: "Unknown"
                Pair(
                    "Order Confirmation - Order #$orderId",
                    """
                    Dear Valued Customer,
                    
                    Thank you for your order! Your order #$orderId has been confirmed.
                    
                    Order Details:
                    - Order ID: $orderId
                    - Order Value: $$orderValue
                    - Estimated Delivery: 3-5 business days
                    
                    You will receive a shipping notification once your order is dispatched.
                    
                    Thank you for shopping with us!
                    """.trimIndent()
                )
            }
            
            "ORDER_SHIPPED" -> {
                val orderId = eventData["orderId"] ?: "Unknown"
                val trackingNumber = eventData["trackingNumber"] ?: "N/A"
                Pair(
                    "Your Order Has Shipped - Order #$orderId",
                    """
                    Great news! Your order #$orderId has been shipped.
                    
                    Tracking Information:
                    - Order ID: $orderId
                    - Tracking Number: $trackingNumber
                    - Estimated Delivery: 2-3 business days
                    
                    You can track your package using the tracking number above.
                    """.trimIndent()
                )
            }
            
            "ORDER_DELIVERED" -> {
                val orderId = eventData["orderId"] ?: "Unknown"
                Pair(
                    "Order Delivered - Order #$orderId",
                    """
                    Your order #$orderId has been delivered successfully!
                    
                    We hope you enjoy your purchase. If you have any issues with your order, 
                    please don't hesitate to contact our customer support.
                    
                    Thank you for choosing us!
                    """.trimIndent()
                )
            }
            
            "PAYMENT_RECEIVED" -> {
                val paymentId = eventData["paymentId"] ?: "Unknown"
                val amount = eventData["amount"] ?: "Unknown"
                Pair(
                    "Payment Confirmation - Payment #$paymentId",
                    """
                    Your payment has been processed successfully.
                    
                    Payment Details:
                    - Payment ID: $paymentId
                    - Amount: $$amount
                    - Status: Confirmed
                    
                    Thank you for your payment!
                    """.trimIndent()
                )
            }
            
            "PAYMENT_FAILED" -> {
                val paymentId = eventData["paymentId"] ?: "Unknown"
                val reason = eventData["failureReason"] ?: "Unknown reason"
                Pair(
                    "Payment Failed - Action Required",
                    """
                    We were unable to process your payment.
                    
                    Payment Details:
                    - Payment ID: $paymentId
                    - Failure Reason: $reason
                    
                    Please update your payment method or contact support for assistance.
                    """.trimIndent()
                )
            }
            
            "ACCOUNT_CREATED" -> {
                val username = eventData["username"] ?: "User"
                Pair(
                    "Welcome to Our Platform!",
                    """
                    Welcome $username!
                    
                    Your account has been created successfully. You can now:
                    - Browse our products
                    - Make purchases
                    - Track your orders
                    - Access customer support
                    
                    Thank you for joining us!
                    """.trimIndent()
                )
            }
            
            "SECURITY_ALERT" -> {
                val alertType = eventData["alertType"] ?: "Security Event"
                val ipAddress = eventData["ipAddress"] ?: "Unknown"
                Pair(
                    "Security Alert - $alertType",
                    """
                    We detected a security event on your account.
                    
                    Alert Details:
                    - Type: $alertType
                    - IP Address: $ipAddress
                    - Time: ${java.time.Instant.now()}
                    
                    If this was not you, please change your password immediately and contact support.
                    """.trimIndent()
                )
            }
            
            "PASSWORD_RESET" -> {
                val resetToken = eventData["resetToken"] ?: "N/A"
                Pair(
                    "Password Reset Request",
                    """
                    You requested a password reset for your account.
                    
                    Use the following link to reset your password:
                    https://example.com/reset-password?token=$resetToken
                    
                    This link will expire in 1 hour.
                    
                    If you didn't request this reset, please ignore this email.
                    """.trimIndent()
                )
            }
            
            "PROMOTION_ALERT" -> {
                val promoCode = eventData["promoCode"] ?: "SAVE20"
                val discount = eventData["discount"] ?: "20%"
                Pair(
                    "Special Offer Just for You!",
                    """
                    Don't miss out on this exclusive offer!
                    
                    Get $discount off your next purchase with code: $promoCode
                    
                    Offer valid until the end of this month.
                    
                    Shop now and save!
                    """.trimIndent()
                )
            }
            
            "SYSTEM_ALERT" -> {
                val alertMessage = eventData["message"] ?: "System maintenance"
                Pair(
                    "System Alert - Important Notice",
                    """
                    Important system notification:
                    
                    $alertMessage
                    
                    We apologize for any inconvenience and appreciate your patience.
                    """.trimIndent()
                )
            }
            
            else -> {
                Pair(
                    "Notification from Our Platform",
                    """
                    You have received a new notification.
                    
                    Event Type: $eventType
                    Data: $eventData
                    
                    Thank you for using our platform!
                    """.trimIndent()
                )
            }
        }
    }
    
    fun sendBulkEmailNotifications(notifications: List<EmailNotification>) {
        if (notifications.isEmpty()) {
            logger.warn("Empty email notifications list provided for bulk send")
            return
        }
        
        try {
            val futures = mutableListOf<CompletableFuture<SendResult<String, EmailNotification>>>()
            
            // Send all notifications and collect futures
            notifications.forEach { notification ->
                val future = kafkaTemplate.send(
                    "email-notifications", 
                    notification.recipientEmail, 
                    notification
                )
                futures.add(future)
            }
            
            // Wait for all sends to complete
            val allFutures = CompletableFuture.allOf(*futures.toTypedArray())
            
            allFutures.whenComplete { _, throwable ->
                if (throwable == null) {
                    logger.info("Bulk email notifications sent successfully: count=${notifications.size}")
                } else {
                    logger.error("Bulk email notifications send failed", throwable)
                }
            }
            
        } catch (e: Exception) {
            logger.error("Exception during bulk email notifications send: count=${notifications.size}", e)
            throw RuntimeException("Failed to send bulk email notifications", e)
        }
    }
}