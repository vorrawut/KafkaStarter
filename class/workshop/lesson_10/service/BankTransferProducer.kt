package com.learning.KafkaStarter.service

import com.learning.KafkaStarter.model.BankTransferEvent
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.stereotype.Service

@Service
class BankTransferProducer {
    
    @Autowired
    private lateinit var kafkaTemplate: KafkaTemplate<String, BankTransferEvent>
    
    fun sendBankTransfer(bankTransferEvent: BankTransferEvent, topicName: String = "bank-transfers") {
        try {
            // TODO: Send bank transfer event with idempotent producer
            // HINT: Use transfer ID as key for consistent partitioning
            TODO("Send bank transfer event")
        } catch (e: Exception) {
            // TODO: Handle send exceptions
            TODO("Handle bank transfer send exception")
        }
    }
    
    fun sendIdempotentBankTransfer(bankTransferEvent: BankTransferEvent) {
        try {
            // TODO: Send bank transfer to idempotent processing topic
            // HINT: Use idempotency key for deduplication
            TODO("Send idempotent bank transfer")
        } catch (e: Exception) {
            // TODO: Handle idempotent send exceptions  
            TODO("Handle idempotent send exception")
        }
    }
    
    fun sendDuplicateTransfer(bankTransferEvent: BankTransferEvent, count: Int = 3) {
        // TODO: Send duplicate transfers for testing idempotency
        // HINT: Send the same transfer multiple times with same idempotency key
        TODO("Send duplicate transfers for testing")
    }
}