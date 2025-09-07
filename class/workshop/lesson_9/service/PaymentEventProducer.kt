package com.learning.KafkaStarter.service

import com.learning.KafkaStarter.model.PaymentEvent
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.stereotype.Service

@Service
class PaymentEventProducer {
    
    @Autowired
    private lateinit var kafkaTemplate: KafkaTemplate<String, PaymentEvent>
    
    private val topicName = "payment-events"
    
    fun sendPaymentEvent(paymentEvent: PaymentEvent) {
        try {
            // TODO: Send payment event to Kafka topic
            // HINT: Use kafkaTemplate.send() with proper error handling
            TODO("Send payment event to Kafka")
        } catch (e: Exception) {
            // TODO: Handle send exceptions
            TODO("Handle payment event send exception")
        }
    }
    
    fun sendPaymentEventWithRetryCount(paymentEvent: PaymentEvent, retryCount: Int) {
        // TODO: Send payment event with retry count
        // HINT: Update the retry count in the event before sending
        TODO("Send payment event with retry count")
    }
}