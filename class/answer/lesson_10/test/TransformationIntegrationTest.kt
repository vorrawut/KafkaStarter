package com.learning.KafkaStarter.test

import com.learning.KafkaStarter.model.RawCustomerEvent
import com.learning.KafkaStarter.service.TransformationProducer
import com.learning.KafkaStarter.service.TransformationConsumer
import com.learning.KafkaStarter.service.MessageTransformationService
import com.learning.KafkaStarter.service.MessageFilterService
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.kafka.test.context.EmbeddedKafka
import org.springframework.test.annotation.DirtiesContext
import java.time.Instant
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

@SpringBootTest
@DirtiesContext
@EmbeddedKafka(
    partitions = 3,
    topics = [
        "raw-customer-events", "enriched-customer-events", "filtered-events-audit",
        "customer-segment-vip-events", "customer-segment-premium-events",
        "event-type-user-registration", "event-type-purchase-completed"
    ],
    brokerProperties = [
        "listeners=PLAINTEXT://localhost:9095",
        "port=9095"
    ]
)
class TransformationIntegrationTest {
    
    @Autowired
    private lateinit var transformationProducer: TransformationProducer
    
    @Autowired
    private lateinit var transformationConsumer: TransformationConsumer
    
    @Autowired
    private lateinit var messageTransformationService: MessageTransformationService
    
    @Autowired
    private lateinit var messageFilterService: MessageFilterService
    
    @BeforeEach
    fun setup() {
        // Reset statistics before each test
        transformationConsumer.resetStatistics()
        messageFilterService.resetStatistics()
    }
    
    @Test
    fun `should transform valid customer events`() {
        // Create valid raw customer event
        val rawEvent = RawCustomerEvent(
            eventId = "test-event-1",
            customerId = "customer-valid-123",
            eventType = "USER_REGISTRATION",
            rawData = mapOf(
                "email" to "test@example.com",
                "firstName" to "John",
                "lastName" to "Doe",
                "registrationSource" to "web-app"
            ),
            timestamp = Instant.now().toEpochMilli(),
            source = "web-app",
            version = "1.0"
        )
        
        // Test transformation service directly
        val enrichedEvent = messageTransformationService.transformCustomerEvent(rawEvent)
        
        assertNotNull(enrichedEvent, "Event should be transformed successfully")
        assertEquals(rawEvent.eventId, enrichedEvent.eventId)
        assertEquals(rawEvent.customerId, enrichedEvent.customerId)
        assertEquals(rawEvent.eventType, enrichedEvent.eventType)
        assertTrue(enrichedEvent.enrichedData.isNotEmpty(), "Should have enriched data")
        assertNotNull(enrichedEvent.customerSegment, "Should have customer segment")
    }
    
    @Test
    fun `should filter invalid events`() {
        // Create events that should be filtered
        val invalidEvents = listOf(
            // Invalid event type
            RawCustomerEvent(
                eventId = "invalid-type",
                customerId = "customer-123",
                eventType = "INVALID_EVENT_TYPE",
                rawData = mapOf("data" to "value"),
                source = "web-app"
            ),
            
            // Test customer (should be filtered)
            RawCustomerEvent(
                eventId = "test-customer",
                customerId = "TEST_customer_123",
                eventType = "USER_REGISTRATION",
                rawData = mapOf("data" to "value"),
                source = "web-app"
            ),
            
            // Invalid source
            RawCustomerEvent(
                eventId = "invalid-source",
                customerId = "customer-123",
                eventType = "USER_REGISTRATION",
                rawData = mapOf("data" to "value"),
                source = "untrusted-source"
            ),
            
            // Empty raw data
            RawCustomerEvent(
                eventId = "empty-data",
                customerId = "customer-123",
                eventType = "USER_REGISTRATION",
                rawData = emptyMap(),
                source = "web-app"
            )
        )
        
        // Test filter service
        invalidEvents.forEach { event ->
            val shouldProcess = messageFilterService.shouldProcessEvent(event)
            assertFalse(shouldProcess, "Event ${event.eventId} should be filtered")
            
            val filterReason = messageFilterService.getFilterReason(event)
            assertNotNull(filterReason, "Should have filter reason for ${event.eventId}")
        }
    }
    
    @Test
    fun `should calculate correct customer segments`() {
        val testCases = mapOf(
            "VIP_customer_123" to "VIP",
            "PREMIUM_customer_456" to "PREMIUM",
            "customer_CORP" to "CORPORATE",
            "customer_GOLD_789" to "GOLD",
            "customer_SILVER_123" to "SILVER",
            "very_long_customer_id_enterprise_level" to "ENTERPRISE",
            "customer_12345" to "HIGH_VALUE",
            "regular_customer" to "STANDARD"
        )
        
        testCases.forEach { (customerId, expectedSegment) ->
            val actualSegment = messageTransformationService.calculateCustomerSegment(customerId)
            assertEquals(expectedSegment, actualSegment, 
                "Customer ID $customerId should map to segment $expectedSegment")
        }
    }
    
