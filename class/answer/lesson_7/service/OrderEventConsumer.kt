package com.learning.KafkaStarter.service

import com.learning.KafkaStarter.model.OrderEvent
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.kafka.support.KafkaHeaders
import org.springframework.messaging.handler.annotation.Header
import org.springframework.messaging.handler.annotation.Payload
import org.springframework.stereotype.Service
import java.util.concurrent.atomic.AtomicLong

@Service
class OrderEventConsumer {
    
    private val logger = org.slf4j.LoggerFactory.getLogger(OrderEventConsumer::class.java)
    
    // Counters to track processing across consumer groups
    private val groupACounter = AtomicLong(0)
    private val groupBCounter = AtomicLong(0)
    private val groupCCounter = AtomicLong(0)
    
    @KafkaListener(
        topics = ["order-events"],
        groupId = "lesson7-consumer-group-A"
    )
    fun consumeOrderEventsGroupA(
        @Payload orderEvent: OrderEvent,
        @Header(KafkaHeaders.RECEIVED_PARTITION_ID) partition: Int,
        @Header(KafkaHeaders.OFFSET) offset: Long,
        @Header(KafkaHeaders.RECEIVED_TOPIC) topic: String
    ) {
        try {
            // Process order event in consumer group A (focus on order validation)
            val processedCount = groupACounter.incrementAndGet()
            
            logger.info("GROUP-A processing order: orderId=${orderEvent.orderId}, " +
                "customerId=${orderEvent.customerId}, topic=$topic, partition=$partition, " +
                "offset=$offset, processedCount=$processedCount")
            
            // Simulate order validation processing
            Thread.sleep(100) // Simulate processing time
            
            when (orderEvent.status) {
                "PENDING" -> {
                    logger.info("GROUP-A: Validating pending order: ${orderEvent.orderId}")
                    // Simulate validation logic
                }
                "CONFIRMED" -> {
                    logger.info("GROUP-A: Processing confirmed order: ${orderEvent.orderId}")
                    // Simulate confirmation processing
                }
                "CANCELLED" -> {
                    logger.info("GROUP-A: Handling cancelled order: ${orderEvent.orderId}")
                    // Simulate cancellation processing
                }
                else -> {
                    logger.warn("GROUP-A: Unknown order status: ${orderEvent.status} for order ${orderEvent.orderId}")
                }
            }
            
            logger.debug("GROUP-A completed processing order: ${orderEvent.orderId} in ${Thread.currentThread().name}")
            
        } catch (e: Exception) {
            logger.error("GROUP-A failed to process order: orderId=${orderEvent.orderId}, " +
                "partition=$partition, offset=$offset", e)
            
            // In a real application, you might:
            // - Send to dead letter topic
            // - Implement retry logic
            // - Alert monitoring systems
            throw e // Re-throw to trigger consumer group retry/error handling
        }
    }
    
    @KafkaListener(
        topics = ["order-events"],
        groupId = "lesson7-consumer-group-B"
    )
    fun consumeOrderEventsGroupB(
        @Payload orderEvent: OrderEvent,
        @Header(KafkaHeaders.RECEIVED_PARTITION_ID) partition: Int,
        @Header(KafkaHeaders.OFFSET) offset: Long
    ) {
        try {
            // Process order event in consumer group B (focus on inventory management)
            val processedCount = groupBCounter.incrementAndGet()
            
            logger.info("GROUP-B processing order: orderId=${orderEvent.orderId}, " +
                "productId=${orderEvent.productId}, quantity=${orderEvent.quantity}, " +
                "partition=$partition, offset=$offset, processedCount=$processedCount")
            
            // Simulate inventory management processing
            Thread.sleep(150) // Different processing time to show load distribution
            
            if (orderEvent.quantity > 0) {
                logger.info("GROUP-B: Checking inventory for product ${orderEvent.productId}, " +
                    "quantity=${orderEvent.quantity}")
                
                // Simulate inventory check
                val isAvailable = orderEvent.quantity <= 100 // Mock inventory logic
                
                if (isAvailable) {
                    logger.info("GROUP-B: Inventory available for order ${orderEvent.orderId}")
                    // Simulate inventory reservation
                } else {
                    logger.warn("GROUP-B: Insufficient inventory for order ${orderEvent.orderId}")
                    // Simulate inventory shortage handling
                }
            } else {
                logger.error("GROUP-B: Invalid quantity ${orderEvent.quantity} for order ${orderEvent.orderId}")
            }
            
            logger.debug("GROUP-B completed processing order: ${orderEvent.orderId} in ${Thread.currentThread().name}")
            
        } catch (e: Exception) {
            logger.error("GROUP-B failed to process order: orderId=${orderEvent.orderId}, " +
                "partition=$partition, offset=$offset", e)
            throw e
        }
    }
    
