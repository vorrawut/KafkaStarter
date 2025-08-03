package com.learning.KafkaStarter.service

import com.learning.KafkaStarter.avro.UserCreatedEvent
import org.apache.avro.generic.GenericRecord
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.slf4j.LoggerFactory
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.kafka.support.Acknowledgment
import org.springframework.kafka.support.KafkaHeaders
import org.springframework.messaging.handler.annotation.Header
import org.springframework.messaging.handler.annotation.Payload
import org.springframework.stereotype.Service

@Service
class SchemaAwareConsumer {
    
    private val logger = LoggerFactory.getLogger(SchemaAwareConsumer::class.java)
    
    @KafkaListener(
        topics = ["user-events-avro"],
        containerFactory = "avroKafkaListenerContainerFactory"
    )
    fun consumeAvroMessage(
        @Payload event: UserCreatedEvent,
        @Header(KafkaHeaders.RECEIVED_TOPIC) topic: String,
        @Header(KafkaHeaders.RECEIVED_PARTITION) partition: Int,
        @Header(KafkaHeaders.OFFSET) offset: Long,
        @Header(KafkaHeaders.RECEIVED_MESSAGE_KEY) key: String,
        acknowledgment: Acknowledgment
    ) {
        try {
            logger.info(
                "Received Avro message: " +
                "userId=${event.userId}, " +
                "email=${event.email}, " +
                "timestamp=${event.timestamp}, " +
                "method=${event.registrationMethod} " +
                "from topic=$topic, partition=$partition, offset=$offset"
            )
            
            // Process the Avro event
            processUserCreatedEvent(event)
            
            // Acknowledge successful processing
            acknowledgment.acknowledge()
            
        } catch (e: Exception) {
            logger.error("Error processing Avro message with key $key", e)
            // Don't acknowledge - message will be retried
        }
    }
    
    @KafkaListener(
        topics = ["user-events-generic"],
        containerFactory = "avroKafkaListenerContainerFactory"
    )
    fun consumeGenericAvroMessage(
        @Payload genericRecord: GenericRecord,
        @Header(KafkaHeaders.RECEIVED_TOPIC) topic: String,
        @Header(KafkaHeaders.RECEIVED_PARTITION) partition: Int,
        @Header(KafkaHeaders.OFFSET) offset: Long,
        acknowledgment: Acknowledgment
    ) {
        try {
            logger.info(
                "Received Generic Avro message: " +
                "userId=${genericRecord.get("userId")}, " +
                "email=${genericRecord.get("email")}, " +
                "timestamp=${genericRecord.get("timestamp")}, " +
                "method=${genericRecord.get("registrationMethod")} " +
                "from topic=$topic, partition=$partition, offset=$offset"
            )
            
            // Process the generic record
            processGenericUserEvent(genericRecord)
            
            acknowledgment.acknowledge()
            
        } catch (e: Exception) {
            logger.error("Error processing Generic Avro message", e)
        }
    }
    
    @KafkaListener(topics = ["user-events-protobuf"])
    fun consumeProtobufMessage(
        record: ConsumerRecord<String, Any>,
        acknowledgment: Acknowledgment
    ) {
        try {
            val event = record.value()
            
            logger.info(
                "Received Protobuf message: $event " +
                "from topic=${record.topic()}, partition=${record.partition()}, offset=${record.offset()}"
            )
            
            // Process the protobuf event
            processProtobufEvent(event)
            
            acknowledgment.acknowledge()
            
        } catch (e: Exception) {
            logger.error("Error processing Protobuf message", e)
        }
    }
    
    private fun processUserCreatedEvent(event: UserCreatedEvent) {
        // Simulate business logic processing
        logger.debug("Processing user creation for: ${event.userId}")
        
        // Example business logic:
        // 1. Validate user data
        validateUserData(event.userId.toString(), event.email.toString())
        
        // 2. Send welcome email
        sendWelcomeEmail(event.email.toString())
        
        // 3. Update user analytics
        updateUserAnalytics(event.userId.toString(), event.registrationMethod.toString())
        
        // 4. Create user profile
        createUserProfile(event)
        
        logger.info("Successfully processed user creation for: ${event.userId}")
    }
    
    private fun processGenericUserEvent(record: GenericRecord) {
        logger.debug("Processing generic user event for: ${record.get("userId")}")
        
        // Extract data from generic record
        val userId = record.get("userId").toString()
        val email = record.get("email").toString()
        val method = record.get("registrationMethod").toString()
        
        // Process similar to specific Avro
        validateUserData(userId, email)
        sendWelcomeEmail(email)
        updateUserAnalytics(userId, method)
        
        logger.info("Successfully processed generic user event for: $userId")
    }
    
    private fun processProtobufEvent(event: Any) {
        logger.debug("Processing protobuf event: $event")
        
        // In a real implementation, this would cast to specific protobuf type
        // For now, process as generic object
        
        logger.info("Successfully processed protobuf event")
    }
    
    private fun validateUserData(userId: String, email: String) {
        // Simulate validation logic
        if (userId.isBlank() || email.isBlank()) {
            throw IllegalArgumentException("Invalid user data: userId=$userId, email=$email")
        }
        
        if (!email.contains("@")) {
            throw IllegalArgumentException("Invalid email format: $email")
        }
        
        logger.debug("User data validation passed for: $userId")
    }
    
    private fun sendWelcomeEmail(email: String) {
        // Simulate email sending
        logger.debug("Sending welcome email to: $email")
        
        // In real implementation:
        // emailService.sendWelcomeEmail(email)
        
        Thread.sleep(10) // Simulate processing time
        logger.debug("Welcome email sent to: $email")
    }
    
    private fun updateUserAnalytics(userId: String, registrationMethod: String) {
        // Simulate analytics update
        logger.debug("Updating analytics for user: $userId, method: $registrationMethod")
        
        // In real implementation:
        // analyticsService.trackUserRegistration(userId, registrationMethod)
        
        Thread.sleep(5) // Simulate processing time
        logger.debug("Analytics updated for user: $userId")
    }
    
    private fun createUserProfile(event: UserCreatedEvent) {
        // Simulate user profile creation
        logger.debug("Creating user profile for: ${event.userId}")
        
        // In real implementation:
        // userProfileService.createProfile(event)
        
        Thread.sleep(15) // Simulate processing time
        logger.debug("User profile created for: ${event.userId}")
    }
}