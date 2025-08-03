package com.learning.KafkaStarter.test

import com.learning.KafkaStarter.model.OrderEvent
import com.learning.KafkaStarter.service.OrderEventProducer
import com.learning.KafkaStarter.service.OrderEventConsumer
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.kafka.test.context.EmbeddedKafka
import org.springframework.test.annotation.DirtiesContext
import java.time.Instant
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@SpringBootTest
@DirtiesContext
@EmbeddedKafka(
    partitions = 3,
    topics = ["order-events"],
    brokerProperties = [
        "listeners=PLAINTEXT://localhost:9093",
        "port=9093"
    ]
)
class ConsumerGroupIntegrationTest {
    
    @Autowired
    private lateinit var orderEventProducer: OrderEventProducer
    
    @Autowired
    private lateinit var orderEventConsumer: OrderEventConsumer
    
    @Test
    fun `should distribute messages across consumer groups`() {
        // Reset counters before test
        orderEventConsumer.resetCounters()
        
        // Create test order events
        val orderEvents = listOf(
            OrderEvent(
                orderId = "test-order-1",
                customerId = "customer-1",
                productId = "product-A",
                quantity = 2,
                price = 100.0,
                status = "PENDING",
                timestamp = Instant.now().toEpochMilli()
            ),
            OrderEvent(
                orderId = "test-order-2",
                customerId = "customer-2",
                productId = "product-B",
                quantity = 1,
                price = 50.0,
                status = "CONFIRMED",
                timestamp = Instant.now().toEpochMilli()
            ),
            OrderEvent(
                orderId = "test-order-3",
                customerId = "customer-3",
                productId = "product-C",
                quantity = 5,
                price = 200.0,
                status = "PENDING",
                timestamp = Instant.now().toEpochMilli()
            )
        )
        
        // Send order events
        orderEvents.forEach { orderEvent ->
            orderEventProducer.sendOrderEvent(orderEvent)
        }
        
        // Wait for processing
        Thread.sleep(3000)
        
        // Verify that all consumer groups processed the events
        val groupACount = orderEventConsumer.getGroupAProcessedCount()
        val groupBCount = orderEventConsumer.getGroupBProcessedCount()
        val groupCCount = orderEventConsumer.getGroupCProcessedCount()
        
        // Each consumer group should process all events (they're in different groups)
        assertEquals(orderEvents.size.toLong(), groupACount, "Group A should process all events")
        assertEquals(orderEvents.size.toLong(), groupBCount, "Group B should process all events")
        assertEquals(orderEvents.size.toLong(), groupCCount, "Group C should process all events")
        
        // Get processing statistics
        val stats = orderEventConsumer.getProcessingStats()
        val totalProcessed = stats["totalProcessed"] as Long
        
        // Total processed should be 3 groups × 3 events = 9
        assertEquals(9L, totalProcessed, "Total processed events should be 9 (3 events × 3 groups)")
    }
    
    @Test
    fun `should handle bulk order events correctly`() {
        // Reset counters before test
        orderEventConsumer.resetCounters()
        
        // Create bulk order events
        val bulkOrderEvents = (1..5).map { index ->
            OrderEvent(
                orderId = "bulk-order-$index",
                customerId = "customer-$index",
                productId = "product-${('A'..'E').random()}",
                quantity = index,
                price = index * 25.0,
                status = if (index % 2 == 0) "CONFIRMED" else "PENDING",
                timestamp = Instant.now().toEpochMilli(),
                metadata = mapOf("bulk" to true, "index" to index)
            )
        }
        
        // Send bulk events
        orderEventProducer.sendBulkOrderEvents(bulkOrderEvents)
        
        // Wait for processing
        Thread.sleep(4000)
        
        // Verify processing counts
        val groupACount = orderEventConsumer.getGroupAProcessedCount()
        val groupBCount = orderEventConsumer.getGroupBProcessedCount()
        val groupCCount = orderEventConsumer.getGroupCProcessedCount()
        
        // Each group should process all 5 events
        assertEquals(5L, groupACount, "Group A should process all 5 bulk events")
        assertEquals(5L, groupBCount, "Group B should process all 5 bulk events")
        assertEquals(5L, groupCCount, "Group C should process all 5 bulk events")
        
        // Verify total processing
        val stats = orderEventConsumer.getProcessingStats()
        assertEquals(15L, stats["totalProcessed"], "Total should be 15 (5 events × 3 groups)")
    }
    
