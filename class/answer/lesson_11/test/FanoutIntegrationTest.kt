package com.learning.KafkaStarter.test

import com.learning.KafkaStarter.model.NotificationTrigger
import com.learning.KafkaStarter.service.NotificationTriggerConsumer
import com.learning.KafkaStarter.service.NotificationFanoutService
import com.learning.KafkaStarter.service.EmailNotificationProducer
import com.learning.KafkaStarter.service.SmsNotificationProducer
import com.learning.KafkaStarter.service.PushNotificationProducer
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.kafka.test.context.EmbeddedKafka
import org.springframework.test.annotation.DirtiesContext
import java.time.Instant
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

@SpringBootTest
@DirtiesContext
@EmbeddedKafka(
    partitions = 3,
    topics = [
        "notification-triggers", "urgent-notifications", "bulk-notifications",
        "email-notifications", "sms-notifications", "push-notifications"
    ],
    brokerProperties = [
        "listeners=PLAINTEXT://localhost:9096",
        "port=9096"
    ]
)
class FanoutIntegrationTest {
    
    @Autowired
    private lateinit var kafkaTemplate: KafkaTemplate<String, NotificationTrigger>
    
    @Autowired
    private lateinit var notificationTriggerConsumer: NotificationTriggerConsumer
    
    @Autowired
    private lateinit var notificationFanoutService: NotificationFanoutService
    
    @Autowired
    private lateinit var emailNotificationProducer: EmailNotificationProducer
    
    @Autowired
    private lateinit var smsNotificationProducer: SmsNotificationProducer
    
    @Autowired
    private lateinit var pushNotificationProducer: PushNotificationProducer
    
    @BeforeEach
    fun setup() {
        // Reset statistics before each test
        notificationTriggerConsumer.resetStatistics()
        notificationFanoutService.resetStatistics()
    }
    
    @Test
    fun `should process notification trigger and fan out to multiple channels`() {
        // Create notification trigger
        val trigger = NotificationTrigger(
            triggerId = "test-trigger-1",
            eventType = "ORDER_CONFIRMED",
            userId = "customer-123",
            eventData = mapOf(
                "orderId" to "order-12345",
                "orderValue" to 299.99
            ),
            timestamp = Instant.now().toEpochMilli(),
            priority = "NORMAL"
        )
        
        // Send trigger for processing
        kafkaTemplate.send("notification-triggers", trigger.userId, trigger)
        
        // Wait for processing
        Thread.sleep(2000)
        
        // Verify processing statistics
        val stats = notificationTriggerConsumer.getProcessingStatistics()
        assertTrue(stats["totalTriggersProcessed"] as Long >= 1, "Should have processed at least 1 trigger")
        assertTrue(stats["processedSuccessfully"] as Long >= 1, "Should have processed successfully")
        
        // Verify fan-out statistics
        val fanoutStats = notificationFanoutService.getFanoutStatistics()
        assertTrue(fanoutStats["totalFanouts"] as Long >= 1, "Should have performed fan-out")
    }
    
    @Test
    fun `should handle urgent notifications with expedited processing`() {
        // Create urgent notification trigger
        val urgentTrigger = NotificationTrigger(
            triggerId = "urgent-trigger-1",
            eventType = "SECURITY_ALERT",
            userId = "VIP_customer_456",
            eventData = mapOf(
                "alertType" to "Suspicious login",
                "ipAddress" to "192.168.1.100"
            ),
            timestamp = Instant.now().toEpochMilli(),
            priority = "URGENT"
        )
        
        // Send urgent trigger
        kafkaTemplate.send("urgent-notifications", urgentTrigger.userId, urgentTrigger)
        
        // Wait for processing
        Thread.sleep(2000)
        
        // Verify urgent processing
        val stats = notificationTriggerConsumer.getProcessingStatistics()
        assertTrue(stats["urgentTriggersProcessed"] as Long >= 1, "Should have processed urgent trigger")
    }
    
