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
        TODO("Not yet implemented")
    }
}
