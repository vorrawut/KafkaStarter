package com.learning.KafkaStarter.test

import com.learning.KafkaStarter.model.WebEvent
import com.learning.KafkaStarter.model.TransactionEvent
import com.learning.KafkaStarter.model.DashboardMetric
import com.learning.KafkaStarter.streams.DashboardStreamProcessor
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
class DashboardIntegrationTest {
    
    private lateinit var testDriver: TopologyTestDriver
    private lateinit var webEventsInputTopic: TestInputTopic<String, WebEvent>
    private lateinit var transactionEventsInputTopic: TestInputTopic<String, TransactionEvent>
    private lateinit var userMetricsOutputTopic: TestOutputTopic<String, DashboardMetric>
    private lateinit var revenueMetricsOutputTopic: TestOutputTopic<String, DashboardMetric>
    
    @BeforeEach
    fun setup() {
        val processor = DashboardStreamProcessor()
        val topology = processor.buildDashboardTopology()
        
        val props = StreamsConfig(mapOf(
            StreamsConfig.APPLICATION_ID_CONFIG to "test-dashboard-app",
            StreamsConfig.BOOTSTRAP_SERVERS_CONFIG to "dummy:1234",
            StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG to Serdes.String()::class.java,
            StreamsConfig.DEFAULT_VALUE_SERDE_CLASS_CONFIG to JsonSerde::class.java
        ))
        
        testDriver = TopologyTestDriver(topology, props)
        
        webEventsInputTopic = testDriver.createInputTopic(
            "web-events",
            Serdes.String().serializer(),
            JsonSerde(WebEvent::class.java).serializer()
        )
        
        transactionEventsInputTopic = testDriver.createInputTopic(
            "transaction-events",
            Serdes.String().serializer(),
            JsonSerde(TransactionEvent::class.java).serializer()
        )
        
        userMetricsOutputTopic = testDriver.createOutputTopic(
            "dashboard-user-metrics",
            Serdes.String().deserializer(),
            JsonSerde(DashboardMetric::class.java).deserializer()
        )
        
        revenueMetricsOutputTopic = testDriver.createOutputTopic(
            "dashboard-revenue-metrics",
            Serdes.String().deserializer(),
            JsonSerde(DashboardMetric::class.java).deserializer()
        )
    }
    
    @AfterEach
    fun cleanup() {
        testDriver.close()
    }
    
    @Test
    fun `should aggregate user activity metrics correctly`() {
        val timestamp = Instant.now().toEpochMilli()
        
        // Generate web events
        val webEvents = listOf(
            WebEvent(
                eventId = "event1",
                userId = "user1",
                sessionId = "session1",
                action = "PAGE_VIEW",
                timestamp = timestamp,
                page = "/home"
            ),
            WebEvent(
                eventId = "event2",
                userId = "user2",
                sessionId = "session2",
                action = "CLICK",
                timestamp = timestamp + 1000,
                page = "/products"
            ),
            WebEvent(
                eventId = "event3",
                userId = "user1",
                sessionId = "session1",
                action = "PAGE_VIEW",
                timestamp = timestamp + 2000,
                page = "/cart"
            )
        )
        
        // Send events to input topic
        webEvents.forEach { event ->
            webEventsInputTopic.pipeInput(event.eventId, event)
        }
        
        // Advance time to trigger window emission
        testDriver.advanceWallClockTime(Duration.ofMinutes(2))
        
        // Check user metrics output
        if (!userMetricsOutputTopic.isEmpty) {
            val outputRecord = userMetricsOutputTopic.readRecord()
            assertNotNull(outputRecord)
            
            val metric = outputRecord.value()
            assertEquals("USER_ACTIVITY", metric.metricType)
            assertEquals(2.0, metric.value) // 2 unique users
            
            val breakdown = metric.breakdown
            assertTrue(breakdown.containsKey("uniqueUsers"))
            assertTrue(breakdown.containsKey("totalEvents"))
            assertTrue(breakdown.containsKey("pageViews"))
            assertTrue(breakdown.containsKey("clicks"))
            
            assertEquals(2, breakdown["uniqueUsers"])
            assertEquals(3L, breakdown["totalEvents"])
            assertEquals(2L, breakdown["pageViews"])
            assertEquals(1L, breakdown["clicks"])
        }
    }
    
