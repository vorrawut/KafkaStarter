package com.learning.KafkaStarter.service

import com.learning.KafkaStarter.model.EnrichedCustomerEvent
import com.learning.KafkaStarter.model.FilteredEvent
import com.learning.KafkaStarter.model.RawCustomerEvent
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.kafka.support.SendResult
import org.springframework.stereotype.Service
import java.util.concurrent.CompletableFuture

@Service
class TransformationProducer {
    
    @Autowired
    private lateinit var kafkaTemplate: KafkaTemplate<String, Any>
    
    private val logger = org.slf4j.LoggerFactory.getLogger(TransformationProducer::class.java)
    
    fun sendEnrichedEvent(enrichedEvent: EnrichedCustomerEvent) {
        try {
            // Send enriched event to main enriched events topic
            val future: CompletableFuture<SendResult<String, Any>> = kafkaTemplate.send(
                "enriched-customer-events",
                enrichedEvent.customerId, // Use customer ID as key for partitioning
                enrichedEvent
            )
            
            future.whenComplete { result, throwable ->
                if (throwable == null) {
                    val metadata = result.recordMetadata
                    logger.debug("Enriched event sent: eventId=${enrichedEvent.eventId}, " +
                        "partition=${metadata.partition()}, offset=${metadata.offset()}")
                } else {
                    logger.error("Failed to send enriched event: eventId=${enrichedEvent.eventId}", throwable)
                }
            }
            
        } catch (e: Exception) {
            logger.error("Exception sending enriched event: eventId=${enrichedEvent.eventId}", e)
            throw RuntimeException("Failed to send enriched event", e)
        }
    }
    
    fun sendFilteredEvent(filteredEvent: FilteredEvent) {
        try {
            // Send filtered event to audit topic for monitoring and analysis
            val future = kafkaTemplate.send(
                "filtered-events-audit",
                filteredEvent.eventId, // Use event ID as key
                filteredEvent
            )
            
            future.whenComplete { result, throwable ->
                if (throwable == null) {
                    val metadata = result.recordMetadata
                    logger.debug("Filtered event sent to audit: eventId=${filteredEvent.eventId}, " +
                        "reason=${filteredEvent.filteredReason}, partition=${metadata.partition()}")
                } else {
                    logger.error("Failed to send filtered event: eventId=${filteredEvent.eventId}", throwable)
                }
            }
            
        } catch (e: Exception) {
            logger.error("Exception sending filtered event: eventId=${filteredEvent.eventId}", e)
        }
    }
    
    fun sendToCustomerSegmentTopic(enrichedEvent: EnrichedCustomerEvent) {
        try {
            // Route to customer segment-specific topic
            val segmentTopic = "customer-segment-${enrichedEvent.customerSegment.lowercase()}-events"
            
            val future = kafkaTemplate.send(
                segmentTopic,
                enrichedEvent.customerId,
                enrichedEvent
            )
            
            future.whenComplete { result, throwable ->
                if (throwable == null) {
                    val metadata = result.recordMetadata
                    logger.debug("Event sent to segment topic: eventId=${enrichedEvent.eventId}, " +
                        "segment=${enrichedEvent.customerSegment}, topic=$segmentTopic, " +
                        "partition=${metadata.partition()}")
                } else {
                    logger.error("Failed to send to segment topic: eventId=${enrichedEvent.eventId}, " +
                        "segment=${enrichedEvent.customerSegment}, topic=$segmentTopic", throwable)
                }
            }
            
        } catch (e: Exception) {
            logger.error("Exception sending to segment topic: eventId=${enrichedEvent.eventId}, " +
                "segment=${enrichedEvent.customerSegment}", e)
        }
    }
    
    fun sendToEventTypeTopic(enrichedEvent: EnrichedCustomerEvent) {
        try {
            // Route to event type-specific topic
            val eventTypeTopic = "event-type-${enrichedEvent.eventType.lowercase().replace("_", "-")}"
            
            val future = kafkaTemplate.send(
                eventTypeTopic,
                enrichedEvent.eventId,
                enrichedEvent
            )
            
            future.whenComplete { result, throwable ->
                if (throwable == null) {
                    val metadata = result.recordMetadata
                    logger.debug("Event sent to type topic: eventId=${enrichedEvent.eventId}, " +
                        "type=${enrichedEvent.eventType}, topic=$eventTypeTopic, " +
                        "partition=${metadata.partition()}")
                } else {
                    logger.error("Failed to send to type topic: eventId=${enrichedEvent.eventId}, " +
                        "type=${enrichedEvent.eventType}, topic=$eventTypeTopic", throwable)
                }
            }
            
        } catch (e: Exception) {
            logger.error("Exception sending to type topic: eventId=${enrichedEvent.eventId}, " +
                "type=${enrichedEvent.eventType}", e)
        }
    }
    
