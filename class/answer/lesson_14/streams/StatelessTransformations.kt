package com.learning.KafkaStarter.streams

import com.learning.KafkaStarter.model.UserEvent
import com.learning.KafkaStarter.model.ProcessedEvent
import org.apache.kafka.common.serialization.Serdes
import org.apache.kafka.streams.KeyValue
import org.apache.kafka.streams.StreamsBuilder
import org.apache.kafka.streams.Topology
import org.apache.kafka.streams.kstream.*
import org.springframework.kafka.support.serializer.JsonSerde
import org.springframework.stereotype.Component
import java.time.Instant
import java.util.*

@Component
class StatelessTransformations {
    
    fun buildTransformationTopology(): Topology {
        val builder = StreamsBuilder()
        
        val userEvents: KStream<String, UserEvent> = builder.stream(
            "user-events",
            Consumed.with(Serdes.String(), JsonSerde(UserEvent::class.java))
        )
        
        // 1. FILTER: Remove test events
        val realEvents = userEvents.filter { key, event ->
            !isTestEvent(event)
        }
        
        // 2. MAP: Re-key events by userId instead of eventId
        val userKeyedEvents = realEvents.map { key, event ->
            KeyValue(event.userId, event)
        }
        
        // 3. MAPVALUES: Add processing timestamp to events
        val timestampedEvents = userKeyedEvents.mapValues { event ->
            addProcessingTimestamp(event)
        }
        
        // 4. FLATMAP: Split multi-action events into individual events
        val individualEvents = timestampedEvents.flatMap { key, event ->
            splitMultiActionEvent(key, event)
        }
        
        // 5. SELECTKEY: Change key based on event type
        val eventTypeKeyed = individualEvents.selectKey { key, event ->
            selectKeyByEventType(key, event)
        }
        
        // Send different streams to appropriate output topics
        
        // Send real events (filtered) to real-events topic
        realEvents.to(
            "real-events",
            Produced.with(Serdes.String(), JsonSerde(UserEvent::class.java))
        )
        
        // Send user-keyed events to user-events topic
        userKeyedEvents.to(
            "user-keyed-events",
            Produced.with(Serdes.String(), JsonSerde(UserEvent::class.java))
        )
        
        // Send timestamped events to timestamped-events topic
        timestampedEvents.to(
            "timestamped-events",
            Produced.with(Serdes.String(), JsonSerde(UserEvent::class.java))
        )
        
        // Send individual events to individual-events topic
        individualEvents.to(
            "individual-events",
            Produced.with(Serdes.String(), JsonSerde(UserEvent::class.java))
        )
        
        // Send event-type-keyed events to event-type-keyed-events topic
        eventTypeKeyed.to(
            "event-type-keyed-events",
            Produced.with(Serdes.String(), JsonSerde(UserEvent::class.java))
        )
        
        return builder.build()
    }
    
    private fun isTestEvent(event: UserEvent): Boolean {
        return event.userId.startsWith("test_") || 
               event.data["environment"] == "test" ||
               event.data["isTest"] == true
    }
    
    private fun addProcessingTimestamp(event: UserEvent): UserEvent {
        val enrichedData = event.data.toMutableMap()
        enrichedData["processingTimestamp"] = Instant.now().toEpochMilli()
        enrichedData["processingId"] = UUID.randomUUID().toString()
        
        return event.copy(data = enrichedData)
    }
    
    private fun splitMultiActionEvent(key: String, event: UserEvent): Iterable<KeyValue<String, UserEvent>> {
        val actions = event.data["actions"] as? List<*>
        
        return if (actions != null && actions.isNotEmpty()) {
            // Split into multiple events
            actions.mapIndexed { index, action ->
                val actionData = event.data.toMutableMap()
                actionData["action"] = action
                actionData["actionIndex"] = index
                actionData["totalActions"] = actions.size
                actionData.remove("actions") // Remove the original actions array
                
                KeyValue(
                    "${key}-${index}",
                    event.copy(
                        eventType = "${event.eventType}_${action.toString().uppercase()}",
                        data = actionData
                    )
                )
            }
        } else {
            // Return single event if no actions to split
            listOf(KeyValue(key, event))
        }
    }
    
    private fun selectKeyByEventType(key: String, event: UserEvent): String {
        return when (event.eventType) {
            "LOGIN", "LOGOUT" -> "auth-${event.userId}"
            "PAGE_VIEW", "CLICK" -> "interaction-${event.userId}"
            "PURCHASE", "ADD_TO_CART" -> "commerce-${event.userId}"
            else -> "general-${event.eventType.lowercase()}"
        }
    }
}