    @Test
    fun `should determine correct notification channels based on user and event type`() {
        // Test different user types and event types
        val testCases = listOf(
            Triple("VIP_customer_123", "ORDER_CONFIRMED", setOf("EMAIL", "SMS", "PUSH")),
            Triple("PREMIUM_customer_456", "SECURITY_ALERT", setOf("EMAIL", "SMS", "PUSH")),
            Triple("customer_789", "PROMOTION_ALERT", setOf("EMAIL", "PUSH")),
            Triple("user_regular", "PASSWORD_RESET", setOf("EMAIL"))
        )
        
        testCases.forEach { (userId, eventType, expectedChannels) ->
            val channels = notificationFanoutService.getNotificationChannels(userId, eventType)
            assertTrue(channels.isNotEmpty(), "Should have at least one channel for $userId and $eventType")
            
            // Check that expected channels are included based on user type
            when {
                userId.startsWith("VIP_") -> assertTrue(channels.size >= 2, "VIP users should have multiple channels")
                eventType == "SECURITY_ALERT" -> assertTrue(channels.contains("EMAIL"), "Security alerts should include email")
                eventType == "PASSWORD_RESET" -> assertTrue(channels.contains("EMAIL"), "Password reset should include email")
            }
        }
    }
    
    @Test
    fun `should create proper email notifications from triggers`() {
        val trigger = NotificationTrigger(
            triggerId = "email-test-trigger",
            eventType = "ORDER_SHIPPED",
            userId = "customer-email-test",
            eventData = mapOf(
                "orderId" to "order-67890",
                "trackingNumber" to "TRK123456789"
            ),
            priority = "NORMAL"
        )
        
        val email = emailNotificationProducer.createEmailFromTrigger(trigger, "test@example.com")
        
        assertNotNull(email, "Email notification should be created")
        assertEquals(trigger.triggerId, email.triggerId, "Trigger ID should match")
        assertEquals("test@example.com", email.recipientEmail, "Recipient email should match")
        assertTrue(email.subject.contains("Shipped"), "Subject should mention shipping")
        assertTrue(email.body.contains("order-67890"), "Body should contain order ID")
        assertTrue(email.body.contains("TRK123456789"), "Body should contain tracking number")
    }
    
    @Test
    fun `should create proper SMS notifications from triggers`() {
        val trigger = NotificationTrigger(
            triggerId = "sms-test-trigger",
            eventType = "PAYMENT_FAILED",
            userId = "customer-sms-test",
            eventData = mapOf(
                "paymentId" to "payment-12345",
                "failureReason" to "Insufficient funds"
            ),
            priority = "HIGH"
        )
        
        val sms = smsNotificationProducer.createSmsFromTrigger(trigger, "+1-555-123-4567")
        
        assertNotNull(sms, "SMS notification should be created")
        assertEquals(trigger.triggerId, sms.triggerId, "Trigger ID should match")
        assertEquals("+1-555-123-4567", sms.recipientPhone, "Recipient phone should match")
        assertTrue(sms.message.contains("payment"), "Message should mention payment")
        assertTrue(sms.message.length <= 160, "SMS message should be within character limit")
    }
    
    @Test
    fun `should create proper push notifications from triggers`() {
        val trigger = NotificationTrigger(
            triggerId = "push-test-trigger",
            eventType = "ORDER_DELIVERED",
            userId = "customer-push-test",
            eventData = mapOf(
                "orderId" to "order-98765",
                "deliveryTime" to System.currentTimeMillis()
            ),
            priority = "NORMAL"
        )
        
        val push = pushNotificationProducer.createPushFromTrigger(trigger, "device-token-12345")
        
        assertNotNull(push, "Push notification should be created")
        assertEquals(trigger.triggerId, push.triggerId, "Trigger ID should match")
        assertEquals("device-token-12345", push.deviceToken, "Device token should match")
        assertTrue(push.title.contains("Delivered"), "Title should mention delivery")
        assertTrue(push.body.contains("order-98765"), "Body should contain order ID")
        assertTrue(push.data.isNotEmpty(), "Push data should not be empty")
        assertTrue(push.data.containsKey("eventType"), "Push data should contain event type")
    }
    
