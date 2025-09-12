package com.learning.KafkaStarter.service

import org.slf4j.LoggerFactory
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.stereotype.Service
import java.util.UUID

/**
 * Service for sending messages to Kafka topics
 */
@Service
class ProducerService(
    private val kafkaTemplate: KafkaTemplate<String, String>
) {
    private val logger = LoggerFactory.getLogger(ProducerService::class.java)
    
    companion object {
        const val TOPIC_NAME = "test-topic"
        const val CLOUD_TOPIC_NAME = "hello-topic"
    }
    
    fun sendMessage(message: String) {
        logger.info("Sending message to topic $TOPIC_NAME: $message")
        val key = UUID.randomUUID().toString()
        kafkaTemplate.send(TOPIC_NAME, key, message)
            .whenComplete { result, exception ->
                if (exception != null) {
                    logger.error("Failed to send message: ${exception.message}")
                } else {
                    logger.info("Message sent successfully to ${result?.recordMetadata?.topic()}:${result?.recordMetadata?.partition()}")
                }
            }
    }
}