    @KafkaListener(
        topics = ["order-events"],
        groupId = "lesson7-consumer-group-C",
        concurrency = "3" // Multiple consumer instances within the same group
    )
    fun consumeOrderEventsGroupC(
        consumerRecord: ConsumerRecord<String, OrderEvent>
    ) {
        try {
            val orderEvent = consumerRecord.value()
            val processedCount = groupCCounter.incrementAndGet()
            
            // Process order event with multiple consumer instances (focus on analytics)
            logger.info("GROUP-C processing order: orderId=${orderEvent.orderId}, " +
                "key=${consumerRecord.key()}, partition=${consumerRecord.partition()}, " +
                "offset=${consumerRecord.offset()}, timestamp=${consumerRecord.timestamp()}, " +
                "thread=${Thread.currentThread().name}, processedCount=$processedCount")
            
            // Simulate analytics processing with different processing times
            val processingTime = when (orderEvent.priority) {
                "HIGH" -> 50L
                "NORMAL" -> 100L
                "LOW" -> 200L
                else -> 100L
            }
            Thread.sleep(processingTime)
            
            // Calculate order metrics
            val orderValue = orderEvent.quantity * orderEvent.price
            
            logger.info("GROUP-C: Analyzing order ${orderEvent.orderId} - " +
                "value=$orderValue, priority=${orderEvent.priority}, " +
                "customer=${orderEvent.customerId}")
            
            // Simulate different analytics based on order value
            when {
                orderValue > 1000.0 -> {
                    logger.info("GROUP-C: High-value order detected: ${orderEvent.orderId} ($orderValue)")
                    // Simulate high-value order analytics
                }
                orderValue > 100.0 -> {
                    logger.info("GROUP-C: Medium-value order: ${orderEvent.orderId} ($orderValue)")
                    // Simulate medium-value order analytics
                }
                else -> {
                    logger.info("GROUP-C: Low-value order: ${orderEvent.orderId} ($orderValue)")
                    // Simulate low-value order analytics
                }
            }
            
            // Simulate customer analytics
            if (orderEvent.metadata.containsKey("returning_customer")) {
                logger.info("GROUP-C: Returning customer order: ${orderEvent.customerId}")
            } else {
                logger.info("GROUP-C: New customer order: ${orderEvent.customerId}")
            }
            
            logger.debug("GROUP-C completed processing order: ${orderEvent.orderId} " +
                "in ${Thread.currentThread().name} (processing time: ${processingTime}ms)")
            
        } catch (e: Exception) {
            val orderEvent = consumerRecord.value()
            logger.error("GROUP-C failed to process order: orderId=${orderEvent?.orderId}, " +
                "partition=${consumerRecord.partition()}, offset=${consumerRecord.offset()}, " +
                "thread=${Thread.currentThread().name}", e)
            throw e
        }
    }
    
    // Utility methods to get processing statistics
    fun getGroupAProcessedCount(): Long = groupACounter.get()
    fun getGroupBProcessedCount(): Long = groupBCounter.get() 
    fun getGroupCProcessedCount(): Long = groupCCounter.get()
    
    fun resetCounters() {
        groupACounter.set(0)
        groupBCounter.set(0)
        groupCCounter.set(0)
        logger.info("All consumer group counters reset")
    }
    
    fun getProcessingStats(): Map<String, Any> {
        return mapOf(
            "groupA" to mapOf(
                "processedCount" to groupACounter.get(),
                "description" to "Order validation processing"
            ),
            "groupB" to mapOf(
                "processedCount" to groupBCounter.get(),
                "description" to "Inventory management processing"
            ),
            "groupC" to mapOf(
                "processedCount" to groupCCounter.get(),
                "description" to "Analytics processing (3 concurrent instances)",
                "concurrency" to 3
            ),
            "totalProcessed" to (groupACounter.get() + groupBCounter.get() + groupCCounter.get())
        )
    }
}