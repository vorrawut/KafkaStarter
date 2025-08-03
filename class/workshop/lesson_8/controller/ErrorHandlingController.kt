package com.learning.KafkaStarter.controller

import com.learning.KafkaStarter.model.PaymentEvent
import com.learning.KafkaStarter.service.PaymentEventProducer
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.util.*

@RestController
@RequestMapping("/api/error-handling")
class ErrorHandlingController {
    
    @Autowired
    private lateinit var paymentEventProducer: PaymentEventProducer
    
    @PostMapping("/payments")
    fun sendPaymentEvent(@RequestBody paymentEvent: PaymentEvent): ResponseEntity<Map<String, Any>> {
        return try {
            // TODO: Send payment event
            // HINT: Use paymentEventProducer to send the event
            TODO("Send payment event")
        } catch (e: Exception) {
            // TODO: Handle send error
            TODO("Handle payment event send error")
        }
    }
    
    @PostMapping("/payments/simulate-failure")
    fun simulateFailingPayment(
        @RequestParam amount: Double,
        @RequestParam(defaultValue = "NETWORK_ERROR") failureType: String
    ): ResponseEntity<Map<String, Any>> {
        return try {
            // TODO: Create a payment event that will fail processing
            // HINT: Use specific values that trigger failures in the consumer
            TODO("Create failing payment event")
        } catch (e: Exception) {
            // TODO: Handle simulation error
            TODO("Handle simulation error")
        }
    }
    
    @PostMapping("/payments/bulk-with-failures")
    fun sendBulkPaymentsWithFailures(
        @RequestParam(defaultValue = "10") count: Int,
        @RequestParam(defaultValue = "30") failurePercentage: Int
    ): ResponseEntity<Map<String, Any>> {
        return try {
            // TODO: Send bulk payments with some configured to fail
            // HINT: Create mix of valid and failing payment events
            TODO("Send bulk payments with failures")
        } catch (e: Exception) {
            // TODO: Handle bulk send error
            TODO("Handle bulk send error")
        }
    }
    
    @GetMapping("/dlt-stats")
    fun getDltStats(): ResponseEntity<Map<String, Any>> {
        return try {
            // TODO: Get dead letter topic statistics
            // HINT: Return information about DLT message counts and types
            TODO("Get DLT statistics")
        } catch (e: Exception) {
            // TODO: Handle stats retrieval error
            TODO("Handle stats error")
        }
    }
}