    @Test
    fun `should validate event data correctly`() {
        // Valid event
        val validEvent = RawCustomerEvent(
            eventId = "valid-event",
            customerId = "customer-123",
            eventType = "USER_REGISTRATION",
            rawData = mapOf("data" to "value"),
            timestamp = Instant.now().toEpochMilli(),
            source = "web-app"
        )
        
        assertTrue(messageTransformationService.validateEventData(validEvent), 
            "Valid event should pass validation")
        
        // Invalid events
        val invalidEvents = listOf(
            // Missing event ID
            validEvent.copy(eventId = ""),
            
            // Missing customer ID
            validEvent.copy(customerId = ""),
            
            // Invalid event type
            validEvent.copy(eventType = "INVALID_TYPE"),
            
            // Old timestamp
            validEvent.copy(timestamp = System.currentTimeMillis() - 25 * 60 * 60 * 1000L),
            
            // Future timestamp
            validEvent.copy(timestamp = System.currentTimeMillis() + 10 * 60 * 1000L),
            
            // Invalid source
            validEvent.copy(source = "invalid-source"),
            
            // Empty raw data
            validEvent.copy(rawData = emptyMap())
        )
        
        invalidEvents.forEach { event ->
            assertFalse(messageTransformationService.validateEventData(event),
                "Invalid event should fail validation: ${event}")
        }
    }
    
    @Test
    fun `should process events end-to-end`() {
        // Create test events
        val testEvents = listOf(
            RawCustomerEvent(
                eventId = "e2e-test-1",
                customerId = "customer-e2e-1",
                eventType = "USER_REGISTRATION",
                rawData = mapOf(
                    "email" to "user1@example.com",
                    "firstName" to "User",
                    "lastName" to "One"
                ),
                source = "web-app"
            ),
            RawCustomerEvent(
                eventId = "e2e-test-2",
                customerId = "VIP_customer_e2e_2",
                eventType = "PURCHASE_COMPLETED",
                rawData = mapOf(
                    "orderId" to "order-12345",
                    "orderValue" to 999.99,
                    "productCount" to 3
                ),
                source = "mobile-app"
            ),
            RawCustomerEvent(
                eventId = "e2e-filtered",
                customerId = "TEST_customer_filtered",
                eventType = "USER_REGISTRATION",
                rawData = mapOf("data" to "test"),
                source = "web-app"
            )
        )
        
        // Send events for processing
        transformationProducer.sendBulkRawEvents(testEvents)
        
        // Wait for processing
        Thread.sleep(3000)
        
        // Check processing statistics
        val stats = transformationConsumer.getProcessingStatistics()
        
        assertTrue(stats["processedCount"] as Long >= testEvents.size.toLong(),
            "Should have processed at least ${testEvents.size} events")
        
        // Should have some transformed and some filtered
        assertTrue(stats["transformedCount"] as Long > 0, "Should have transformed some events")
        assertTrue(stats["filteredCount"] as Long > 0, "Should have filtered some events")
        
        // Check filter statistics
        val filterStats = messageFilterService.getFilterStatistics()
        assertTrue(filterStats["totalEventsProcessed"] as Long > 0, "Should have processed events")
    }
    
    @Test
    fun `should normalize event data correctly`() {
        val rawData = mapOf(
            "firstName" to "John",
            "lastName" to "Doe",
            "orderValue" to "123.45",
            "userEmail" to "JOHN.DOE@EXAMPLE.COM",
            "createdDate" to "1640995200000"
        )
        
        val normalizedData = messageTransformationService.normalizeEventData(rawData)
        
        // Check field name normalization
        assertTrue(normalizedData.containsKey("first_name"), "Should normalize firstName to first_name")
        assertTrue(normalizedData.containsKey("last_name"), "Should normalize lastName to last_name")
        assertTrue(normalizedData.containsKey("order_value"), "Should normalize orderValue to order_value")
        assertTrue(normalizedData.containsKey("user_email"), "Should normalize userEmail to user_email")
        
        // Check data type normalization
        assertTrue(normalizedData["order_value"] is Double, "Should convert string amount to Double")
        assertEquals(123.45, normalizedData["order_value"], "Should preserve numeric value")
        
        // Check email normalization
        assertEquals("john.doe@example.com", normalizedData["user_email"], "Should lowercase email")
        
        // Check timestamp normalization
        assertTrue(normalizedData["created_date"] is Long, "Should convert string timestamp to Long")
    }
    
    @Test
    fun `should handle bulk transformation correctly`() {
        val bulkEvents = (1..10).map { index ->
            RawCustomerEvent(
                eventId = "bulk-$index",
                customerId = "customer-bulk-$index",
                eventType = if (index % 2 == 0) "USER_REGISTRATION" else "PURCHASE_COMPLETED",
                rawData = mapOf(
                    "index" to index,
                    "batchTest" to true,
                    "value" to index * 10.0
                ),
                source = if (index % 3 == 0) "mobile-app" else "web-app"
            )
        }
        
        // Send bulk events
        transformationProducer.sendBulkRawEvents(bulkEvents)
        
        // Wait for processing
        Thread.sleep(4000)
        
        // Check that bulk processing worked
        val stats = transformationConsumer.getProcessingStatistics()
        assertTrue(stats["processedCount"] as Long >= 10, "Should process bulk events")
        
        // Most should be transformed (valid events)
        val transformationRate = stats["transformationRate"] as Double
        assertTrue(transformationRate > 80.0, "Should have high transformation rate for valid bulk events")
    }
}