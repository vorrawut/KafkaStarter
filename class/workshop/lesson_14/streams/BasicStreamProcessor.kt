package com.learning.KafkaStarter.streams

import com.learning.KafkaStarter.model.UserEvent
import com.learning.KafkaStarter.model.EnrichedUserEvent
import org.apache.kafka.common.serialization.Serdes
import org.apache.kafka.streams.StreamsBuilder
import org.apache.kafka.streams.Topology
import org.apache.kafka.streams.kstream.*
import org.springframework.kafka.support.serializer.JsonSerde
import org.springframework.stereotype.Component

@Component
class BasicStreamProcessor {
    
    fun buildBasicTopology(): Topology {
        val builder = StreamsBuilder()
        
        // TODO: Create source stream from "user-events" topic
        // HINT: Use builder.stream() with appropriate serdes
        val userEvents: KStream<String, UserEvent> = TODO("Implement source stream")
        
        // TODO: Filter events to only process LOGIN events
        // HINT: Use filter() method with appropriate predicate
        val loginEvents = TODO("Implement filter for LOGIN events")
        
        // TODO: Transform/enrich the events
        // HINT: Use mapValues() to transform UserEvent to EnrichedUserEvent
        val enrichedEvents = TODO("Implement event enrichment")
        
        // TODO: Send enriched events to "enriched-user-events" topic
        // HINT: Use to() method with appropriate topic name and serdes
        TODO("Implement sink to output topic")
        
        return builder.build()
    }
    
    private fun enrichEvent(event: UserEvent): EnrichedUserEvent {
        // TODO: Implement event enrichment logic
        // HINT: Add additional data like geolocation, device type, etc.
        return TODO("Implement enrichment logic")
    }
    
    private fun detectDeviceType(data: Map<String, Any>): String {
        // TODO: Implement device type detection from user agent or other data
        // HINT: Check for mobile, tablet, desktop patterns
        return TODO("Implement device type detection")
    }
    
    private fun extractLocation(data: Map<String, Any>): String {
        // TODO: Extract location information from event data
        // HINT: Look for IP address, coordinates, or city information
        return TODO("Implement location extraction")
    }
}