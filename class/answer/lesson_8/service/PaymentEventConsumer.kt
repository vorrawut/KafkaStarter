package com.learning.KafkaStarter.service

import com.learning.KafkaStarter.model.PaymentEvent
import com.learning.KafkaStarter.model.PaymentFailure
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.springframework.kafka.annotation.DltHandler
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.kafka.annotation.RetryableTopic
import org.springframework.kafka.retrytopic.DltStrategy
import org.springframework.kafka.support.KafkaHeaders
import org.springframework.messaging.handler.annotation.Header
import org.springframework.messaging.handler.annotation.Payload
import org.springframework.retry.annotation.Backoff
import org.springframework.stereotype.Service
import java.util.concurrent.atomic.AtomicLong

@Service
class PaymentEventConsumer {
    
    private val logger = org.slf4j.LoggerFactory.getLogger(PaymentEventConsumer::class.java)
    
    // Counters for tracking processing statistics
    private val successfulPayments = AtomicLong(0)
    private val failedPayments = AtomicLong(0)
    private val retriedPayments = AtomicLong(0)
    private val dltPayments = AtomicLong(0)
    
    @RetryableTopic(
        attempts = "4",
        backoff = Backoff(delay = 1000, multiplier = 2.0, maxDelay = 10000),
        dltStrategy = DltStrategy.FAIL_ON_ERROR,
        include = [
            PaymentProcessingException::class,
            NetworkException::class,
            TemporaryServiceException::class
        ],
        exclude = [
            InvalidPaymentException::class, // Non-retryable exceptions
            SecurityException::class
        ]
    )
    @KafkaListener(topics = ["payment-events"])
    fun processPaymentEvent(
        @Payload paymentEvent: PaymentEvent,
        @Header(KafkaHeaders.RECEIVED_TOPIC) topic: String,
        @Header(KafkaHeaders.RECEIVED_PARTITION_ID) partition: Int,
        @Header(KafkaHeaders.OFFSET) offset: Long,
        @Header(value = KafkaHeaders.RETRY_TOPIC_ORIGINAL_TIMESTAMP, required = false) originalTimestamp: Long?
    ) {
        try {
            logger.info("Processing payment: paymentId=${paymentEvent.paymentId}, " +
                "amount=${paymentEvent.amount}, method=${paymentEvent.paymentMethod}, " +
                "topic=$topic, partition=$partition, offset=$offset, " +
                "originalTimestamp=$originalTimestamp")
            
            // If this is a retry, increment retry counter
            if (originalTimestamp != null) {
                retriedPayments.incrementAndGet()
                logger.info("Retrying payment: paymentId=${paymentEvent.paymentId}, " +
                    "retryCount=${paymentEvent.retryCount}")
            }
            
            // Simulate payment processing with various failure scenarios
            simulatePaymentProcessing(paymentEvent)
            
            // If we reach here, payment was successful
            successfulPayments.incrementAndGet()
            logger.info("Payment processed successfully: paymentId=${paymentEvent.paymentId}")
            
        } catch (e: Exception) {
            failedPayments.incrementAndGet()
            logger.error("Payment processing failed: paymentId=${paymentEvent.paymentId}, " +
                "error=${e.message}", e)
            
            // Re-throw to trigger retry mechanism
            throw e
        }
    }
    
    @DltHandler
    fun handlePaymentEventDlt(
        consumerRecord: ConsumerRecord<String, PaymentEvent>,
        @Header(KafkaHeaders.RECEIVED_TOPIC) topic: String,
        @Header(KafkaHeaders.EXCEPTION_MESSAGE) exceptionMessage: String,
        @Header(KafkaHeaders.EXCEPTION_STACKTRACE) stackTrace: String
    ) {
        val paymentEvent = consumerRecord.value()
        dltPayments.incrementAndGet()
        
        logger.error("Payment sent to DLT: paymentId=${paymentEvent.paymentId}, " +
            "topic=$topic, partition=${consumerRecord.partition()}, " +
            "offset=${consumerRecord.offset()}, exception=$exceptionMessage")
        
        // Create payment failure record
        val paymentFailure = PaymentFailure(
            paymentId = paymentEvent.paymentId,
            originalEvent = paymentEvent,
            errorType = extractErrorType(exceptionMessage),
            errorMessage = exceptionMessage,
            failureCount = paymentEvent.retryCount + 1,
            stackTrace = stackTrace
        )
        
        // In a real application, you would:
        // 1. Store the failure in a database for analysis
        // 2. Send alerts to support team
        // 3. Potentially create manual review tasks
        // 4. Log to monitoring systems
        
        logger.info("Created payment failure record: $paymentFailure")
        
        // Simulate alerting system
        if (paymentEvent.amount > 1000.0) {
            logger.warn("HIGH-VALUE PAYMENT FAILED - ALERT SUPPORT TEAM: " +
                "paymentId=${paymentEvent.paymentId}, amount=${paymentEvent.amount}")
        }
    }
    