    @Test
    fun `should validate phone numbers correctly`() {
        val validPhones = listOf(
            "+1-555-123-4567",
            "+15551234567",
            "5551234567",
            "+44-20-1234-5678"
        )
        
        val invalidPhones = listOf(
            "",
            "123",
            "invalid-phone",
            "+1-555-abc-defg"
        )
        
        validPhones.forEach { phone ->
            assertTrue(smsNotificationProducer.validatePhoneNumber(phone), 
                "Phone $phone should be valid")
        }
        
        invalidPhones.forEach { phone ->
            assertTrue(!smsNotificationProducer.validatePhoneNumber(phone), 
                "Phone $phone should be invalid")
        }
    }
    
    @Test
    fun `should validate device tokens correctly`() {
        val validTokens = listOf(
            "device_token_12345",
            "aBcDeFgHiJkLmNoPqRsTuVwXyZ123456",
            "token-with-dashes-and-numbers-123"
        )
        
        val invalidTokens = listOf(
            "",
            "short",
            "token with spaces",
            "token@with#special$characters"
        )
        
        validTokens.forEach { token ->
            assertTrue(pushNotificationProducer.validateDeviceToken(token), 
                "Device token $token should be valid")
        }
        
        invalidTokens.forEach { token ->
            assertTrue(!pushNotificationProducer.validateDeviceToken(token), 
                "Device token $token should be invalid")
        }
    }
    
    @Test
    fun `should handle bulk notification triggers`() {
        // Create bulk triggers
        val bulkTriggers = (1..5).map { index ->
            NotificationTrigger(
                triggerId = "bulk-trigger-$index",
                eventType = "PROMOTION_ALERT",
                userId = "bulk-user-$index",
                eventData = mapOf(
                    "promoCode" to "SAVE20",
                    "discount" to "20%",
                    "batchIndex" to index
                ),
                priority = "NORMAL"
            )
        }
        
        // Send bulk triggers
        bulkTriggers.forEach { trigger ->
            kafkaTemplate.send("notification-triggers", trigger.userId, trigger)
        }
        
        // Wait for processing
        Thread.sleep(4000)
        
        // Verify bulk processing
        val stats = notificationTriggerConsumer.getProcessingStatistics()
        assertTrue(stats["totalTriggersProcessed"] as Long >= 5, "Should process bulk triggers")
        
        val fanoutStats = notificationFanoutService.getFanoutStatistics()
        assertTrue(fanoutStats["totalFanouts"] as Long >= 5, "Should perform bulk fan-outs")
    }
    
    @Test
    fun `should demonstrate complete order flow fan-out`() {
        val userId = "customer-order-flow"
        val orderId = "order-flow-12345"
        
        // Simulate order flow: confirmed -> shipped -> delivered
        val orderFlowTriggers = listOf(
            NotificationTrigger(
                triggerId = "order-confirmed-trigger",
                eventType = "ORDER_CONFIRMED",
                userId = userId,
                eventData = mapOf("orderId" to orderId, "orderValue" to 199.99),
                priority = "NORMAL"
            ),
            NotificationTrigger(
                triggerId = "order-shipped-trigger",
                eventType = "ORDER_SHIPPED",
                userId = userId,
                eventData = mapOf("orderId" to orderId, "trackingNumber" to "TRK987654321"),
                priority = "NORMAL"
            ),
            NotificationTrigger(
                triggerId = "order-delivered-trigger",
                eventType = "ORDER_DELIVERED",
                userId = userId,
                eventData = mapOf("orderId" to orderId, "deliveryTime" to System.currentTimeMillis()),
                priority = "NORMAL"
            )
        )
        
        // Send order flow triggers
        orderFlowTriggers.forEach { trigger ->
            kafkaTemplate.send("notification-triggers", trigger.userId, trigger)
        }
        
        // Wait for processing
        Thread.sleep(5000)
        
        // Verify complete flow processing
        val stats = notificationTriggerConsumer.getProcessingStatistics()
        assertTrue(stats["totalTriggersProcessed"] as Long >= 3, "Should process order flow triggers")
        
        val fanoutStats = notificationFanoutService.getFanoutStatistics()
        assertTrue(fanoutStats["totalFanouts"] as Long >= 3, "Should fan-out order flow notifications")
        
        // Each order event should generate multiple notifications
        val avgChannelsPerFanout = fanoutStats["averageChannelsPerFanout"] as Double
        assertTrue(avgChannelsPerFanout > 1.0, "Should average more than 1 channel per fan-out")
    }
}