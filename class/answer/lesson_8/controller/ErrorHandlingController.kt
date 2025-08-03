package com.learning.KafkaStarter.controller

import com.learning.KafkaStarter.model.PaymentEvent
import com.learning.KafkaStarter.service.PaymentEventProducer
import com.learning.KafkaStarter.service.PaymentEventConsumer
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.time.Instant
import java.util.*
import kotlin.random.Random

@RestController
@RequestMapping("/api/error-handling")
class ErrorHandlingController {
    
    @Autowired
    private lateinit var paymentEventProducer: PaymentEventProducer
    
    @Autowired
    private lateinit var paymentEventConsumer: PaymentEventConsumer
    
    private val logger = org.slf4j.LoggerFactory.getLogger(ErrorHandlingController::class.java)
    
    @PostMapping("/payments")
    fun sendPaymentEvent(@RequestBody paymentEvent: PaymentEvent): ResponseEntity<Map<String, Any>> {
        return try {
            // Send payment event
            paymentEventProducer.sendPaymentEvent(paymentEvent)
            
            logger.info("Payment event sent: paymentId=${paymentEvent.paymentId}, " +
                "amount=${paymentEvent.amount}, method=${paymentEvent.paymentMethod}")
            
            ResponseEntity.ok(
                mapOf(
                    "success" to true,
                    "message" to "Payment event sent successfully",
                    "paymentId" to paymentEvent.paymentId,
                    "amount" to paymentEvent.amount,
                    "paymentMethod" to paymentEvent.paymentMethod,
                    "topic" to paymentEventProducer.getTopicName(),
                    "timestamp" to System.currentTimeMillis()
                )
            )
        } catch (e: Exception) {
            logger.error("Failed to send payment event: paymentId=${paymentEvent.paymentId}", e)
            
            ResponseEntity.internalServerError().body(
                mapOf(
                    "success" to false,
                    "error" to (e.message ?: "Unknown error"),
                    "paymentId" to paymentEvent.paymentId,
                    "timestamp" to System.currentTimeMillis()
                )
            )
        }
    }
    
    @PostMapping("/payments/simulate-failure")
    fun simulateFailingPayment(
        @RequestParam amount: Double,
        @RequestParam(defaultValue = "NETWORK_ERROR") failureType: String
    ): ResponseEntity<Map<String, Any>> {
        return try {
            // Create a payment event that will fail processing based on failure type
            val paymentEvent = when (failureType) {
                "NETWORK_ERROR" -> PaymentEvent(
                    paymentId = "fail-network-${UUID.randomUUID()}",
                    orderId = "order-${UUID.randomUUID()}",
                    customerId = "customer-network",
                    amount = if (amount > 0) amount else 6000.0, // Triggers network error for high amounts
                    paymentMethod = "CREDIT_CARD",
                    status = "PENDING"
                )
                
                "SERVICE_UNAVAILABLE" -> PaymentEvent(
                    paymentId = "fail-service-${UUID.randomUUID()}",
                    orderId = "order-${UUID.randomUUID()}",
                    customerId = "customer-paypal-3", // Triggers service error for customers ending in 3
                    amount = if (amount > 0) amount else 100.0,
                    paymentMethod = "PAYPAL",
                    status = "PENDING"
                )
                
                "INVALID_PAYMENT" -> PaymentEvent(
                    paymentId = "fail-invalid-${UUID.randomUUID()}",
                    orderId = "order-${UUID.randomUUID()}",
                    customerId = "customer-invalid",
                    amount = -100.0, // Negative amount triggers invalid payment error
                    paymentMethod = "BANK_TRANSFER",
                    status = "PENDING"
                )
                
                "SECURITY_ERROR" -> PaymentEvent(
                    paymentId = "fail-security-${UUID.randomUUID()}",
                    orderId = "order-${UUID.randomUUID()}",
                    customerId = "BLOCKED_customer", // Blocked customer triggers security error
                    amount = if (amount > 0) amount else 100.0,
                    paymentMethod = "CREDIT_CARD",
                    status = "PENDING"
                )
                
                "RANDOM_ERROR" -> {
                    // Create payment with hash that triggers random failure
                    var paymentId: String
                    do {
                        paymentId = "fail-random-${Random.nextInt(1000, 9999)}"
                    } while (paymentId.hashCode() % 10 != 0) // Ensure it triggers the random failure
                    
                    PaymentEvent(
                        paymentId = paymentId,
                        orderId = "order-${UUID.randomUUID()}",
                        customerId = "customer-random",
                        amount = if (amount > 0) amount else 150.0,
                        paymentMethod = "DEBIT_CARD",
                        status = "PENDING"
                    )
                }
                
                else -> PaymentEvent(
                    paymentId = "fail-unknown-${UUID.randomUUID()}",
                    orderId = "order-${UUID.randomUUID()}",
                    customerId = "customer-unknown",
                    amount = 6000.0, // Default to network error scenario
                    paymentMethod = "CREDIT_CARD",
                    status = "PENDING"
                )
            }
            
            paymentEventProducer.sendPaymentEvent(paymentEvent)
            
            logger.info("Failing payment event sent: paymentId=${paymentEvent.paymentId}, " +
                "failureType=$failureType")
            
            ResponseEntity.ok(
                mapOf(
                    "success" to true,
                    "message" to "Failing payment event sent",
                    "paymentId" to paymentEvent.paymentId,
                    "failureType" to failureType,
                    "expectedFailure" to getExpectedFailureDescription(failureType),
                    "timestamp" to System.currentTimeMillis()
                )
            )
        } catch (e: Exception) {
            logger.error("Failed to send failing payment event: failureType=$failureType", e)
            
            ResponseEntity.internalServerError().body(
                mapOf(
                    "success" to false,
                    "error" to (e.message ?: "Unknown error"),
                    "failureType" to failureType,
                    "timestamp" to System.currentTimeMillis()
                )
            )
        }
    }
    
