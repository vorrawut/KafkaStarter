package com.learning.KafkaStarter.controller

import com.learning.KafkaStarter.model.RawCustomerEvent
import com.learning.KafkaStarter.service.TransformationProducer
import com.learning.KafkaStarter.service.TransformationConsumer
import com.learning.KafkaStarter.service.MessageTransformationService
import com.learning.KafkaStarter.service.MessageFilterService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.time.Instant
import java.util.*
import kotlin.random.Random

@RestController
@RequestMapping("/api/transformation")
class TransformationController {
    
    @Autowired
    private lateinit var transformationProducer: TransformationProducer
    
    @Autowired
    private lateinit var transformationConsumer: TransformationConsumer
    
    @Autowired
    private lateinit var messageTransformationService: MessageTransformationService
    
    @Autowired
    private lateinit var messageFilterService: MessageFilterService
    
    private val logger = org.slf4j.LoggerFactory.getLogger(TransformationController::class.java)
    
    @PostMapping("/events/raw")
    fun sendRawEvent(@RequestBody rawEvent: RawCustomerEvent): ResponseEntity<Map<String, Any>> {
        return try {
            // Send raw customer event for processing
            transformationProducer.sendRawEvent(rawEvent)
            
            logger.info("Raw event sent for transformation: eventId=${rawEvent.eventId}, " +
                "type=${rawEvent.eventType}, customerId=${rawEvent.customerId}")
            
            ResponseEntity.ok(
                mapOf(
                    "success" to true,
                    "message" to "Raw event sent for transformation",
                    "eventId" to rawEvent.eventId,
                    "eventType" to rawEvent.eventType,
                    "customerId" to rawEvent.customerId,
                    "topic" to "raw-customer-events",
                    "timestamp" to System.currentTimeMillis()
                )
            )
        } catch (e: Exception) {
            logger.error("Failed to send raw event: eventId=${rawEvent.eventId}", e)
            
            ResponseEntity.internalServerError().body(
                mapOf(
                    "success" to false,
                    "error" to (e.message ?: "Unknown error"),
                    "eventId" to rawEvent.eventId,
                    "timestamp" to System.currentTimeMillis()
                )
            )
        }
    }
    
    @PostMapping("/events/bulk")
    fun sendBulkEvents(
        @RequestBody rawEvents: List<RawCustomerEvent>
    ): ResponseEntity<Map<String, Any>> {
        return try {
            // Send bulk raw events for processing
            transformationProducer.sendBulkRawEvents(rawEvents)
            
            logger.info("Bulk raw events sent for transformation: count=${rawEvents.size}")
            
            ResponseEntity.ok(
                mapOf(
                    "success" to true,
                    "message" to "Bulk raw events sent for transformation",
                    "count" to rawEvents.size,
                    "eventIds" to rawEvents.map { it.eventId },
                    "eventTypes" to rawEvents.map { it.eventType }.distinct(),
                    "customerIds" to rawEvents.map { it.customerId }.distinct(),
                    "topic" to "raw-customer-events",
                    "timestamp" to System.currentTimeMillis()
                )
            )
        } catch (e: Exception) {
            logger.error("Failed to send bulk raw events: count=${rawEvents.size}", e)
            
            ResponseEntity.internalServerError().body(
                mapOf(
                    "success" to false,
                    "error" to (e.message ?: "Unknown error"),
                    "requestedCount" to rawEvents.size,
                    "timestamp" to System.currentTimeMillis()
                )
            )
        }
    }
    
