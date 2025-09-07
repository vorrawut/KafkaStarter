package com.learning.KafkaStarter.streams

import com.learning.KafkaStarter.model.UserEvent
import org.apache.kafka.streams.KeyValue
import org.apache.kafka.streams.StreamsBuilder
import org.apache.kafka.streams.Topology
import org.apache.kafka.streams.kstream.*
import org.springframework.stereotype.Component

@Component
class StatelessTransformations {
    
    fun buildTransformationTopology(): Topology {
        val builder = StreamsBuilder()
        
        val userEvents: KStream<String, UserEvent> = builder.stream("user-events")
        
        // TODO: Implement different stateless transformations
        
        // 1. FILTER: Remove test events
        val realEvents = TODO("Filter out test events (userId starting with 'test_')")
        
        // 2. MAP: Re-key events by userId instead of eventId
        val userKeyedEvents = TODO("Re-key events by userId")
        
        // 3. MAPVALUES: Add processing timestamp to events
        val timestampedEvents = TODO("Add processing timestamp to events")
        
        // 4. FLATMAP: Split multi-action events into individual events
        val individualEvents = TODO("Split events with multiple actions")
        
        // 5. SELECTKEY: Change key based on event type
        val eventTypeKeyed = TODO("Key events by event type")
        
        // TODO: Send different streams to appropriate output topics
        TODO("Send processed streams to output topics")
        
        return builder.build()
    }
    
    private fun isTestEvent(event: UserEvent): Boolean {
        // TODO: Implement logic to identify test events
        return TODO("Check if event is from test user")
    }
    
    private fun addProcessingTimestamp(event: UserEvent): UserEvent {
        // TODO: Add current timestamp to event data
        return TODO("Add processing timestamp")
    }
    
    private fun splitMultiActionEvent(key: String, event: UserEvent): Iterable<KeyValue<String, UserEvent>> {
        // TODO: Split events that contain multiple actions
        // HINT: Check if event.data contains "actions" array
        return TODO("Split multi-action events")
    }
    
    private fun selectKeyByEventType(key: String, event: UserEvent): String {
        // TODO: Generate new key based on event type
        // HINT: Combine event type with other attributes
        return TODO("Generate event-type-based key")
    }
}