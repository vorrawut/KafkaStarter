package com.learning.KafkaStarter.streams

import com.learning.KafkaStarter.model.UserEvent
import com.learning.KafkaStarter.model.EnrichedUserEvent
import org.apache.kafka.common.serialization.Serdes
import org.apache.kafka.streams.StreamsBuilder
import org.apache.kafka.streams.Topology
import org.apache.kafka.streams.kstream.*
import org.springframework.kafka.support.serializer.JsonSerde
import org.springframework.stereotype.Component
import java.time.Instant

@Component
class BasicStreamProcessor {
    
    fun buildBasicTopology(): Topology {
        val builder = StreamsBuilder()
        
        // Source stream from "user-events" topic
        val userEvents: KStream<String, UserEvent> = builder.stream(
            "user-events",
            Consumed.with(Serdes.String(), JsonSerde(UserEvent::class.java))
        )
        
        // Filter events to only process LOGIN events
        val loginEvents = userEvents.filter { key, event ->
            event.eventType == "LOGIN"
        }
        
        // Transform/enrich the events
        val enrichedEvents = loginEvents.mapValues { event ->
            enrichEvent(event)
        }
        
        // Send enriched events to "enriched-user-events" topic
        enrichedEvents.to(
            "enriched-user-events",
            Produced.with(Serdes.String(), JsonSerde(EnrichedUserEvent::class.java))
        )
        
        return builder.build()
    }
    
    private fun enrichEvent(event: UserEvent): EnrichedUserEvent {
        return EnrichedUserEvent(
            userId = event.userId,
            eventType = event.eventType,
            timestamp = event.timestamp,
            originalData = event.data,
            enrichedData = mapOf(
                "deviceType" to detectDeviceType(event.data),
                "location" to extractLocation(event.data),
                "sessionInfo" to enrichSessionInfo(event),
                "processingNode" to getProcessingNodeInfo(),
                "enrichmentVersion" to "1.0"
            ),
            processingTimestamp = Instant.now().toEpochMilli()
        )
    }
    
    private fun detectDeviceType(data: Map<String, Any>): String {
        val userAgent = data["userAgent"] as? String ?: ""
        
        return when {
            userAgent.contains("Mobile", ignoreCase = true) -> "MOBILE"
            userAgent.contains("Tablet", ignoreCase = true) -> "TABLET"
            userAgent.contains("Desktop", ignoreCase = true) -> "DESKTOP"
            else -> "UNKNOWN"
        }
    }
    
    private fun extractLocation(data: Map<String, Any>): String {
        return when {
            data.containsKey("country") -> data["country"] as? String ?: "UNKNOWN"
            data.containsKey("ip") -> {
                // In real implementation, you would do IP geolocation lookup
                val ip = data["ip"] as? String ?: ""
                when {
                    ip.startsWith("192.168") -> "LOCAL"
                    ip.startsWith("10.") -> "PRIVATE"
                    else -> "EXTERNAL"
                }
            }
            else -> "UNKNOWN"
        }
    }
    
    private fun enrichSessionInfo(event: UserEvent): Map<String, Any> {
        return mapOf(
            "sessionId" to (event.sessionId ?: "unknown"),
            "isFirstEvent" to (event.data["isFirstEvent"] ?: false),
            "sessionDuration" to (event.data["sessionDuration"] ?: 0)
        )
    }
    
    private fun getProcessingNodeInfo(): String {
        return "stream-processor-${System.currentTimeMillis()}"
    }
}