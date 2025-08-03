package com.learning.KafkaStarter.service

import com.learning.KafkaStarter.model.UserRegisteredEvent
import org.slf4j.LoggerFactory
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.kafka.support.Acknowledgment
import org.springframework.kafka.support.KafkaHeaders
import org.springframework.messaging.handler.annotation.Header
import org.springframework.messaging.handler.annotation.Payload
import org.springframework.stereotype.Service

@Service
class UserEventConsumer {
    
    private val logger = LoggerFactory.getLogger(UserEventConsumer::class.java)
    
    @KafkaListener(topics = ["user-registration"])
    fun handleUserRegistered(
        @Payload event: UserRegisteredEvent,
        @Header(KafkaHeaders.RECEIVED_TOPIC) topic: String,
        @Header(KafkaHeaders.RECEIVED_PARTITION) partition: Int,
        @Header(KafkaHeaders.OFFSET) offset: Long,
        acknowledgment: Acknowledgment
    ) {
        logger.info(
            "Received user registration event: userId=${event.userId}, " +
            "topic=$topic, partition=$partition, offset=$offset"
        )
        
        try {
            // Simulate business logic processing
            processUserRegistration(event)
            
            // Manually acknowledge the message
            acknowledgment.acknowledge()
            
            logger.info("Successfully processed user registration: userId=${event.userId}")
            
        } catch (e: Exception) {
            logger.error("Failed to process user registration: userId=${event.userId}", e)
            // Don't acknowledge - message will be retried
            throw e
        }
    }
    
    private fun processUserRegistration(event: UserRegisteredEvent) {
        // Simulate some processing time
        Thread.sleep(100)
        
        // Simulate business logic
        when (event.source) {
            "web" -> processWebRegistration(event)
            "mobile" -> processMobileRegistration(event)
            "api" -> processApiRegistration(event)
            else -> processDefaultRegistration(event)
        }
        
        logger.debug("Business logic processing completed for user: ${event.userId}")
    }
    
    private fun processWebRegistration(event: UserRegisteredEvent) {
        logger.info("Processing web registration for user: ${event.userId}")
        // Web-specific logic: send welcome email, set web preferences, etc.
    }
    
    private fun processMobileRegistration(event: UserRegisteredEvent) {
        logger.info("Processing mobile registration for user: ${event.userId}")
        // Mobile-specific logic: send push notification setup, mobile onboarding, etc.
    }
    
    private fun processApiRegistration(event: UserRegisteredEvent) {
        logger.info("Processing API registration for user: ${event.userId}")
        // API-specific logic: generate API keys, set up webhooks, etc.
    }
    
    private fun processDefaultRegistration(event: UserRegisteredEvent) {
        logger.info("Processing default registration for user: ${event.userId}")
        // Default logic: basic account setup, send welcome message, etc.
    }
}