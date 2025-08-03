package com.learning.KafkaStarter.service

import com.learning.KafkaStarter.model.EnrichedCustomerEvent
import com.learning.KafkaStarter.model.FilteredEvent
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.stereotype.Service

@Service
class TransformationProducer {
    
    @Autowired
    private lateinit var kafkaTemplate: KafkaTemplate<String, Any>
    
    fun sendEnrichedEvent(enrichedEvent: EnrichedCustomerEvent) {
        try {
            // TODO: Send enriched event to appropriate topic
            // HINT: Use customer segment to determine target topic
            TODO("Send enriched event")
        } catch (e: Exception) {
            // TODO: Handle send exceptions
            TODO("Handle enriched event send exception")
        }
    }
    
    fun sendFilteredEvent(filteredEvent: FilteredEvent) {
        try {
            // TODO: Send filtered event to filtered events topic
            // HINT: Send to topic for auditing and monitoring filtered events
            TODO("Send filtered event")
        } catch (e: Exception) {
            // TODO: Handle filtered event send exceptions
            TODO("Handle filtered event send exception")
        }
    }
    
    fun sendToCustomerSegmentTopic(enrichedEvent: EnrichedCustomerEvent) {
        // TODO: Send event to customer segment-specific topic
        // HINT: Route to different topics based on customer segment
        TODO("Send to customer segment topic")
    }
    
    fun sendToEventTypeTopic(enrichedEvent: EnrichedCustomerEvent) {
        // TODO: Send event to event type-specific topic
        // HINT: Route to different topics based on event type
        TODO("Send to event type topic")
    }
}