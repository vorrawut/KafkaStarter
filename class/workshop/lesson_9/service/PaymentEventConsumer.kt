package com.learning.KafkaStarter.service

import com.learning.KafkaStarter.model.PaymentEvent
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.kafka.annotation.RetryableTopic
import org.springframework.kafka.support.KafkaHeaders
import org.springframework.messaging.handler.annotation.Header
import org.springframework.messaging.handler.annotation.Payload
import org.springframework.retry.annotation.Backoff
import org.springframework.stereotype.Service

@Service
class PaymentEventConsumer {
    
    @RetryableTopic(
        attempts = "4",
        backoff = Backoff(delay = 1000, multiplier = 2.0),
        dltStrategy = TODO("Configure DLT strategy"),
        include = [TODO("Configure retryable exceptions")]
    )
    @KafkaListener(topics = ["payment-events"])
    fun processPaymentEvent(
        @Payload paymentEvent: PaymentEvent,
        @Header(KafkaHeaders.RECEIVED_TOPIC) topic: String,
        @Header(KafkaHeaders.RECEIVED_PARTITION_ID) partition: Int,
        @Header(KafkaHeaders.OFFSET) offset: Long
    ) {
        try {
            // TODO: Implement payment processing logic
            // HINT: Process payment and simulate various failure scenarios
            TODO("Process payment event")
        } catch (e: Exception) {
            // TODO: Handle processing exceptions
            // HINT: Log the error and let retry mechanism handle it
            TODO("Handle payment processing exception")
        }
    }
    
    @KafkaListener(topics = ["payment-events-dlt"])
    fun handlePaymentEventDlt(
        consumerRecord: ConsumerRecord<String, PaymentEvent>
    ) {
        // TODO: Handle dead letter topic messages
        // HINT: Log the failed message and potentially alert support team
        TODO("Handle dead letter topic message")
    }
    
    fun simulatePaymentProcessing(paymentEvent: PaymentEvent) {
        // TODO: Simulate different payment processing scenarios
        // HINT: Implement logic that fails for certain conditions to test retry
        TODO("Simulate payment processing")
    }
}