    @PostMapping("/payments/bulk-with-failures")
    fun sendBulkPaymentsWithFailures(
        @RequestParam(defaultValue = "10") count: Int,
        @RequestParam(defaultValue = "30") failurePercentage: Int
    ): ResponseEntity<Map<String, Any>> {
        return try {
            // Send bulk payments with some configured to fail
            val failureCount = (count * failurePercentage) / 100
            val successCount = count - failureCount
            
            val paymentEvents = mutableListOf<PaymentEvent>()
            
            // Create successful payments
            repeat(successCount) { index ->
                paymentEvents.add(
                    PaymentEvent(
                        paymentId = "bulk-success-${index}-${UUID.randomUUID()}",
                        orderId = "order-${UUID.randomUUID()}",
                        customerId = "customer-success-$index",
                        amount = Random.nextDouble(10.0, 500.0),
                        paymentMethod = listOf("CREDIT_CARD", "DEBIT_CARD", "BANK_TRANSFER").random(),
                        status = "PENDING",
                        metadata = mapOf("bulk" to true, "expectedSuccess" to true)
                    )
                )
            }
            
            // Create failing payments
            val failureTypes = listOf("NETWORK_ERROR", "SERVICE_UNAVAILABLE", "RANDOM_ERROR")
            repeat(failureCount) { index ->
                val failureType = failureTypes[index % failureTypes.size]
                
                val paymentEvent = when (failureType) {
                    "NETWORK_ERROR" -> PaymentEvent(
                        paymentId = "bulk-fail-network-$index-${UUID.randomUUID()}",
                        orderId = "order-${UUID.randomUUID()}",
                        customerId = "customer-bulk-$index",
                        amount = 6000.0 + index, // High amount triggers network error
                        paymentMethod = "CREDIT_CARD",
                        status = "PENDING",
                        metadata = mapOf("bulk" to true, "expectedFailure" to failureType)
                    )
                    
                    "SERVICE_UNAVAILABLE" -> PaymentEvent(
                        paymentId = "bulk-fail-service-$index-${UUID.randomUUID()}",
                        orderId = "order-${UUID.randomUUID()}",
                        customerId = "customer-paypal-3", // Triggers service error
                        amount = Random.nextDouble(100.0, 500.0),
                        paymentMethod = "PAYPAL",
                        status = "PENDING",
                        metadata = mapOf("bulk" to true, "expectedFailure" to failureType)
                    )
                    
                    else -> { // RANDOM_ERROR
                        var paymentId: String
                        do {
                            paymentId = "bulk-fail-random-$index-${Random.nextInt(100, 999)}"
                        } while (paymentId.hashCode() % 10 != 0)
                        
                        PaymentEvent(
                            paymentId = paymentId,
                            orderId = "order-${UUID.randomUUID()}",
                            customerId = "customer-bulk-random-$index",
                            amount = Random.nextDouble(100.0, 500.0),
                            paymentMethod = "DEBIT_CARD",
                            status = "PENDING",
                            metadata = mapOf("bulk" to true, "expectedFailure" to failureType)
                        )
                    }
                }
                
                paymentEvents.add(paymentEvent)
            }
            
            // Shuffle the events to mix successes and failures
            paymentEvents.shuffle()
            
            paymentEventProducer.sendBulkPaymentEvents(paymentEvents)
            
            logger.info("Bulk payments with failures sent: total=$count, " +
                "success=$successCount, failures=$failureCount")
            
            ResponseEntity.ok(
                mapOf(
                    "success" to true,
                    "message" to "Bulk payments with failures sent",
                    "totalCount" to count,
                    "successCount" to successCount,
                    "failureCount" to failureCount,
                    "failurePercentage" to failurePercentage,
                    "paymentIds" to paymentEvents.map { it.paymentId },
                    "timestamp" to System.currentTimeMillis()
                )
            )
        } catch (e: Exception) {
            logger.error("Failed to send bulk payments with failures: count=$count", e)
            
            ResponseEntity.internalServerError().body(
                mapOf(
                    "success" to false,
                    "error" to (e.message ?: "Unknown error"),
                    "requestedCount" to count,
                    "failurePercentage" to failurePercentage,
                    "timestamp" to System.currentTimeMillis()
                )
            )
        }
    }
    
