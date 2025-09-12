package com.learning.KafkaStarter.service

import org.apache.logging.log4j.message.SimpleMessage
import org.slf4j.LoggerFactory
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.kafka.support.Acknowledgment
import org.springframework.kafka.support.KafkaHeaders
import org.springframework.messaging.handler.annotation.Header
import org.springframework.messaging.handler.annotation.Payload
import org.springframework.stereotype.Service

/**
 * Service for consuming messages from Kafka topics
 */
@Service
class MessageConsumerService {
    private val logger = LoggerFactory.getLogger(MessageConsumerService::class.java)
    
//    @KafkaListener(topics = ["simple-messages"], groupId = "kafka-starter-main")
//    fun consumeMessage(
//        @Payload message: SimpleMessage,
//        @Header(KafkaHeaders.RECEIVED_TOPIC) topic: String,
//        @Header(KafkaHeaders.RECEIVED_PARTITION) partition: Int,
//        @Header(KafkaHeaders.OFFSET) offset: Long,
//        acknowledgment: Acknowledgment
//    ) {
//        try {
//            logger.info("Received message from topic: $topic, partition: $partition, offset: $offset")
//            logger.info("Message content: $message")
//
//            // Process the message here
//            processMessage(message)
//
//            // Manually acknowledge the message after successful processing
//            acknowledgment.acknowledge()
//            logger.debug("Message acknowledged successfully")
//
//        } catch (e: Exception) {
//            logger.error("Error processing message: $message", e)
//            // Don't acknowledge on error - this will trigger retry mechanism
//            throw e
//        }
//    }
//
//    private fun processMessage(message: SimpleMessage) {
//        // Simulate some processing time
//        Thread.sleep(100)
//
//        // Simple processing logic - just log the message
//        logger.info("Processing message with ID: ${message.id}, Content: ${message.content}, Timestamp: ${message.timestamp}")
//
//        // Add your business logic here
//        // For example: save to database, call other services, etc.
//
//        // Simulate occasional failures for testing error handling
//        if (message.content.contains("error", ignoreCase = true)) {
//            throw RuntimeException("Simulated processing error for message: ${message.id}")
//        }
//    }
}
