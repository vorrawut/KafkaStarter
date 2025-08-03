package com.learning.KafkaStarter.service

import com.learning.KafkaStarter.model.UserRegisteredEvent
import org.slf4j.LoggerFactory
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.stereotype.Service

@Service
class UserEventProducer(
    private val kafkaTemplate: KafkaTemplate<String, Any>
) {
    
    private val logger = LoggerFactory.getLogger(UserEventProducer::class.java)
    
    companion object {
        const val USER_REGISTRATION_TOPIC = "user-registration"
    }
    
    fun publishUserRegistered(event: UserRegisteredEvent) {
        logger.info("Publishing user registration event: userId=${event.userId}, email=${event.email}")
        
        kafkaTemplate.send(USER_REGISTRATION_TOPIC, event.userId, event)
            .thenAccept { result ->
                val recordMetadata = result.recordMetadata
                logger.info(
                    "Successfully published user registration event: " +
                    "userId=${event.userId}, topic=${recordMetadata.topic()}, " +
                    "partition=${recordMetadata.partition()}, offset=${recordMetadata.offset()}"
                )
            }
            .exceptionally { failure ->
                logger.error(
                    "Failed to publish user registration event: userId=${event.userId}", 
                    failure
                )
                // In a real application, you might want to:
                // - Store failed events for retry
                // - Send alerts
                // - Update metrics
                null
            }
    }
}