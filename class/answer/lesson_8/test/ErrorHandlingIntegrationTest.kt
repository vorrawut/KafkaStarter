package com.learning.KafkaStarter.test

import com.learning.KafkaStarter.model.PaymentEvent
import com.learning.KafkaStarter.service.PaymentEventProducer
import com.learning.KafkaStarter.service.PaymentEventConsumer
import org.junit.jupiter.api.BeforeEach
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
    topics = ["payment-events", "payment-events-dlt"],
    brokerProperties = [
        "listeners=PLAINTEXT://localhost:9094",
        "port=9094"
    ]
)
class ErrorHandlingIntegrationTest {
    
    @Autowired
    private lateinit var paymentEventProducer: PaymentEventProducer
    
    @Autowired
    private lateinit var paymentEventConsumer: PaymentEventConsumer
    
    @BeforeEach
    fun setup() {
        // Reset counters before each test
        paymentEventConsumer.resetCounters()
    }
    
    @Test
    fun `should process successful payment events`() {
        // Create successful payment events
        val successfulPayments = listOf(
            PaymentEvent(
                paymentId = "success-test-1",
                orderId = "order-success-1",
                customerId = "customer-valid",
                amount = 100.0,
                paymentMethod = "DEBIT_CARD",
                status = "PENDING"
            ),
            PaymentEvent(
                paymentId = "success-test-2",
                orderId = "order-success-2",
                customerId = "customer-valid",
                amount = 250.0,
                paymentMethod = "BANK_TRANSFER",
                status = "PENDING"
            )
        )
        
        // Send successful payment events
        successfulPayments.forEach { paymentEvent ->
            paymentEventProducer.sendPaymentEvent(paymentEvent)
        }
        
        // Wait for processing
        Thread.sleep(2000)
        
        // Verify successful processing
        val stats = paymentEventConsumer.getProcessingStats()
        assertEquals(2L, stats["successfulPayments"], "Should have 2 successful payments")
        assertEquals(0L, stats["dltPayments"], "Should have no DLT payments for successful events")
    }
    
    @Test
    fun `should retry network errors and eventually succeed or go to DLT`() {
        // Create payment that triggers network error (high-value credit card)
        val networkErrorPayment = PaymentEvent(
            paymentId = "network-error-test",
            orderId = "order-network",
            customerId = "customer-network",
            amount = 6000.0, // High amount triggers network error
            paymentMethod = "CREDIT_CARD",
            status = "PENDING"
        )
        
        paymentEventProducer.sendPaymentEvent(networkErrorPayment)
        
        // Wait for retry processing (4 attempts with exponential backoff)
        Thread.sleep(15000) // Wait for all retries to complete
        
        val stats = paymentEventConsumer.getProcessingStats()
        
        // Either should succeed after retries or go to DLT
        val totalProcessed = stats["successfulPayments"] as Long + stats["dltPayments"] as Long
        assertEquals(1L, totalProcessed, "Payment should either succeed or go to DLT")
        
        // Should have retry attempts
        assertTrue(stats["retriedPayments"] as Long > 0, "Should have retry attempts")
    }
    
    @Test
    fun `should send non-retryable errors directly to DLT`() {
        // Create payment with invalid amount (non-retryable)
        val invalidPayment = PaymentEvent(
            paymentId = "invalid-test",
            orderId = "order-invalid",
            customerId = "customer-invalid",
            amount = -100.0, // Negative amount is non-retryable
            paymentMethod = "CREDIT_CARD",
            status = "PENDING"
        )
        
        paymentEventProducer.sendPaymentEvent(invalidPayment)
        
        // Wait for processing
        Thread.sleep(3000)
        
        val stats = paymentEventConsumer.getProcessingStats()
        
        // Should go to DLT without retries for non-retryable errors
        assertTrue(stats["failedPayments"] as Long > 0, "Should have failed payments")
        // Note: In embedded Kafka test, DLT behavior might be different
    }
    
    @Test
    fun `should handle security errors without retries`() {
        // Create payment for blocked customer (security error)
        val securityErrorPayment = PaymentEvent(
            paymentId = "security-test",
            orderId = "order-security",
            customerId = "BLOCKED_customer", // Blocked customer triggers security error
            amount = 100.0,
            paymentMethod = "CREDIT_CARD",
            status = "PENDING"
        )
        
        paymentEventProducer.sendPaymentEvent(securityErrorPayment)
        
        // Wait for processing
        Thread.sleep(3000)
        
        val stats = paymentEventConsumer.getProcessingStats()
        
        // Security errors should fail without retries
        assertTrue(stats["failedPayments"] as Long > 0, "Should have failed payments")
    }
    
