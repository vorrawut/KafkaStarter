package com.learning.KafkaStarter.service

import com.learning.KafkaStarter.model.SecureMessage
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.kafka.support.SendResult
import org.springframework.stereotype.Service
import java.security.MessageDigest
import java.util.concurrent.CompletableFuture
import java.util.concurrent.atomic.AtomicLong

@Service
class SecureMessageProducer {
    
    @Autowired
    private lateinit var kafkaTemplate: KafkaTemplate<String, SecureMessage>
    
    @Autowired
    private lateinit var securityService: SecurityService
    
    private val logger = org.slf4j.LoggerFactory.getLogger(SecureMessageProducer::class.java)
    
    // Statistics
    private val messagesProduced = AtomicLong(0)
    private val secureMessagesProduced = AtomicLong(0)
    private val accessDenials = AtomicLong(0)
    
    fun sendSecureMessage(message: SecureMessage, userRole: String): Boolean {
        messagesProduced.incrementAndGet()
        
        return try {
            logger.info("Sending secure message: messageId=${message.messageId}, " +
                "classification=${message.classification}, userRole=$userRole")
            
            // Validate user access for message classification
            if (!validateUserAccess(message.userId, message.classification)) {
                logger.warn("Access denied for user ${message.userId} to send ${message.classification} message")
                accessDenials.incrementAndGet()
                return false
            }
            
            // Check authorization
            if (!securityService.authorizeOperation(message.userId, "secure-messages", "WRITE")) {
                logger.warn("Authorization failed for user ${message.userId}")
                accessDenials.incrementAndGet()
                return false
            }
            
            // Sign message for integrity
            val signedMessage = signMessage(message)
            
            // Encrypt sensitive data if needed
            val processedMessage = if (message.classification in setOf("CONFIDENTIAL", "SECRET")) {
                encryptSensitiveFields(signedMessage)
            } else {
                signedMessage
            }
            
            // Send to classification-appropriate topic
            val success = sendToClassifiedTopic(processedMessage)
            
            if (success) {
                secureMessagesProduced.incrementAndGet()
                logger.info("Secure message sent successfully: messageId=${message.messageId}")
            }
            
            success
            
        } catch (e: Exception) {
            logger.error("Failed to send secure message: messageId=${message.messageId}", e)
            false
        }
    }
    
    fun sendToClassifiedTopic(message: SecureMessage): Boolean {
        return try {
            // Route to topic based on message classification level
            val topic = when (message.classification) {
                "PUBLIC" -> "public-messages"
                "INTERNAL" -> "internal-messages" 
                "CONFIDENTIAL" -> "confidential-messages"
                "SECRET" -> "secret-messages"
                else -> "secure-messages"
            }
            
            val future: CompletableFuture<SendResult<String, SecureMessage>> = kafkaTemplate.send(
                topic,
                message.userId, // Use user ID as key for partitioning
                message
            )
            
            future.whenComplete { result, throwable ->
                if (throwable == null) {
                    val metadata = result.recordMetadata
                    logger.debug("Message sent to classified topic: messageId=${message.messageId}, " +
                        "topic=$topic, partition=${metadata.partition()}, offset=${metadata.offset()}")
                } else {
                    logger.error("Failed to send to classified topic: messageId=${message.messageId}, " +
                        "topic=$topic", throwable)
                }
            }
            
            true
            
        } catch (e: Exception) {
            logger.error("Exception sending to classified topic: messageId=${message.messageId}", e)
            false
        }
    }
    
    fun signMessage(message: SecureMessage): SecureMessage {
        try {
            // Create message signature using SHA-256
            val messageContent = "${message.messageId}:${message.userId}:${message.data}:${message.timestamp}"
            val digest = MessageDigest.getInstance("SHA-256")
            val hash = digest.digest(messageContent.toByteArray())
            val signature = hash.joinToString("") { "%02x".format(it) }
            
            return message.copy(signature = signature)
            
        } catch (e: Exception) {
            logger.error("Failed to sign message: messageId=${message.messageId}", e)
            return message
        }
    }
    
    fun validateUserAccess(userId: String, classification: String): Boolean {
        try {
            // Get user credentials to check role
            val user = securityService.authenticateUser(userId, "temp") // In real system, get from context
            if (user == null) {
                logger.warn("User not found for access validation: $userId")
                return false
            }
            
            // Check if user role can access this classification
            val userRole = user.roles.maxByOrNull { roleLevel(it) }?.let { it } ?: "USER"
            
            return securityService.validateMessageClassification(classification, userRole)
            
        } catch (e: Exception) {
            logger.error("Error validating user access: userId=$userId, classification=$classification", e)
            return false
        }
    }
    
    private fun encryptSensitiveFields(message: SecureMessage): SecureMessage {
        try {
            // Encrypt sensitive data fields
            val encryptedData = message.data.mapValues { (key, value) ->
                if (isSensitiveField(key)) {
                    securityService.encryptSensitiveData(value.toString())
                } else {
                    value
                }
            }
            
            return message.copy(data = encryptedData)
            
        } catch (e: Exception) {
            logger.error("Failed to encrypt sensitive fields: messageId=${message.messageId}", e)
            return message
        }
    }
    
    private fun isSensitiveField(fieldName: String): Boolean {
        val sensitiveFields = setOf(
            "password", "ssn", "creditCard", "bankAccount", "personalId",
            "medicalRecord", "salary", "confidentialData", "secretKey"
        )
        
        return sensitiveFields.any { 
            fieldName.contains(it, ignoreCase = true) 
        }
    }
    
    private fun roleLevel(role: String): Int {
        return when (role) {
            "ADMIN" -> 3
            "MANAGER" -> 2
            "EMPLOYEE" -> 1
            "USER" -> 0
            else -> 0
        }
    }
    
    // Statistics methods
    fun getProducerStatistics(): Map<String, Any> {
        return mapOf(
            "messagesProduced" to messagesProduced.get(),
            "secureMessagesProduced" to secureMessagesProduced.get(),
            "accessDenials" to accessDenials.get(),
            "securityCompliance" to if (messagesProduced.get() > 0) {
                (secureMessagesProduced.get().toDouble() / messagesProduced.get()) * 100
            } else 0.0
        )
    }
    
    fun resetStatistics() {
        messagesProduced.set(0)
        secureMessagesProduced.set(0)
        accessDenials.set(0)
        logger.info("Secure message producer statistics reset")
    }
}