    fun sendRawEvent(rawEvent: RawCustomerEvent, topicName: String = "raw-customer-events") {
        try {
            // Send raw event to specified topic
            val future = kafkaTemplate.send(
                topicName,
                rawEvent.customerId,
                rawEvent
            )
            
            future.whenComplete { result, throwable ->
                if (throwable == null) {
                    val metadata = result.recordMetadata
                    logger.debug("Raw event sent: eventId=${rawEvent.eventId}, " +
                        "topic=$topicName, partition=${metadata.partition()}, offset=${metadata.offset()}")
                } else {
                    logger.error("Failed to send raw event: eventId=${rawEvent.eventId}, " +
                        "topic=$topicName", throwable)
                }
            }
            
        } catch (e: Exception) {
            logger.error("Exception sending raw event: eventId=${rawEvent.eventId}, " +
                "topic=$topicName", e)
            throw RuntimeException("Failed to send raw event", e)
        }
    }
    
    fun sendBulkRawEvents(rawEvents: List<RawCustomerEvent>, topicName: String = "raw-customer-events") {
        if (rawEvents.isEmpty()) {
            logger.warn("Empty raw events list provided for bulk send")
            return
        }
        
        try {
            val futures = mutableListOf<CompletableFuture<SendResult<String, Any>>>()
            
            // Send all events and collect futures
            rawEvents.forEach { rawEvent ->
                val future = kafkaTemplate.send(topicName, rawEvent.customerId, rawEvent)
                futures.add(future)
            }
            
            // Wait for all sends to complete
            val allFutures = CompletableFuture.allOf(*futures.toTypedArray())
            
            allFutures.whenComplete { _, throwable ->
                if (throwable == null) {
                    logger.info("Bulk raw events sent successfully: count=${rawEvents.size}, topic=$topicName")
                } else {
                    logger.error("Bulk raw events send failed: topic=$topicName", throwable)
                }
            }
            
        } catch (e: Exception) {
            logger.error("Exception during bulk raw events send: count=${rawEvents.size}, " +
                "topic=$topicName", e)
            throw RuntimeException("Failed to send bulk raw events", e)
        }
    }
    
    fun sendToMultipleTopics(enrichedEvent: EnrichedCustomerEvent, additionalTopics: List<String>) {
        try {
            // Send enriched event to multiple additional topics
            val futures = mutableListOf<CompletableFuture<SendResult<String, Any>>>()
            
            additionalTopics.forEach { topic ->
                val future = kafkaTemplate.send(topic, enrichedEvent.customerId, enrichedEvent)
                futures.add(future)
            }
            
            // Wait for all sends to complete
            val allFutures = CompletableFuture.allOf(*futures.toTypedArray())
            
            allFutures.whenComplete { _, throwable ->
                if (throwable == null) {
                    logger.info("Event sent to multiple topics: eventId=${enrichedEvent.eventId}, " +
                        "topics=$additionalTopics")
                } else {
                    logger.error("Failed to send to multiple topics: eventId=${enrichedEvent.eventId}, " +
                        "topics=$additionalTopics", throwable)
                }
            }
            
        } catch (e: Exception) {
            logger.error("Exception sending to multiple topics: eventId=${enrichedEvent.eventId}, " +
                "topics=$additionalTopics", e)
        }
    }
    
    fun sendHighPriorityEvent(enrichedEvent: EnrichedCustomerEvent) {
        try {
            // Send to high priority topic for urgent processing
            val priorityTopic = "high-priority-enriched-events"
            
            val future = kafkaTemplate.send(
                priorityTopic,
                enrichedEvent.customerId,
                enrichedEvent
            )
            
            future.whenComplete { result, throwable ->
                if (throwable == null) {
                    val metadata = result.recordMetadata
                    logger.info("High priority event sent: eventId=${enrichedEvent.eventId}, " +
                        "partition=${metadata.partition()}, offset=${metadata.offset()}")
                } else {
                    logger.error("Failed to send high priority event: eventId=${enrichedEvent.eventId}", throwable)
                }
            }
            
        } catch (e: Exception) {
            logger.error("Exception sending high priority event: eventId=${enrichedEvent.eventId}", e)
        }
    }
    
    fun sendBatchEvent(enrichedEvent: EnrichedCustomerEvent) {
        try {
            // Send to batch processing topic
            val batchTopic = "batch-enriched-events"
            
            val future = kafkaTemplate.send(
                batchTopic,
                enrichedEvent.customerId,
                enrichedEvent
            )
            
            future.whenComplete { result, throwable ->
                if (throwable == null) {
                    val metadata = result.recordMetadata
                    logger.debug("Batch event sent: eventId=${enrichedEvent.eventId}, " +
                        "partition=${metadata.partition()}")
                } else {
                    logger.error("Failed to send batch event: eventId=${enrichedEvent.eventId}", throwable)
                }
            }
            
        } catch (e: Exception) {
            logger.error("Exception sending batch event: eventId=${enrichedEvent.eventId}", e)
        }
    }
    
    // Utility method to get topic name for customer segment
    fun getSegmentTopicName(customerSegment: String): String {
        return "customer-segment-${customerSegment.lowercase()}-events"
    }
    
    // Utility method to get topic name for event type
    fun getEventTypeTopicName(eventType: String): String {
        return "event-type-${eventType.lowercase().replace("_", "-")}"
    }
}