    @PostMapping("/events/generate")
    fun generateTestEvents(
        @RequestParam(defaultValue = "10") count: Int,
        @RequestParam(defaultValue = "MIXED") eventType: String
    ): ResponseEntity<Map<String, Any>> {
        return try {
            // Generate test events with various characteristics
            val testEvents = generateTestRawEvents(count, eventType)
            
            transformationProducer.sendBulkRawEvents(testEvents)
            
            logger.info("Generated and sent test events: count=$count, type=$eventType")
            
            ResponseEntity.ok(
                mapOf(
                    "success" to true,
                    "message" to "Test events generated and sent",
                    "count" to count,
                    "eventType" to eventType,
                    "generatedEvents" to testEvents.map { 
                        mapOf(
                            "eventId" to it.eventId,
                            "eventType" to it.eventType,
                            "customerId" to it.customerId,
                            "source" to it.source
                        )
                    },
                    "expectedOutcomes" to getExpectedOutcomes(testEvents),
                    "timestamp" to System.currentTimeMillis()
                )
            )
        } catch (e: Exception) {
            logger.error("Failed to generate test events: count=$count, eventType=$eventType", e)
            
            ResponseEntity.internalServerError().body(
                mapOf(
                    "success" to false,
                    "error" to (e.message ?: "Unknown error"),
                    "requestedCount" to count,
                    "requestedEventType" to eventType,
                    "timestamp" to System.currentTimeMillis()
                )
            )
        }
    }
    
