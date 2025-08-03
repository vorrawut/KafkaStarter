package com.learning.KafkaStarter.service

import com.learning.KafkaStarter.model.PaymentEvent
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.kafka.support.SendResult
import org.springframework.stereotype.Service
import java.util.concurrent.CompletableFuture

@Service
class PaymentEventProducer {
    
    @Autowired
    private lateinit var kafkaTemplate: KafkaTemplate<String, PaymentEvent>
    
    private val topicName = "payment-events"
    private val logger = org.slf4j.LoggerFactory.getLogger(PaymentEventProducer::class.java)
    
    fun sendPaymentEvent(paymentEvent: PaymentEvent) {
        try {
            // Send payment event to Kafka topic
            val future: CompletableFuture<SendResult<String, PaymentEvent>> = kafkaTemplate.send(
                topicName,
                paymentEvent.paymentId, // Use payment ID as key for consistent partitioning
                paymentEvent
            )
            
            future.whenComplete { result, throwable ->
                if (throwable == null) {
                    val metadata = result.recordMetadata
                    logger.info("Payment event sent successfully: paymentId=${paymentEvent.paymentId}, " +
                        "amount=${paymentEvent.amount}, partition=${metadata.partition()}, " +
                        "offset=${metadata.offset()}")
                } else {
                    logger.error("Failed to send payment event: paymentId=${paymentEvent.paymentId}", throwable)
                    
                    // In a real application, you might:
                    // - Store failed events for retry
                    // - Send to a backup topic
                    // - Alert monitoring systems
                    throw RuntimeException("Failed to send payment event", throwable)
                }
            }
            
        } catch (e: Exception) {
            logger.error("Exception while sending payment event: paymentId=${paymentEvent.paymentId}", e)
            throw RuntimeException("Failed to send payment event", e)
        }
    }
    
    fun sendPaymentEventWithRetryCount(paymentEvent: PaymentEvent, retryCount: Int) {
        try {
            // Update the retry count in the event before sending
            val updatedPaymentEvent = paymentEvent.copy(
                retryCount = retryCount,
                metadata = paymentEvent.metadata + mapOf(
                    "manualRetry" to true,
                    "retryTimestamp" to System.currentTimeMillis()
                )
            )
            
            val future = kafkaTemplate.send(topicName, updatedPaymentEvent.paymentId, updatedPaymentEvent)
            
            future.whenComplete { result, throwable ->
                if (throwable == null) {
                    val metadata = result.recordMetadata
                    logger.info("Payment event sent with retry count: paymentId=${updatedPaymentEvent.paymentId}, " +
                        "retryCount=$retryCount, partition=${metadata.partition()}, offset=${metadata.offset()}")
                } else {
                    logger.error("Failed to send payment event with retry count: " +
                        "paymentId=${updatedPaymentEvent.paymentId}, retryCount=$retryCount", throwable)
                    throw RuntimeException("Failed to send payment event with retry count", throwable)
                }
            }
            
        } catch (e: Exception) {
            logger.error("Exception while sending payment event with retry count: " +
                "paymentId=${paymentEvent.paymentId}, retryCount=$retryCount", e)
            throw RuntimeException("Failed to send payment event with retry count", e)
        }
    }
    
    fun sendBulkPaymentEvents(paymentEvents: List<PaymentEvent>) {
        if (paymentEvents.isEmpty()) {
            logger.warn("Empty payment events list provided for bulk send")
            return
        }
        
        try {
            val futures = mutableListOf<CompletableFuture<SendResult<String, PaymentEvent>>>()
            
            // Send all events and collect futures
            paymentEvents.forEach { paymentEvent ->
                val future = kafkaTemplate.send(topicName, paymentEvent.paymentId, paymentEvent)
                futures.add(future)
            }
            
            // Wait for all sends to complete
            val allFutures = CompletableFuture.allOf(*futures.toTypedArray())
            
            allFutures.whenComplete { _, throwable ->
                if (throwable == null) {
                    logger.info("Bulk payment events sent successfully: count=${paymentEvents.size}")
                    
                    // Log individual results
                    futures.forEachIndexed { index, future ->
                        try {
                            val result = future.get()
                            val metadata = result.recordMetadata
                            val paymentEvent = paymentEvents[index]
                            logger.debug("Bulk payment event ${index + 1}/${paymentEvents.size}: " +
                                "paymentId=${paymentEvent.paymentId}, partition=${metadata.partition()}, " +
                                "offset=${metadata.offset()}")
                        } catch (e: Exception) {
                            logger.error("Failed to send bulk payment event ${index + 1}/${paymentEvents.size}: " +
                                "paymentId=${paymentEvents[index].paymentId}", e)
                        }
                    }
                } else {
                    logger.error("Bulk payment events send failed", throwable)
                }
            }
            
        } catch (e: Exception) {
            logger.error("Exception during bulk payment events send: count=${paymentEvents.size}", e)
            throw RuntimeException("Failed to send bulk payment events", e)
        }
    }
    
    fun getTopicName(): String = topicName
}