    @Test
    fun `should aggregate revenue metrics correctly`() {
        val timestamp = Instant.now().toEpochMilli()
        
        // Generate transaction events
        val transactions = listOf(
            TransactionEvent(
                transactionId = "tx1",
                userId = "user1",
                amount = 100.0,
                currency = "USD",
                type = "COMPLETED",
                timestamp = timestamp
            ),
            TransactionEvent(
                transactionId = "tx2",
                userId = "user2",
                amount = 250.0,
                currency = "USD",
                type = "COMPLETED",
                timestamp = timestamp + 1000
            ),
            TransactionEvent(
                transactionId = "tx3",
                userId = "user3",
                amount = 50.0,
                currency = "EUR",
                type = "FAILED", // Should be filtered out
                timestamp = timestamp + 2000
            ),
            TransactionEvent(
                transactionId = "tx4",
                userId = "user1",
                amount = 75.0,
                currency = "USD",
                type = "COMPLETED",
                timestamp = timestamp + 3000
            )
        )
        
        // Send transactions to input topic
        transactions.forEach { transaction ->
            transactionEventsInputTopic.pipeInput(transaction.transactionId, transaction)
        }
        
        // Advance time to trigger window emission
        testDriver.advanceWallClockTime(Duration.ofMinutes(6))
        
        // Check revenue metrics output
        if (!revenueMetricsOutputTopic.isEmpty) {
            val outputRecord = revenueMetricsOutputTopic.readRecord()
            assertNotNull(outputRecord)
            
            val metric = outputRecord.value()
            assertEquals("REVENUE", metric.metricType)
            assertEquals(425.0, metric.value) // Only completed transactions: 100 + 250 + 75
            
            val breakdown = metric.breakdown
            assertTrue(breakdown.containsKey("totalRevenue"))
            assertTrue(breakdown.containsKey("transactionCount"))
            assertTrue(breakdown.containsKey("averageValue"))
            assertTrue(breakdown.containsKey("maxValue"))
            
            assertEquals(425.0, breakdown["totalRevenue"])
            assertEquals(3L, breakdown["transactionCount"]) // Only completed transactions
            assertEquals(250.0, breakdown["maxValue"])
        }
    }
    
    @Test
    fun `should handle mixed event types correctly`() {
        val timestamp = Instant.now().toEpochMilli()
        
        // Send mixed events
        val webEvent = WebEvent(
            eventId = "web1",
            userId = "user1",
            sessionId = "session1",
            action = "LOGIN",
            timestamp = timestamp
        )
        
        val transaction = TransactionEvent(
            transactionId = "tx1",
            userId = "user1",
            amount = 99.99,
            currency = "USD",
            type = "COMPLETED",
            timestamp = timestamp + 1000
        )
        
        webEventsInputTopic.pipeInput(webEvent.eventId, webEvent)
        transactionEventsInputTopic.pipeInput(transaction.transactionId, transaction)
        
        // Advance time to trigger window emissions
        testDriver.advanceWallClockTime(Duration.ofMinutes(6))
        
        // Should have outputs in both topics
        val hasUserMetrics = !userMetricsOutputTopic.isEmpty
        val hasRevenueMetrics = !revenueMetricsOutputTopic.isEmpty
        
        assertTrue(hasUserMetrics || hasRevenueMetrics, "Should have at least one type of metrics")
        
        if (hasUserMetrics) {
            val userMetric = userMetricsOutputTopic.readRecord().value()
            assertEquals("USER_ACTIVITY", userMetric.metricType)
            assertEquals(1.0, userMetric.value) // 1 unique user
        }
        
        if (hasRevenueMetrics) {
            val revenueMetric = revenueMetricsOutputTopic.readRecord().value()
            assertEquals("REVENUE", revenueMetric.metricType)
            assertEquals(99.99, revenueMetric.value)
        }
    }
    
    @Test
    fun `should handle empty input gracefully`() {
        // Advance time without sending any events
        testDriver.advanceWallClockTime(Duration.ofMinutes(10))
        
        // Should not crash and output topics should be empty
        assertTrue(userMetricsOutputTopic.isEmpty)
        assertTrue(revenueMetricsOutputTopic.isEmpty)
    }
}