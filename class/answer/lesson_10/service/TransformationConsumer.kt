package com.learning.KafkaStarter.service

import com.learning.KafkaStarter.model.RawCustomerEvent
import com.learning.KafkaStarter.model.FilteredEvent
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.kafka.support.KafkaHeaders
import org.springframework.messaging.handler.annotation.Header
import org.springframework.messaging.handler.annotation.Payload
import org.springframework.stereotype.Service
import java.util.concurrent.atomic.AtomicLong

@Service
class TransformationConsumer {
    
    @Autowired
    private lateinit var messageTransformationService: MessageTransformationService
    
    @Autowired
    private lateinit var messageFilterService: MessageFilterService
    
    @Autowired
    private lateinit var transformationProducer: TransformationProducer
    
    private val logger = org.slf4j.LoggerFactory.getLogger(TransformationConsumer::class.java)
    
    // Processing statistics
    private val processedCount = AtomicLong(0)
    private val transformedCount = AtomicLong(0)
    private val filteredCount = AtomicLong(0)
    private val errorCount = AtomicLong(0)
    
    @KafkaListener(topics = ["raw-customer-events"])
    fun processRawCustomerEvent(
        @Payload rawEvent: RawCustomerEvent,
        @Header(KafkaHeaders.RECEIVED_PARTITION_ID) partition: Int,
        @Header(KafkaHeaders.OFFSET) offset: Long
    ) {
        processedCount.incrementAndGet()
        
        try {
            logger.info("Processing raw customer event: eventId=${rawEvent.eventId}, " +
                "type=${rawEvent.eventType}, customerId=${rawEvent.customerId}, " +
                "partition=$partition, offset=$offset")
            
            // Step 1: Filter the event
            val shouldProcess = messageFilterService.shouldProcessEvent(rawEvent)
            
            if (!shouldProcess) {
                // Event was filtered - send to filtered events topic for auditing
                filteredCount.incrementAndGet()
                
                val filterReason = messageFilterService.getFilterReason(rawEvent) ?: "UNKNOWN_FILTER"
                val filteredEvent = FilteredEvent(
                    eventId = rawEvent.eventId,
                    filteredReason = filterReason,
                    originalEvent = rawEvent,
                    filterCriteria = mapOf(
                        "eventType" to rawEvent.eventType,
                        "customerId" to rawEvent.customerId,
                        "source" to rawEvent.source,
                        "timestamp" to rawEvent.timestamp
                    )
                )
                
                transformationProducer.sendFilteredEvent(filteredEvent)
                
                logger.info("Event filtered and sent to audit topic: eventId=${rawEvent.eventId}, " +
                    "reason=$filterReason")
                return
            }
            
            // Step 2: Transform the event
            val enrichedEvent = messageTransformationService.transformCustomerEvent(rawEvent)
            
            if (enrichedEvent != null) {
                transformedCount.incrementAndGet()
                
                // Step 3: Send enriched event to appropriate topics
                transformationProducer.sendEnrichedEvent(enrichedEvent)
                transformationProducer.sendToCustomerSegmentTopic(enrichedEvent)
                transformationProducer.sendToEventTypeTopic(enrichedEvent)
                
                logger.info("Event transformed and routed successfully: " +
                    "eventId=${rawEvent.eventId} -> ${enrichedEvent.eventId}, " +
                    "segment=${enrichedEvent.customerSegment}")
                
            } else {
                errorCount.incrementAndGet()
                logger.error("Failed to transform event: eventId=${rawEvent.eventId}")
                
                // Send to filtered events as transformation failure
                val filteredEvent = FilteredEvent(
                    eventId = rawEvent.eventId,
                    filteredReason = "TRANSFORMATION_FAILED",
                    originalEvent = rawEvent,
                    filterCriteria = mapOf("transformationError" to true)
                )
                
                transformationProducer.sendFilteredEvent(filteredEvent)
            }
            
        } catch (e: Exception) {
            errorCount.incrementAndGet()
            logger.error("Exception processing raw customer event: eventId=${rawEvent.eventId}", e)
            
            // Send to filtered events as processing error
            try {
                val filteredEvent = FilteredEvent(
                    eventId = rawEvent.eventId,
                    filteredReason = "PROCESSING_ERROR: ${e.message}",
                    originalEvent = rawEvent,
                    filterCriteria = mapOf(
                        "errorType" to e::class.simpleName,
                        "errorMessage" to (e.message ?: "Unknown error")
                    )
                )
                
                transformationProducer.sendFilteredEvent(filteredEvent)
            } catch (sendError: Exception) {
                logger.error("Failed to send error event to filtered topic", sendError)
            }
        }
    }
    