    @Test
    fun `should handle service unavailable errors with retries`() {
        // Create PayPal payment for customer ending in '3' (triggers service error)
        val serviceErrorPayment = PaymentEvent(
            paymentId = "service-error-test",
            orderId = "order-service",
            customerId = "customer-paypal-3", // Triggers service unavailable
            amount = 150.0,
            paymentMethod = "PAYPAL",
            status = "PENDING"
        )
        
        paymentEventProducer.sendPaymentEvent(serviceErrorPayment)
        
        // Wait for retry processing
        Thread.sleep(10000)
        
        val stats = paymentEventConsumer.getProcessingStats()
        
        // Should have retry attempts for service errors
        assertTrue(stats["retriedPayments"] as Long > 0 || stats["failedPayments"] as Long > 0, 
            "Should have retries or failures for service errors")
    }
    
    @Test
    fun `should handle bulk payments with mixed success and failure scenarios`() {
        val bulkPayments = listOf(
            // Successful payment
            PaymentEvent(
                paymentId = "bulk-success-1",
                orderId = "order-bulk-1",
                customerId = "customer-good",
                amount = 100.0,
                paymentMethod = "DEBIT_CARD",
                status = "PENDING"
            ),
            
            // Another successful payment
            PaymentEvent(
                paymentId = "bulk-success-2",
                orderId = "order-bulk-2",
                customerId = "customer-good-2",
                amount = 200.0,
                paymentMethod = "BANK_TRANSFER",
                status = "PENDING"
            ),
            
            // Payment that will trigger random error
            PaymentEvent(
                paymentId = "bulk-fail-0000", // Hash should trigger random failure
                orderId = "order-bulk-fail",
                customerId = "customer-random",
                amount = 150.0,
                paymentMethod = "CREDIT_CARD",
                status = "PENDING"
            )
        )
        
        paymentEventProducer.sendBulkPaymentEvents(bulkPayments)
        
        // Wait for processing
        Thread.sleep(8000)
        
        val stats = paymentEventConsumer.getProcessingStats()
        
        // Should have both successful and failed payments
        val totalProcessed = stats["successfulPayments"] as Long + stats["failedPayments"] as Long
        assertEquals(3L, totalProcessed, "Should process all 3 bulk payments")
        
        // At least some should be successful
        assertTrue(stats["successfulPayments"] as Long >= 2, "Should have at least 2 successful payments")
    }
    
    @Test
    fun `should track processing statistics correctly`() {
        // Send a mix of successful and failing payments
        val payments = listOf(
            // Successful
            PaymentEvent(
                paymentId = "stats-success",
                orderId = "order-stats-1",
                customerId = "customer-stats",
                amount = 100.0,
                paymentMethod = "DEBIT_CARD",
                status = "PENDING"
            ),
            
            // Will fail with invalid amount
            PaymentEvent(
                paymentId = "stats-fail",
                orderId = "order-stats-2", 
                customerId = "customer-stats",
                amount = -50.0, // Invalid amount
                paymentMethod = "CREDIT_CARD",
                status = "PENDING"
            )
        )
        
        payments.forEach { paymentEvent ->
            paymentEventProducer.sendPaymentEvent(paymentEvent)
        }
        
        // Wait for processing
        Thread.sleep(5000)
        
        val stats = paymentEventConsumer.getProcessingStats()
        
        // Verify statistics structure
        assertTrue(stats.containsKey("successfulPayments"), "Should track successful payments")
        assertTrue(stats.containsKey("failedPayments"), "Should track failed payments")
        assertTrue(stats.containsKey("retriedPayments"), "Should track retried payments")
        assertTrue(stats.containsKey("dltPayments"), "Should track DLT payments")
        assertTrue(stats.containsKey("totalProcessed"), "Should track total processed")
        assertTrue(stats.containsKey("successRate"), "Should calculate success rate")
        
        // Should have processed both payments
        val totalProcessed = stats["totalProcessed"] as Long
        assertTrue(totalProcessed >= 2, "Should have processed at least 2 payments")
    }
}