    @GetMapping("/stats")
    fun getTransformationStats(): ResponseEntity<Map<String, Any>> {
        return try {
            // Get transformation and filtering statistics
            val processingStats = transformationConsumer.getProcessingStatistics()
            val filterStats = messageFilterService.getFilterStatistics()
            
            ResponseEntity.ok(
                mapOf(
                    "success" to true,
                    "processingStatistics" to processingStats,
                    "filterStatistics" to filterStats,
                    "topicRouting" to mapOf(
                        "enrichedEventsTopic" to "enriched-customer-events",
                        "filteredEventsTopic" to "filtered-events-audit",
                        "segmentTopics" to listOf(
                            "customer-segment-vip-events",
                            "customer-segment-premium-events",
                            "customer-segment-standard-events"
                        ),
                        "eventTypeTopics" to listOf(
                            "event-type-user-registration",
                            "event-type-purchase-completed",
                            "event-type-product-viewed"
                        )
                    ),
                    "timestamp" to System.currentTimeMillis()
                )
            )
        } catch (e: Exception) {
            logger.error("Failed to get transformation stats", e)
            
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
            transformationConsumer.resetStatistics()
            messageFilterService.resetStatistics()
            
            ResponseEntity.ok(
                mapOf(
                    "success" to true,
                    "message" to "Transformation and filter statistics reset",
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
    
    @PostMapping("/events/high-priority")
    fun sendHighPriorityEvent(@RequestBody rawEvent: RawCustomerEvent): ResponseEntity<Map<String, Any>> {
        return try {
            // Send to high priority topic
            transformationProducer.sendRawEvent(rawEvent, "high-priority-events")
            
            logger.info("High priority event sent: eventId=${rawEvent.eventId}")
            
            ResponseEntity.ok(
                mapOf(
                    "success" to true,
                    "message" to "High priority event sent",
                    "eventId" to rawEvent.eventId,
                    "topic" to "high-priority-events",
                    "expeditedProcessing" to true,
                    "timestamp" to System.currentTimeMillis()
                )
            )
        } catch (e: Exception) {
            ResponseEntity.internalServerError().body(
                mapOf(
                    "success" to false,
                    "error" to (e.message ?: "Unknown error"),
                    "eventId" to rawEvent.eventId,
                    "timestamp" to System.currentTimeMillis()
                )
            )
        }
    }
    
    @PostMapping("/events/batch")
    fun sendBatchEvent(@RequestBody rawEvent: RawCustomerEvent): ResponseEntity<Map<String, Any>> {
        return try {
            // Send to batch processing topic
            transformationProducer.sendRawEvent(rawEvent, "batch-events")
            
            logger.info("Batch event sent: eventId=${rawEvent.eventId}")
            
            ResponseEntity.ok(
                mapOf(
                    "success" to true,
                    "message" to "Batch event sent",
                    "eventId" to rawEvent.eventId,
                    "topic" to "batch-events",
                    "batchProcessing" to true,
                    "timestamp" to System.currentTimeMillis()
                )
            )
        } catch (e: Exception) {
            ResponseEntity.internalServerError().body(
                mapOf(
                    "success" to false,
                    "error" to (e.message ?: "Unknown error"),
                    "eventId" to rawEvent.eventId,
                    "timestamp" to System.currentTimeMillis()
                )
            )
        }
    }
    
    private fun generateTestRawEvents(count: Int, eventType: String): List<RawCustomerEvent> {
        val eventTypes = when (eventType) {
            "USER" -> listOf("USER_REGISTRATION", "USER_LOGIN", "USER_LOGOUT")
            "PURCHASE" -> listOf("PURCHASE_COMPLETED", "PURCHASE_CANCELLED", "CART_ABANDONED")
            "PRODUCT" -> listOf("PRODUCT_VIEWED", "PRODUCT_LIKED", "REVIEW_SUBMITTED")
            "MIXED" -> listOf(
                "USER_REGISTRATION", "USER_LOGIN", "PURCHASE_COMPLETED", 
                "PRODUCT_VIEWED", "CART_ABANDONED", "SUPPORT_TICKET_CREATED"
            )
            else -> listOf(eventType)
        }
        
        val sources = listOf("web-app", "mobile-app", "api", "batch-import")
        val customerPrefixes = listOf("", "VIP_", "PREMIUM_", "TEST_", "INTERNAL_")
        
        return (1..count).map { index ->
            val selectedEventType = eventTypes.random()
            val customerId = "${customerPrefixes.random()}customer-${index}-${Random.nextInt(1000, 9999)}"
            
            // Create varied raw data based on event type
            val rawData = when (selectedEventType) {
                "USER_REGISTRATION" -> mapOf(
                    "email" to "${customerId}@example.com",
                    "firstName" to "User$index",
                    "lastName" to "Test$index",
                    "source" to sources.random()
                )
                "PURCHASE_COMPLETED" -> mapOf(
                    "orderId" to "order-${Random.nextInt(10000, 99999)}",
                    "orderValue" to Random.nextDouble(10.0, 1000.0),
                    "productCount" to Random.nextInt(1, 10),
                    "paymentMethod" to listOf("CREDIT_CARD", "PAYPAL", "BANK_TRANSFER").random()
                )
                "PRODUCT_VIEWED" -> mapOf(
                    "productId" to "product-${Random.nextInt(1000, 9999)}",
                    "categoryId" to "category-${Random.nextInt(1, 20)}",
                    "viewDurationSeconds" to Random.nextInt(5, 300)
                )
                else -> mapOf(
                    "action" to selectedEventType,
                    "sessionId" to "session-${Random.nextInt(100000, 999999)}",
                    "value" to Random.nextDouble(1.0, 100.0)
                )
            }
            
            RawCustomerEvent(
                eventId = "test-event-$index-${UUID.randomUUID()}",
                customerId = customerId,
                eventType = selectedEventType,
                rawData = rawData,
                timestamp = System.currentTimeMillis() - Random.nextLong(0, 3600000), // Up to 1 hour ago
                source = sources.random(),
                version = "1.0"
            )
        }
    }
    
    private fun getExpectedOutcomes(events: List<RawCustomerEvent>): Map<String, Any> {
        val shouldPass = events.count { event ->
            messageFilterService.filterByEventType(event.eventType) &&
            messageFilterService.filterByCustomerSegment(event.customerId) &&
            messageFilterService.filterBySource(event.source)
        }
        
        val shouldFilter = events.size - shouldPass
        
        return mapOf(
            "expectedToPass" to shouldPass,
            "expectedToFilter" to shouldFilter,
            "expectedSegments" to events.map { messageTransformationService.calculateCustomerSegment(it.customerId) }.distinct(),
            "expectedTopics" to events.map { 
                transformationProducer.getEventTypeTopicName(it.eventType) 
            }.distinct()
        )
    }
}