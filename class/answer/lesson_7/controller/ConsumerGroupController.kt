package com.learning.KafkaStarter.controller

import com.learning.KafkaStarter.model.OrderEvent
import com.learning.KafkaStarter.service.OrderEventProducer
import com.learning.KafkaStarter.service.OrderEventConsumer
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.time.Instant
import java.util.*
import kotlin.random.Random

@RestController
@RequestMapping("/api/consumer-groups")
class ConsumerGroupController {
    
    @Autowired
    private lateinit var orderEventProducer: OrderEventProducer
    
    @Autowired
    private lateinit var orderEventConsumer: OrderEventConsumer
    
    private val logger = org.slf4j.LoggerFactory.getLogger(ConsumerGroupController::class.java)
    
    @PostMapping("/orders")
    fun sendOrderEvent(@RequestBody orderEvent: OrderEvent): ResponseEntity<Map<String, Any>> {
        return try {
            // Send single order event
            orderEventProducer.sendOrderEvent(orderEvent)
            
            logger.info("Order event sent: orderId=${orderEvent.orderId}, customerId=${orderEvent.customerId}")
            
            ResponseEntity.ok(
                mapOf(
                    "success" to true,
                    "message" to "Order event sent successfully",
                    "orderId" to orderEvent.orderId,
                    "customerId" to orderEvent.customerId,
                    "topic" to orderEventProducer.getTopicName(),
                    "timestamp" to System.currentTimeMillis()
                )
            )
        } catch (e: Exception) {
            logger.error("Failed to send order event: orderId=${orderEvent.orderId}", e)
            
            ResponseEntity.internalServerError().body(
                mapOf(
                    "success" to false,
                    "error" to (e.message ?: "Unknown error"),
                    "orderId" to orderEvent.orderId,
                    "timestamp" to System.currentTimeMillis()
                )
            )
        }
    }
    
    @PostMapping("/orders/bulk")
    fun sendBulkOrderEvents(
        @RequestParam(defaultValue = "10") count: Int,
        @RequestParam(defaultValue = "NORMAL") priority: String
    ): ResponseEntity<Map<String, Any>> {
        return try {
            // Generate and send multiple order events
            val orderEvents = generateOrderEvents(count, priority)
            orderEventProducer.sendBulkOrderEvents(orderEvents)
            
            logger.info("Bulk order events sent: count=$count, priority=$priority")
            
            ResponseEntity.ok(
                mapOf(
                    "success" to true,
                    "message" to "Bulk order events sent successfully",
                    "count" to count,
                    "priority" to priority,
                    "orderIds" to orderEvents.map { it.orderId },
                    "customerIds" to orderEvents.map { it.customerId }.distinct(),
                    "topic" to orderEventProducer.getTopicName(),
                    "timestamp" to System.currentTimeMillis()
                )
            )
        } catch (e: Exception) {
            logger.error("Failed to send bulk order events: count=$count", e)
            
            ResponseEntity.internalServerError().body(
                mapOf(
                    "success" to false,
                    "error" to (e.message ?: "Unknown error"),
                    "requestedCount" to count,
                    "timestamp" to System.currentTimeMillis()
                )
            )
        }
    }
    
    @PostMapping("/orders/partition/{partition}")
    fun sendOrderEventToPartition(
        @PathVariable partition: Int,
        @RequestBody orderEvent: OrderEvent
    ): ResponseEntity<Map<String, Any>> {
        return try {
            // Send order event to specific partition
            orderEventProducer.sendOrderEventWithPartition(orderEvent, partition)
            
            logger.info("Order event sent to partition: orderId=${orderEvent.orderId}, partition=$partition")
            
            ResponseEntity.ok(
                mapOf(
                    "success" to true,
                    "message" to "Order event sent to specific partition",
                    "orderId" to orderEvent.orderId,
                    "customerId" to orderEvent.customerId,
                    "requestedPartition" to partition,
                    "topic" to orderEventProducer.getTopicName(),
                    "timestamp" to System.currentTimeMillis()
                )
            )
        } catch (e: Exception) {
            logger.error("Failed to send order event to partition: orderId=${orderEvent.orderId}, partition=$partition", e)
            
            ResponseEntity.internalServerError().body(
                mapOf(
                    "success" to false,
                    "error" to (e.message ?: "Unknown error"),
                    "orderId" to orderEvent.orderId,
                    "requestedPartition" to partition,
                    "timestamp" to System.currentTimeMillis()
                )
            )
        }
    }
    
    @GetMapping("/info")
    fun getConsumerGroupInfo(): ResponseEntity<Map<String, Any>> {
        return try {
            // Get information about consumer groups and processing statistics
            val processingStats = orderEventConsumer.getProcessingStats()
            
            ResponseEntity.ok(
                mapOf(
                    "success" to true,
                    "consumerGroups" to mapOf(
                        "lesson7-consumer-group-A" to mapOf(
                            "purpose" to "Order validation processing",
                            "concurrency" to 1,
                            "topics" to listOf("order-events")
                        ),
                        "lesson7-consumer-group-B" to mapOf(
                            "purpose" to "Inventory management processing",
                            "concurrency" to 1,
                            "topics" to listOf("order-events")
                        ),
                        "lesson7-consumer-group-C" to mapOf(
                            "purpose" to "Analytics processing",
                            "concurrency" to 3,
                            "topics" to listOf("order-events")
                        )
                    ),
                    "processingStatistics" to processingStats,
                    "topic" to orderEventProducer.getTopicName(),
                    "timestamp" to System.currentTimeMillis()
                )
            )
        } catch (e: Exception) {
            logger.error("Failed to get consumer group info", e)
            
            ResponseEntity.internalServerError().body(
                mapOf(
                    "success" to false,
                    "error" to (e.message ?: "Unknown error"),
                    "timestamp" to System.currentTimeMillis()
                )
            )
        }
    }
    
