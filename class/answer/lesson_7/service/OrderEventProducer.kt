package com.learning.KafkaStarter.service

import com.learning.KafkaStarter.model.OrderEvent
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.kafka.support.SendResult
import org.springframework.stereotype.Service
import org.springframework.util.concurrent.ListenableFutureCallback
import java.util.concurrent.CompletableFuture

@Service
class OrderEventProducer {
    
    @Autowired
    private lateinit var kafkaTemplate: KafkaTemplate<String, OrderEvent>
    
    private val topicName = "order-events"
    private val logger = org.slf4j.LoggerFactory.getLogger(OrderEventProducer::class.java)
    
    fun sendOrderEvent(orderEvent: OrderEvent) {
        try {
            // Send order event to Kafka topic with customer ID as key for partitioning
            val future: CompletableFuture<SendResult<String, OrderEvent>> = kafkaTemplate.send(
                topicName, 
                orderEvent.customerId, // Use customer ID as partition key
                orderEvent
            )
            
            future.whenComplete { result, throwable ->
                if (throwable == null) {
                    val metadata = result.recordMetadata
                    logger.info("Order event sent successfully: orderId=${orderEvent.orderId}, " +
                        "partition=${metadata.partition()}, offset=${metadata.offset()}")
                } else {
                    logger.error("Failed to send order event: orderId=${orderEvent.orderId}", throwable)
                }
            }
            
        } catch (e: Exception) {
            logger.error("Exception while sending order event: orderId=${orderEvent.orderId}", e)
            throw RuntimeException("Failed to send order event", e)
        }
    }
    
    fun sendOrderEventWithPartition(orderEvent: OrderEvent, partition: Int?) {
        try {
            // Send order event to specific partition
            val future = if (partition != null) {
                kafkaTemplate.send(topicName, partition, orderEvent.customerId, orderEvent)
            } else {
                kafkaTemplate.send(topicName, orderEvent.customerId, orderEvent)
            }
            
            future.whenComplete { result, throwable ->
                if (throwable == null) {
                    val metadata = result.recordMetadata
                    logger.info("Order event sent to partition: orderId=${orderEvent.orderId}, " +
                        "requestedPartition=$partition, actualPartition=${metadata.partition()}, " +
                        "offset=${metadata.offset()}")
                } else {
                    logger.error("Failed to send order event to partition: orderId=${orderEvent.orderId}, " +
                        "partition=$partition", throwable)
                }
            }
            
        } catch (e: Exception) {
            logger.error("Exception while sending order event to partition: orderId=${orderEvent.orderId}, " +
                "partition=$partition", e)
            throw RuntimeException("Failed to send order event to partition", e)
        }
    }
    
    fun sendBulkOrderEvents(orderEvents: List<OrderEvent>) {
        if (orderEvents.isEmpty()) {
            logger.warn("Empty order events list provided for bulk send")
            return
        }
        
        try {
            val futures = mutableListOf<CompletableFuture<SendResult<String, OrderEvent>>>()
            
            // Send all events and collect futures
            orderEvents.forEach { orderEvent ->
                val future = kafkaTemplate.send(topicName, orderEvent.customerId, orderEvent)
                futures.add(future)
            }
            
            // Wait for all sends to complete
            val allFutures = CompletableFuture.allOf(*futures.toTypedArray())
            
            allFutures.whenComplete { _, throwable ->
                if (throwable == null) {
                    logger.info("Bulk order events sent successfully: count=${orderEvents.size}")
                    
                    // Log individual results
                    futures.forEachIndexed { index, future ->
                        try {
                            val result = future.get()
                            val metadata = result.recordMetadata
                            val orderEvent = orderEvents[index]
                            logger.debug("Bulk order event ${index + 1}/${orderEvents.size}: " +
                                "orderId=${orderEvent.orderId}, partition=${metadata.partition()}, " +
                                "offset=${metadata.offset()}")
                        } catch (e: Exception) {
                            logger.error("Failed to send bulk order event ${index + 1}/${orderEvents.size}: " +
                                "orderId=${orderEvents[index].orderId}", e)
                        }
                    }
                } else {
                    logger.error("Bulk order events send failed", throwable)
                }
            }
            
        } catch (e: Exception) {
            logger.error("Exception during bulk order events send: count=${orderEvents.size}", e)
            throw RuntimeException("Failed to send bulk order events", e)
        }
    }
    
    fun getTopicName(): String = topicName
}