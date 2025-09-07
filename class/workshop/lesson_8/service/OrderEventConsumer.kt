package com.learning.KafkaStarter.service

import com.learning.KafkaStarter.model.OrderEvent
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.kafka.support.KafkaHeaders
import org.springframework.messaging.handler.annotation.Header
import org.springframework.messaging.handler.annotation.Payload
import org.springframework.stereotype.Service

@Service
class OrderEventConsumer {
    
    @KafkaListener(
        topics = ["order-events"],
        groupId = "lesson7-consumer-group-A"
    )
    fun consumeOrderEventsGroupA(
        @Payload orderEvent: OrderEvent,
        @Header(KafkaHeaders.RECEIVED_PARTITION_ID) partition: Int,
        @Header(KafkaHeaders.OFFSET) offset: Long,
        @Header(KafkaHeaders.RECEIVED_TOPIC) topic: String
    ) {
        try {
            // TODO: Process order event in consumer group A
            // HINT: Log the processing details including consumer group, partition, offset
            TODO("Process order event in group A")
        } catch (e: Exception) {
            // TODO: Handle processing exceptions for group A
            TODO("Handle processing exception for group A")
        }
    }
    
    @KafkaListener(
        topics = ["order-events"],
        groupId = "lesson7-consumer-group-B"
    )
    fun consumeOrderEventsGroupB(
        @Payload orderEvent: OrderEvent,
        @Header(KafkaHeaders.RECEIVED_PARTITION_ID) partition: Int,
        @Header(KafkaHeaders.OFFSET) offset: Long
    ) {
        try {
            // TODO: Process order event in consumer group B
            // HINT: Implement different processing logic to demonstrate load balancing
            TODO("Process order event in group B")
        } catch (e: Exception) {
            // TODO: Handle processing exceptions for group B
            TODO("Handle processing exception for group B")
        }
    }
    
    @KafkaListener(
        topics = ["order-events"],
        groupId = "lesson7-consumer-group-C",
        concurrency = "3"
    )
    fun consumeOrderEventsGroupC(
        consumerRecord: ConsumerRecord<String, OrderEvent>
    ) {
        try {
            // TODO: Process order event with multiple consumer instances
            // HINT: Use ConsumerRecord to access all message metadata
            TODO("Process order event with concurrency")
        } catch (e: Exception) {
            // TODO: Handle processing exceptions for concurrent consumers
            TODO("Handle concurrent processing exception")
        }
    }
}