    fun simulatePaymentProcessing(paymentEvent: PaymentEvent) {
        // Simulate different payment processing scenarios based on payment details
        
        when {
            // Simulate network errors (retryable)
            paymentEvent.paymentMethod == "CREDIT_CARD" && paymentEvent.amount > 5000.0 -> {
                throw NetworkException("Payment gateway network timeout for high-value transaction")
            }
            
            // Simulate temporary service unavailability (retryable)
            paymentEvent.paymentMethod == "PAYPAL" && paymentEvent.customerId.endsWith("3") -> {
                throw TemporaryServiceException("PayPal service temporarily unavailable")
            }
            
            // Simulate payment processing errors (retryable)
            paymentEvent.amount < 0 -> {
                throw InvalidPaymentException("Invalid payment amount: ${paymentEvent.amount}")
            }
            
            // Simulate security errors (non-retryable)
            paymentEvent.customerId.startsWith("BLOCKED_") -> {
                throw SecurityException("Customer is blocked: ${paymentEvent.customerId}")
            }
            
            // Simulate random processing failures (retryable)
            paymentEvent.paymentId.hashCode() % 10 == 0 -> {
                throw PaymentProcessingException("Random processing failure for testing")
            }
            
            // Simulate successful processing for most cases
            else -> {
                // Simulate processing time
                Thread.sleep(50 + (paymentEvent.amount.toLong() % 100))
                
                logger.debug("Payment processed: paymentId=${paymentEvent.paymentId}, " +
                    "amount=${paymentEvent.amount}, method=${paymentEvent.paymentMethod}")
            }
        }
    }
    
    private fun extractErrorType(exceptionMessage: String): String {
        return when {
            exceptionMessage.contains("NetworkException") -> "NETWORK_ERROR"
            exceptionMessage.contains("TemporaryServiceException") -> "SERVICE_UNAVAILABLE"
            exceptionMessage.contains("PaymentProcessingException") -> "PROCESSING_ERROR"
            exceptionMessage.contains("InvalidPaymentException") -> "INVALID_PAYMENT"
            exceptionMessage.contains("SecurityException") -> "SECURITY_ERROR"
            else -> "UNKNOWN_ERROR"
        }
    }
    
    // Statistics methods
    fun getProcessingStats(): Map<String, Any> {
        return mapOf(
            "successfulPayments" to successfulPayments.get(),
            "failedPayments" to failedPayments.get(),
            "retriedPayments" to retriedPayments.get(),
            "dltPayments" to dltPayments.get(),
            "totalProcessed" to (successfulPayments.get() + failedPayments.get()),
            "successRate" to if (successfulPayments.get() + failedPayments.get() > 0) {
                (successfulPayments.get().toDouble() / (successfulPayments.get() + failedPayments.get())) * 100
            } else 0.0
        )
    }
    
    fun resetCounters() {
        successfulPayments.set(0)
        failedPayments.set(0)
        retriedPayments.set(0)
        dltPayments.set(0)
        logger.info("Payment processing counters reset")
    }
}

// Custom exception classes for different error scenarios
class PaymentProcessingException(message: String) : RuntimeException(message)
class NetworkException(message: String) : RuntimeException(message)
class TemporaryServiceException(message: String) : RuntimeException(message)
class InvalidPaymentException(message: String) : RuntimeException(message)
class SecurityException(message: String) : RuntimeException(message)