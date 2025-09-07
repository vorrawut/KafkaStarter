package com.learning.KafkaStarter.service

import com.learning.KafkaStarter.model.RawCustomerEvent
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.kafka.support.KafkaHeaders
import org.springframework.messaging.handler.annotation.Header
import org.springframework.messaging.handler.annotation.Payload
import org.springframework.stereotype.Service

@Service
class TransformationConsumer {
    
    @Autowired
    private lateinit var messageTransformationService: MessageTransformationService
    
    @Autowired
    private lateinit var messageFilterService: MessageFilterService
    
    @Autowired
    private lateinit var transformationProducer: TransformationProducer
    
    @KafkaListener(topics = ["raw-customer-events"])
    fun processRawCustomerEvent(
        @Payload rawEvent: RawCustomerEvent,
        @Header(KafkaHeaders.RECEIVED_PARTITION_ID) partition: Int,
        @Header(KafkaHeaders.OFFSET) offset: Long
    ) {
        try {
            // TODO: Process raw customer event with filtering and transformation
            // HINT: 1. Filter event, 2. Transform if passes filter, 3. Send to appropriate topic
            TODO("Process raw customer event")
        } catch (e: Exception) {
            // TODO: Handle processing exceptions
            TODO("Handle raw event processing exception")
        }
    }
    
    @KafkaListener(topics = ["high-priority-events"])
    fun processHighPriorityEvent(
        @Payload rawEvent: RawCustomerEvent,
        @Header(KafkaHeaders.RECEIVED_TOPIC) topic: String
    ) {
        try {
            // TODO: Process high priority events with special handling
            // HINT: Apply different transformation rules for high priority events
            TODO("Process high priority event")
        } catch (e: Exception) {
            // TODO: Handle high priority processing exceptions
            TODO("Handle high priority processing exception")
        }
    }
    
    @KafkaListener(topics = ["batch-events"])
    fun processBatchEvent(
        @Payload rawEvent: RawCustomerEvent
    ) {
        try {
            // TODO: Process batch events with bulk transformation
            // HINT: Collect events and process in batches for efficiency
            TODO("Process batch event")
        } catch (e: Exception) {
            // TODO: Handle batch processing exceptions
            TODO("Handle batch processing exception")
        }
    }
}