    @Test
    fun `should handle different order statuses correctly`() {
        // Reset counters before test
        orderEventConsumer.resetCounters()
        
        // Create orders with different statuses
        val orderStatuses = listOf("PENDING", "CONFIRMED", "CANCELLED")
        val statusTestEvents = orderStatuses.map { status ->
            OrderEvent(
                orderId = "status-test-$status",
                customerId = "customer-status",
                productId = "product-status",
                quantity = 1,
                price = 100.0,
                status = status,
                timestamp = Instant.now().toEpochMilli()
            )
        }
        
        // Send events with different statuses
        statusTestEvents.forEach { orderEvent ->
            orderEventProducer.sendOrderEvent(orderEvent)
        }
        
        // Wait for processing
        Thread.sleep(3000)
        
        // Verify all statuses were processed
        val groupACount = orderEventConsumer.getGroupAProcessedCount()
        assertEquals(3L, groupACount, "Group A should process all status variations")
    }
    
    @Test
    fun `should handle partition-specific routing`() {
        // Reset counters before test
        orderEventConsumer.resetCounters()
        
        // Send events to specific partitions
        val partitionTestEvent = OrderEvent(
            orderId = "partition-test-1",
            customerId = "customer-partition",
            productId = "product-partition",
            quantity = 1,
            price = 150.0,
            status = "CONFIRMED",
            timestamp = Instant.now().toEpochMilli()
        )
        
        // Send to partition 0
        orderEventProducer.sendOrderEventWithPartition(partitionTestEvent, 0)
        
        // Send another to partition 1
        val partitionTestEvent2 = partitionTestEvent.copy(orderId = "partition-test-2")
        orderEventProducer.sendOrderEventWithPartition(partitionTestEvent2, 1)
        
        // Wait for processing
        Thread.sleep(2000)
        
        // Verify events were processed
        val groupACount = orderEventConsumer.getGroupAProcessedCount()
        val groupBCount = orderEventConsumer.getGroupBProcessedCount()
        val groupCCount = orderEventConsumer.getGroupCProcessedCount()
        
        assertTrue(groupACount >= 2, "Group A should process partition-routed events")
        assertTrue(groupBCount >= 2, "Group B should process partition-routed events")
        assertTrue(groupCCount >= 2, "Group C should process partition-routed events")
    }
    
    @Test
    fun `should demonstrate load balancing within consumer group C`() {
        // Reset counters before test
        orderEventConsumer.resetCounters()
        
        // Send many events to test concurrency in Group C
        val concurrencyTestEvents = (1..10).map { index ->
            OrderEvent(
                orderId = "concurrency-test-$index",
                customerId = "customer-$index",
                productId = "product-concurrency",
                quantity = 1,
                price = 50.0,
                status = "CONFIRMED",
                timestamp = Instant.now().toEpochMilli(),
                priority = if (index % 3 == 0) "HIGH" else "NORMAL"
            )
        }
        
        // Send events quickly to test concurrent processing
        concurrencyTestEvents.forEach { orderEvent ->
            orderEventProducer.sendOrderEvent(orderEvent)
        }
        
        // Wait for processing (Group C has 3 concurrent consumers)
        Thread.sleep(5000)
        
        // Verify Group C processed all events
        val groupCCount = orderEventConsumer.getGroupCProcessedCount()
        assertEquals(10L, groupCCount, "Group C should process all 10 concurrency test events")
        
        // Verify processing statistics
        val stats = orderEventConsumer.getProcessingStats()
        val groupCStats = stats["groupC"] as Map<*, *>
        assertEquals(3, groupCStats["concurrency"], "Group C should have concurrency of 3")
    }
}