    @KafkaListener(topics = ["high-priority-events"])
    fun processHighPriorityEvent(
        @Payload rawEvent: RawCustomerEvent,
        @Header(KafkaHeaders.RECEIVED_TOPIC) topic: String
    ) {
        try {
            logger.info("Processing high priority event: eventId=${rawEvent.eventId}, topic=$topic")
            
            // High priority events bypass normal filtering and get expedited processing
            val enrichedEvent = messageTransformationService.transformCustomerEvent(rawEvent)
            
            if (enrichedEvent != null) {
                // Send to high priority enriched events topic
                transformationProducer.sendEnrichedEvent(enrichedEvent)
                
                // Also send to priority-specific topics
                val priorityTopic = "high-priority-${enrichedEvent.customerSegment.lowercase()}-events"
                transformationProducer.sendToCustomerSegmentTopic(enrichedEvent)
                
                logger.info("High priority event processed: eventId=${rawEvent.eventId}, " +
                    "segment=${enrichedEvent.customerSegment}")
                
            } else {
                logger.error("Failed to transform high priority event: eventId=${rawEvent.eventId}")
            }
            
        } catch (e: Exception) {
            logger.error("Exception processing high priority event: eventId=${rawEvent.eventId}", e)
        }
    }
    
    @KafkaListener(topics = ["batch-events"])
    fun processBatchEvent(
        @Payload rawEvent: RawCustomerEvent
    ) {
        try {
            logger.debug("Processing batch event: eventId=${rawEvent.eventId}")
            
            // Batch events can be processed with relaxed filtering
            val shouldProcess = messageFilterService.filterByEventType(rawEvent.eventType) &&
                              messageFilterService.filterByDataQuality(rawEvent.rawData)
            
            if (shouldProcess) {
                val enrichedEvent = messageTransformationService.transformCustomerEvent(rawEvent)
                
                if (enrichedEvent != null) {
                    // Send to batch-specific topic for bulk processing
                    transformationProducer.sendEnrichedEvent(enrichedEvent)
                    
                    logger.debug("Batch event transformed: eventId=${rawEvent.eventId}")
                } else {
                    logger.warn("Failed to transform batch event: eventId=${rawEvent.eventId}")
                }
            } else {
                logger.debug("Batch event filtered: eventId=${rawEvent.eventId}")
            }
            
        } catch (e: Exception) {
            logger.error("Exception processing batch event: eventId=${rawEvent.eventId}", e)
        }
    }
    
    // Specialized processors for different event types
    @KafkaListener(topics = ["user-events"])
    fun processUserEvent(
        @Payload rawEvent: RawCustomerEvent
    ) {
        try {
            if (rawEvent.eventType.startsWith("USER_")) {
                logger.info("Processing user-specific event: ${rawEvent.eventId}")
                
                // Apply user-specific transformations
                val enrichedEvent = messageTransformationService.transformCustomerEvent(rawEvent)
                
                if (enrichedEvent != null) {
                    // Send to user-specific topics
                    transformationProducer.sendToEventTypeTopic(enrichedEvent)
                    
                    // Additional user event processing
                    if (rawEvent.eventType == "USER_REGISTRATION") {
                        // Send to onboarding pipeline
                        logger.info("Sending new user to onboarding pipeline: ${rawEvent.customerId}")
                    }
                }
            }
            
        } catch (e: Exception) {
            logger.error("Exception processing user event: eventId=${rawEvent.eventId}", e)
        }
    }
    
    @KafkaListener(topics = ["purchase-events"])
    fun processPurchaseEvent(
        @Payload rawEvent: RawCustomerEvent
    ) {
        try {
            if (rawEvent.eventType.startsWith("PURCHASE_")) {
                logger.info("Processing purchase-specific event: ${rawEvent.eventId}")
                
                // Apply purchase-specific transformations
                val enrichedEvent = messageTransformationService.transformCustomerEvent(rawEvent)
                
                if (enrichedEvent != null) {
                    // Send to purchase analytics and recommendation systems
                    transformationProducer.sendToEventTypeTopic(enrichedEvent)
                    
                    // Check for high-value purchases
                    val orderValue = rawEvent.rawData["orderValue"] as? Number
                    if (orderValue != null && orderValue.toDouble() > 1000.0) {
                        logger.info("High-value purchase detected: ${rawEvent.eventId}, " +
                            "value=${orderValue}, customer=${rawEvent.customerId}")
                    }
                }
            }
            
        } catch (e: Exception) {
            logger.error("Exception processing purchase event: eventId=${rawEvent.eventId}", e)
        }
    }
    
    // Statistics methods
    fun getProcessingStatistics(): Map<String, Any> {
        return mapOf(
            "processedCount" to processedCount.get(),
            "transformedCount" to transformedCount.get(),
            "filteredCount" to filteredCount.get(),
            "errorCount" to errorCount.get(),
            "transformationRate" to if (processedCount.get() > 0) {
                (transformedCount.get().toDouble() / processedCount.get()) * 100
            } else 0.0,
            "filterStatistics" to messageFilterService.getFilterStatistics()
        )
    }
    
    fun resetStatistics() {
        processedCount.set(0)
        transformedCount.set(0)
        filteredCount.set(0)
        errorCount.set(0)
        messageFilterService.resetStatistics()
        logger.info("Transformation processing statistics reset")
    }
}