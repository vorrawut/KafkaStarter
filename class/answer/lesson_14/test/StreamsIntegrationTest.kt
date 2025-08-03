package com.learning.KafkaStarter.test

import com.learning.KafkaStarter.model.UserEvent
import com.learning.KafkaStarter.model.EnrichedUserEvent
import com.learning.KafkaStarter.streams.BasicStreamProcessor
import org.apache.kafka.common.serialization.Serdes
import org.apache.kafka.streams.*
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
class StreamsIntegrationTest {
    
    private lateinit var testDriver: TopologyTestDriver
    private lateinit var inputTopic: TestInputTopic<String, UserEvent>
    private lateinit var outputTopic: TestOutputTopic<String, EnrichedUserEvent>
    
    @BeforeEach
    fun setup() {
        val processor = BasicStreamProcessor()
        val topology = processor.buildBasicTopology()
        
        val props = StreamsConfig(mapOf(
            StreamsConfig.APPLICATION_ID_CONFIG to "test-app",
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
            "enriched-user-events",
            Serdes.String().deserializer(),
            JsonSerde(EnrichedUserEvent::class.java).deserializer()
        )
    }
    
    @AfterEach
    fun cleanup() {
        testDriver.close()
    }
    
    @Test
    fun `should process LOGIN events and enrich them`() {
        // Given
        val loginEvent = UserEvent(
            userId = "user123",
            eventType = "LOGIN",
            timestamp = Instant.now().toEpochMilli(),
            data = mapOf(
                "userAgent" to "Mozilla/5.0 (Mobile; rv:40.0) Gecko/40.0 Firefox/40.0",
                "ip" to "192.168.1.100"
            ),
            sessionId = "session456"
        )
        
        // When
        inputTopic.pipeInput("event123", loginEvent)
        
        // Then
        val outputRecord = outputTopic.readRecord()
        assertNotNull(outputRecord)
        
        val enrichedEvent = outputRecord.value()
        assertEquals("user123", enrichedEvent.userId)
        assertEquals("LOGIN", enrichedEvent.eventType)
        assertEquals(loginEvent.timestamp, enrichedEvent.timestamp)
        
        // Check enrichment
        assertTrue(enrichedEvent.enrichedData.containsKey("deviceType"))
        assertTrue(enrichedEvent.enrichedData.containsKey("location"))
        assertTrue(enrichedEvent.enrichedData.containsKey("sessionInfo"))
        assertEquals("MOBILE", enrichedEvent.enrichedData["deviceType"])
        assertEquals("LOCAL", enrichedEvent.enrichedData["location"])
    }
    
    @Test
    fun `should filter out non-LOGIN events`() {
        // Given
        val pageViewEvent = UserEvent(
            userId = "user123",
            eventType = "PAGE_VIEW",
            timestamp = Instant.now().toEpochMilli(),
            data = mapOf("page" to "/home")
        )
        
        // When
        inputTopic.pipeInput("event123", pageViewEvent)
        
        // Then
        assertTrue(outputTopic.isEmpty)
    }
    
    @Test
    fun `should handle multiple LOGIN events`() {
        // Given
        val events = listOf(
            UserEvent(
                userId = "user1",
                eventType = "LOGIN",
                data = mapOf("userAgent" to "Desktop Browser")
            ),
            UserEvent(
                userId = "user2", 
                eventType = "PAGE_VIEW",
                data = mapOf("page" to "/products")
            ),
            UserEvent(
                userId = "user3",
                eventType = "LOGIN",
                data = mapOf("userAgent" to "Mobile App")
            )
        )
        
        // When
        events.forEachIndexed { index, event ->
            inputTopic.pipeInput("event$index", event)
        }
        
        // Then
        val outputRecords = outputTopic.readRecordsToList()
        assertEquals(2, outputRecords.size) // Only LOGIN events should be processed
        
        outputRecords.forEach { record ->
            assertEquals("LOGIN", record.value().eventType)
            assertTrue(record.value().enrichedData.containsKey("deviceType"))
        }
    }
    
    @Test
    fun `should preserve original data in enriched event`() {
        // Given
        val originalData = mapOf(
            "userAgent" to "Test Browser",
            "ip" to "10.0.0.1",
            "customField" to "customValue"
        )
        
        val loginEvent = UserEvent(
            userId = "user123",
            eventType = "LOGIN",
            data = originalData
        )
        
        // When
        inputTopic.pipeInput("event123", loginEvent)
        
        // Then
        val outputRecord = outputTopic.readRecord()
        val enrichedEvent = outputRecord.value()
        
        assertEquals(originalData, enrichedEvent.originalData)
        assertTrue(enrichedEvent.processingTimestamp > enrichedEvent.timestamp)
    }
}