    @PostMapping("/simulate-load")
    fun simulateLoad(
        @RequestParam(defaultValue = "100") eventsPerSecond: Int,
        @RequestParam(defaultValue = "60") durationSeconds: Int
    ): ResponseEntity<Map<String, Any>> {
        return try {
            // Simulate load to test consumer group balancing
            val totalEvents = eventsPerSecond * durationSeconds
            val intervalMs = 1000L / eventsPerSecond
            
            logger.info("Starting load simulation: $eventsPerSecond events/sec for $durationSeconds seconds")
            
            // Run load simulation in background thread
            Thread {
                val startTime = System.currentTimeMillis()
                var sentEvents = 0
                
                repeat(totalEvents) { eventIndex ->
                    try {
                        val orderEvent = generateSingleOrderEvent(
                            priority = if (eventIndex % 10 == 0) "HIGH" else "NORMAL"
                        )
                        
                        orderEventProducer.sendOrderEvent(orderEvent)
                        sentEvents++
                        
                        // Control the rate
                        Thread.sleep(intervalMs)
                        
                    } catch (e: Exception) {
                        logger.error("Error during load simulation at event $eventIndex", e)
                    }
                }
                
                val endTime = System.currentTimeMillis()
                val durationMs = endTime - startTime
                val actualEventsPerSecond = (sentEvents * 1000.0) / durationMs
                
                logger.info("Load simulation completed: sent $sentEvents events in ${durationMs}ms " +
                    "(${String.format("%.2f", actualEventsPerSecond)} events/sec)")
                
            }.start()
            
            ResponseEntity.ok(
                mapOf(
                    "success" to true,
                    "message" to "Load simulation started",
                    "targetEventsPerSecond" to eventsPerSecond,
                    "durationSeconds" to durationSeconds,
                    "totalEventsToSend" to totalEvents,
                    "intervalMs" to intervalMs,
                    "timestamp" to System.currentTimeMillis()
                )
            )
        } catch (e: Exception) {
            logger.error("Failed to start load simulation", e)
            
            ResponseEntity.internalServerError().body(
                mapOf(
                    "success" to false,
                    "error" to (e.message ?: "Unknown error"),
                    "timestamp" to System.currentTimeMillis()
                )
            )
        }
    }
    
    @PostMapping("/reset-counters")
    fun resetProcessingCounters(): ResponseEntity<Map<String, Any>> {
        return try {
            orderEventConsumer.resetCounters()
            
            ResponseEntity.ok(
                mapOf(
                    "success" to true,
                    "message" to "Processing counters reset",
                    "timestamp" to System.currentTimeMillis()
                )
            )
        } catch (e: Exception) {
            ResponseEntity.internalServerError().body(
                mapOf(
                    "success" to false,
                    "error" to (e.message ?: "Unknown error"),
                    "timestamp" to System.currentTimeMillis()
                )
            )
        }
    }
    
    @GetMapping("/stats")
    fun getProcessingStats(): ResponseEntity<Map<String, Any>> {
        return try {
            val stats = orderEventConsumer.getProcessingStats()
            
            ResponseEntity.ok(
                mapOf(
                    "success" to true,
                    "statistics" to stats,
                    "timestamp" to System.currentTimeMillis()
                )
            )
        } catch (e: Exception) {
            ResponseEntity.internalServerError().body(
                mapOf(
                    "success" to false,
                    "error" to (e.message ?: "Unknown error"),
                    "timestamp" to System.currentTimeMillis()
                )
            )
        }
    }
    
    private fun generateOrderEvents(count: Int, priority: String): List<OrderEvent> {
        val customers = listOf("customer-1", "customer-2", "customer-3", "customer-4", "customer-5")
        val products = listOf("product-A", "product-B", "product-C", "product-D", "product-E")
        val statuses = listOf("PENDING", "CONFIRMED", "CANCELLED")
        
        return (1..count).map { index ->
            OrderEvent(
                orderId = "order-${UUID.randomUUID()}",
                customerId = customers.random(),
                productId = products.random(),
                quantity = Random.nextInt(1, 11),
                price = Random.nextDouble(10.0, 500.0),
                status = statuses.random(),
                timestamp = Instant.now().toEpochMilli(),
                priority = priority,
                metadata = mapOf(
                    "batchIndex" to index,
                    "batchSize" to count,
                    "returning_customer" to Random.nextBoolean()
                )
            )
        }
    }
    
    private fun generateSingleOrderEvent(priority: String = "NORMAL"): OrderEvent {
        val customers = listOf("customer-1", "customer-2", "customer-3", "customer-4", "customer-5")
        val products = listOf("product-A", "product-B", "product-C", "product-D", "product-E")
        val statuses = listOf("PENDING", "CONFIRMED")
        
        return OrderEvent(
            orderId = "order-${UUID.randomUUID()}",
            customerId = customers.random(),
            productId = products.random(),
            quantity = Random.nextInt(1, 11),
            price = Random.nextDouble(10.0, 500.0),
            status = statuses.random(),
            timestamp = Instant.now().toEpochMilli(),
            priority = priority,
            metadata = mapOf(
                "loadTest" to true,
                "returning_customer" to Random.nextBoolean()
            )
        )
    }
}