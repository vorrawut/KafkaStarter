package com.demo.userservice.service

import com.demo.userservice.model.UserEvent
import com.demo.userservice.model.UserRegisteredEvent
import com.demo.userservice.model.UserLoginEvent
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.kafka.support.SendResult
import org.springframework.stereotype.Service
import java.util.concurrent.CompletableFuture
import java.util.concurrent.atomic.AtomicLong

@Service
class UserEventPublisher {
    
    @Autowired
    private lateinit var kafkaTemplate: KafkaTemplate<String, Any>
    
    private val logger = org.slf4j.LoggerFactory.getLogger(UserEventPublisher::class.java)
    
    // Metrics
    private val eventsPublished = AtomicLong(0)
    private val publishFailures = AtomicLong(0)
    
    companion object {
        const val USER_EVENTS_TOPIC = "user-events"
        const val USER_REGISTERED_TOPIC = "user-registered"
        const val USER_LOGIN_TOPIC = "user-login"
    }
    
    fun publishUserEvent(userEvent: UserEvent) {
        try {
            logger.debug("Publishing user event: ${userEvent.eventType} for user ${userEvent.userId}")
            
            val future: CompletableFuture<SendResult<String, Any>> = kafkaTemplate.send(
                USER_EVENTS_TOPIC,
                userEvent.userId.toString(), // Use user ID as key for consistent partitioning
                userEvent
            )
            
            future.whenComplete { result, throwable ->
                if (throwable == null) {
                    eventsPublished.incrementAndGet()
                    val metadata = result.recordMetadata
                    logger.debug("User event published successfully: topic=${metadata.topic()}, " +
                        "partition=${metadata.partition()}, offset=${metadata.offset()}")
                } else {
                    publishFailures.incrementAndGet()
                    logger.error("Failed to publish user event: ${userEvent.eventType} for user ${userEvent.userId}", throwable)
                }
            }
            
        } catch (e: Exception) {
            publishFailures.incrementAndGet()
            logger.error("Exception publishing user event: ${userEvent.eventType} for user ${userEvent.userId}", e)
        }
    }
    
    fun publishUserRegistered(registeredEvent: UserRegisteredEvent) {
        try {
            logger.info("Publishing user registration event for user: ${registeredEvent.username}")
            
            val future: CompletableFuture<SendResult<String, Any>> = kafkaTemplate.send(
                USER_REGISTERED_TOPIC,
                registeredEvent.userId.toString(),
                registeredEvent
            )
            
            future.whenComplete { result, throwable ->
                if (throwable == null) {
                    eventsPublished.incrementAndGet()
                    val metadata = result.recordMetadata
                    logger.info("User registration event published: userId=${registeredEvent.userId}, " +
                        "topic=${metadata.topic()}, partition=${metadata.partition()}, offset=${metadata.offset()}")
                } else {
                    publishFailures.incrementAndGet()
                    logger.error("Failed to publish user registration event for user: ${registeredEvent.userId}", throwable)
                }
            }
            
        } catch (e: Exception) {
            publishFailures.incrementAndGet()
            logger.error("Exception publishing user registration event for user: ${registeredEvent.userId}", e)
        }
    }
    
    fun publishUserLogin(loginEvent: UserLoginEvent) {
        try {
            logger.debug("Publishing user login event for user: ${loginEvent.username}")
            
            val future: CompletableFuture<SendResult<String, Any>> = kafkaTemplate.send(
                USER_LOGIN_TOPIC,
                loginEvent.userId.toString(),
                loginEvent
            )
            
            future.whenComplete { result, throwable ->
                if (throwable == null) {
                    eventsPublished.incrementAndGet()
                    val metadata = result.recordMetadata
                    logger.debug("User login event published: userId=${loginEvent.userId}, " +
                        "sessionId=${loginEvent.sessionId}, topic=${metadata.topic()}, " +
                        "partition=${metadata.partition()}, offset=${metadata.offset()}")
                } else {
                    publishFailures.incrementAndGet()
                    logger.error("Failed to publish user login event for user: ${loginEvent.userId}", throwable)
                }
            }
            
        } catch (e: Exception) {
            publishFailures.incrementAndGet()
            logger.error("Exception publishing user login event for user: ${loginEvent.userId}", e)
        }
    }
    
    // Metrics methods
    fun getPublisherMetrics(): Map<String, Any> {
        return mapOf(
            "eventsPublished" to eventsPublished.get(),
            "publishFailures" to publishFailures.get(),
            "successRate" to if (eventsPublished.get() + publishFailures.get() > 0) {
                (eventsPublished.get().toDouble() / (eventsPublished.get() + publishFailures.get())) * 100
            } else 0.0
        )
    }
    
    fun resetMetrics() {
        eventsPublished.set(0)
        publishFailures.set(0)
        logger.info("User event publisher metrics reset")
    }
}