    @GetMapping("/dlt-stats")
    fun getDltStats(): ResponseEntity<Map<String, Any>> {
        return try {
            // Get dead letter topic statistics
            val processingStats = paymentEventConsumer.getProcessingStats()
            
            ResponseEntity.ok(
                mapOf(
                    "success" to true,
                    "statistics" to processingStats,
                    "dltTopicName" to "payment-events-dlt",
                    "retryConfiguration" to mapOf(
                        "maxAttempts" to 4,
                        "baseDelay" to "1000ms",
                        "multiplier" to 2.0,
                        "maxDelay" to "10000ms"
                    ),
                    "retryableExceptions" to listOf(
                        "PaymentProcessingException",
                        "NetworkException",
                        "TemporaryServiceException"
                    ),
                    "nonRetryableExceptions" to listOf(
                        "InvalidPaymentException",
                        "SecurityException"
                    ),
                    "timestamp" to System.currentTimeMillis()
                )
            )
        } catch (e: Exception) {
            logger.error("Failed to get DLT statistics", e)
            
            ResponseEntity.internalServerError().body(
                mapOf(
                    "success" to false,
                    "error" to (e.message ?: "Unknown error"),
                    "timestamp" to System.currentTimeMillis()
                )
            )
        }
    }
    
    @PostMapping("/reset-stats")
    fun resetProcessingStats(): ResponseEntity<Map<String, Any>> {
        return try {
            paymentEventConsumer.resetCounters()
            
            ResponseEntity.ok(
                mapOf(
                    "success" to true,
                    "message" to "Processing statistics reset",
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
    
    private fun getExpectedFailureDescription(failureType: String): String {
        return when (failureType) {
            "NETWORK_ERROR" -> "High-value credit card payment will trigger network timeout (retryable)"
            "SERVICE_UNAVAILABLE" -> "PayPal payment for customer ending in '3' will trigger service unavailable (retryable)"
            "INVALID_PAYMENT" -> "Negative amount will trigger invalid payment error (non-retryable)"
            "SECURITY_ERROR" -> "Blocked customer will trigger security error (non-retryable)"
            "RANDOM_ERROR" -> "Payment ID hash will trigger random processing failure (retryable)"
            else -> "Unknown failure type - defaults to network error scenario"
        }
    }
}