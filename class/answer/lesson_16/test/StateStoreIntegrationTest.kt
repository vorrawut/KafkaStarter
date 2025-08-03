package com.learning.KafkaStarter.test

import com.learning.KafkaStarter.model.UserEvent
import com.learning.KafkaStarter.model.UserProfile
import com.learning.KafkaStarter.streams.StateStoreProcessor
import org.apache.kafka.common.serialization.Serdes
import org.apache.kafka.streams.*
import org.apache.kafka.streams.state.KeyValueStore
import org.apache.kafka.streams.test.TestRecord
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.kafka.support.serializer.JsonSerde
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig
import java.time.Instant
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

@SpringJUnitConfig
class StateStoreIntegrationTest {
    
    private lateinit var testDriver: TopologyTestDriver
    private lateinit var inputTopic: TestInputTopic<String, UserEvent>
    private lateinit var outputTopic: TestOutputTopic<String, UserProfile>
    
    @BeforeEach
    fun setup() {
        val processor = StateStoreProcessor()
        val topology = processor.buildStateStoreTopology()
        
        val props = StreamsConfig(mapOf(
            StreamsConfig.APPLICATION_ID_CONFIG to "test-state-app",
            StreamsConfig.BOOTSTRAP_SERVERS_CONFIG to "dummy:1234",
            StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG to Serdes.String()::class.java,
            StreamsConfig.DEFAULT_VALUE_SERDE_CLASS_CONFIG to JsonSerde::class.java
        ))
        
        testDriver = TopologyTestDriver(topology, props)
        
        inputTopic = testDriver.createInputTopic(
            "user-events",
            Serdes.String().serializer(),
            JsonSerde(UserEvent::class.java).serializer()
        )
        
        outputTopic = testDriver.createOutputTopic(
            "user-profiles-updated",
            Serdes.String().deserializer(),
            JsonSerde(UserProfile::class.java).deserializer()
        )
    }
    
    @AfterEach
    fun cleanup() {
        testDriver.close()
    }
    
    @Test
    fun `should create and update user profiles in state store`() {
        val timestamp = Instant.now().toEpochMilli()
        
        // First event for user
        val firstEvent = UserEvent(
            userId = "user123",
            eventType = "LOGIN",
            timestamp = timestamp,
            data = mapOf(
                "pref_theme" to "dark",
                "pref_language" to "en"
            )
        )
        
        inputTopic.pipeInput("event1", firstEvent)
        
        // Check output
        val firstOutput = outputTopic.readRecord()
        assertNotNull(firstOutput)
        
        val firstProfile = firstOutput.value()
        assertEquals("user123", firstProfile.userId)
        assertEquals(1, firstProfile.eventCount)
        assertEquals(timestamp, firstProfile.lastSeen)
        assertEquals(timestamp, firstProfile.firstSeen)
        assertEquals("dark", firstProfile.preferences["theme"])
        assertEquals("en", firstProfile.preferences["language"])
        
        // Second event for same user
        val secondEvent = UserEvent(
            userId = "user123",
            eventType = "PURCHASE",
            timestamp = timestamp + 1000,
            data = mapOf(
                "amount" to 99.99,
                "pref_currency" to "USD"
            )
        )
        
        inputTopic.pipeInput("event2", secondEvent)
        
        // Check updated output
        val secondOutput = outputTopic.readRecord()
        val secondProfile = secondOutput.value()
        
        assertEquals("user123", secondProfile.userId)
        assertEquals(2, secondProfile.eventCount)
        assertEquals(timestamp + 1000, secondProfile.lastSeen)
        assertEquals(timestamp, secondProfile.firstSeen) // Should remain the same
        assertEquals(99.99, secondProfile.totalValue)
        assertEquals("USD", secondProfile.preferences["currency"])
        assertTrue(secondProfile.preferences.containsKey("theme")) // Previous preferences preserved
    }
    
    @Test
    fun `should handle multiple users independently`() {
        val timestamp = Instant.now().toEpochMilli()
        
        // Events for different users
        val user1Event = UserEvent(
            userId = "user1",
            eventType = "LOGIN",
            timestamp = timestamp,
            data = mapOf("pref_theme" to "light")
        )
        
        val user2Event = UserEvent(
            userId = "user2",
            eventType = "REGISTER",
            timestamp = timestamp + 500,
            data = mapOf("pref_theme" to "dark")
        )
        
        inputTopic.pipeInput("event1", user1Event)
        inputTopic.pipeInput("event2", user2Event)
        
        // Check outputs
        val outputs = outputTopic.readRecordsToList()
        assertEquals(2, outputs.size)
        
        val user1Profile = outputs[0].value()
        val user2Profile = outputs[1].value()
        
        assertEquals("user1", user1Profile.userId)
        assertEquals("user2", user2Profile.userId)
        assertEquals("light", user1Profile.preferences["theme"])
        assertEquals("dark", user2Profile.preferences["theme"])
    }
    
    @Test
    fun `should accumulate total value correctly`() {
        val timestamp = Instant.now().toEpochMilli()
        
        // Multiple purchase events
        val purchases = listOf(
            UserEvent("user123", "PURCHASE", timestamp, mapOf("amount" to 25.50)),
            UserEvent("user123", "PURCHASE", timestamp + 1000, mapOf("amount" to 75.25)),
            UserEvent("user123", "LOGIN", timestamp + 2000, emptyMap()), // Non-purchase event
            UserEvent("user123", "PURCHASE", timestamp + 3000, mapOf("amount" to 100.00))
        )
        
        purchases.forEachIndexed { index, event ->
            inputTopic.pipeInput("event$index", event)
        }
        
        // Read all outputs and check final profile
        val outputs = outputTopic.readRecordsToList()
        assertEquals(4, outputs.size)
        
        val finalProfile = outputs.last().value()
        assertEquals(4, finalProfile.eventCount) // All events counted
        assertEquals(200.75, finalProfile.totalValue) // Only purchase amounts summed
    }
    
    @Test
    fun `should access state store directly`() {
        val timestamp = Instant.now().toEpochMilli()
        
        // Process some events
        val event = UserEvent(
            userId = "user456",
            eventType = "LOGIN",
            timestamp = timestamp,
            data = mapOf("pref_theme" to "blue")
        )
        
        inputTopic.pipeInput("event1", event)
        outputTopic.readRecord() // Consume output to trigger processing
        
        // Access state store directly
        val store: KeyValueStore<String, UserProfile> = testDriver.getKeyValueStore("user-profiles")
        
        val storedProfile = store.get("user456")
        assertNotNull(storedProfile)
        assertEquals("user456", storedProfile.userId)
        assertEquals(1, storedProfile.eventCount)
        assertEquals("blue", storedProfile.preferences["theme"])
    }
}