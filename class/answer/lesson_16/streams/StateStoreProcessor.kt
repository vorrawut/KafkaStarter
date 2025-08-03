package com.learning.KafkaStarter.streams

import com.learning.KafkaStarter.model.UserEvent
import com.learning.KafkaStarter.model.UserProfile
import org.apache.kafka.common.serialization.Serdes
import org.apache.kafka.streams.StreamsBuilder
import org.apache.kafka.streams.Topology
import org.apache.kafka.streams.kstream.*
import org.apache.kafka.streams.processor.api.Processor
import org.apache.kafka.streams.processor.api.ProcessorContext
import org.apache.kafka.streams.processor.api.ProcessorSupplier
import org.apache.kafka.streams.processor.api.Record
import org.apache.kafka.streams.state.*
import org.springframework.kafka.support.serializer.JsonSerde
import org.springframework.stereotype.Component
import java.time.Instant

@Component
class StateStoreProcessor {
    
    fun buildStateStoreTopology(): Topology {
        val builder = StreamsBuilder()
        
        // Create a persistent key-value store for user profiles
        val userProfileStore = Stores.keyValueStoreBuilder(
            Stores.persistentKeyValueStore("user-profiles"),
            Serdes.String(),
            JsonSerde(UserProfile::class.java)
        )
        .withCachingEnabled()
        .withLoggingEnabled(mapOf(
            "cleanup.policy" to "compact",
            "min.compaction.lag.ms" to "3600000" // 1 hour
        ))
        
        // Add the state store to the topology
        builder.addStateStore(userProfileStore)
        
        // Create source stream for user events
        val userEvents: KStream<String, UserEvent> = builder.stream(
            "user-events",
            Consumed.with(Serdes.String(), JsonSerde(UserEvent::class.java))
        )
        
        // Process events using state store
        val processedEvents = userEvents.transformValues(
            ValueTransformerWithKeySupplier {
                UserProfileValueTransformer()
            },
            "user-profiles"
        )
        
        // Output processed events
        processedEvents.to(
            "user-profiles-updated",
            Produced.with(Serdes.String(), JsonSerde(UserProfile::class.java))
        )
        
        return builder.build()
    }
    
    fun buildTimestampedStateStoreTopology(): Topology {
        val builder = StreamsBuilder()
        
        // Create a timestamped key-value store
        val timestampedStore = Stores.keyValueStoreBuilder(
            Stores.persistentTimestampedKeyValueStore("user-profiles-timestamped"),
            Serdes.String(),
            JsonSerde(UserProfile::class.java)
        )
        .withCachingEnabled()
        .withLoggingEnabled(emptyMap())
        
        // Add timestamped store to topology
        builder.addStateStore(timestampedStore)
        
        // Process events with timestamp information
        val timestampedEvents = builder.stream<String, UserEvent>("user-events")
            .process(
                ProcessorSupplier { TimestampedUserProfileProcessor() },
                "user-profiles-timestamped"
            )
        
        return builder.build()
    }
}

class UserProfileValueTransformer : ValueTransformerWithKey<String, UserEvent, UserProfile> {
    private lateinit var stateStore: KeyValueStore<String, UserProfile>
    private lateinit var context: ProcessorContext
    
    override fun init(context: ProcessorContext) {
        this.context = context
        this.stateStore = context.getStateStore("user-profiles")
    }
    
    override fun transform(key: String, userEvent: UserEvent): UserProfile {
        // Get current profile or create new one
        val currentProfile = stateStore.get(userEvent.userId) ?: UserProfile(
            userId = userEvent.userId,
            firstSeen = userEvent.timestamp
        )
        
        // Update profile based on event
        val updatedProfile = currentProfile.copy(
            eventCount = currentProfile.eventCount + 1,
            lastSeen = userEvent.timestamp,
            preferences = mergePreferences(currentProfile.preferences, userEvent.data),
            totalValue = currentProfile.totalValue + extractValue(userEvent)
        )
        
        // Store updated profile
        stateStore.put(userEvent.userId, updatedProfile)
        
        return updatedProfile
    }
    
    override fun close() {
        // Cleanup if needed
    }
    
    private fun mergePreferences(current: Map<String, String>, eventData: Map<String, Any>): Map<String, String> {
        val newPrefs = eventData.filterKeys { it.startsWith("pref_") }
            .mapKeys { it.key.removePrefix("pref_") }
            .mapValues { it.value.toString() }
        
        return current + newPrefs
    }
    
    private fun extractValue(event: UserEvent): Double {
        return when (event.eventType) {
            "PURCHASE" -> event.data["amount"] as? Double ?: 0.0
            else -> 0.0
        }
    }
}

class TimestampedUserProfileProcessor : Processor<String, UserEvent, String, UserProfile> {
    private lateinit var timestampedStore: TimestampedKeyValueStore<String, UserProfile>
    private lateinit var context: ProcessorContext<String, UserProfile>
    
    override fun init(context: ProcessorContext<String, UserProfile>) {
        this.context = context
        this.timestampedStore = context.getStateStore("user-profiles-timestamped")
    }
    
    override fun process(record: Record<String, UserEvent>) {
        val userEvent = record.value()
        val timestamp = record.timestamp()
        
        // Get current profile with timestamp
        val currentProfileWithTimestamp = timestampedStore.get(userEvent.userId)
        val currentProfile = currentProfileWithTimestamp?.value() ?: UserProfile(
            userId = userEvent.userId,
            firstSeen = timestamp
        )
        
        // Update profile
        val updatedProfile = currentProfile.copy(
            eventCount = currentProfile.eventCount + 1,
            lastSeen = timestamp,
            preferences = currentProfile.preferences + extractPreferences(userEvent)
        )
        
        // Store with timestamp
        timestampedStore.put(
            userEvent.userId,
            ValueAndTimestamp.make(updatedProfile, timestamp)
        )
        
        // Forward the updated profile
        context.forward(record.withValue(updatedProfile))
    }
    
    override fun close() {
        // Cleanup if needed
    }
    
    private fun extractPreferences(event: UserEvent): Map<String, String> {
        return event.data.filterKeys { it.startsWith("pref_") }
            .mapKeys { it.key.removePrefix("pref_") }
            .mapValues { it.value.toString() }
    }
}