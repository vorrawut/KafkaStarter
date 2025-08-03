package com.learning.KafkaStarter.service

import com.learning.KafkaStarter.model.UserRegisteredEvent
import org.springframework.stereotype.Service

// TODO: Add @Service annotation
// TODO: Inject KafkaTemplate<String, UserRegisteredEvent>
// TODO: Create publishUserRegistered method
// TODO: Add logging and error handling

@Service
class UserEventProducer(
    // TODO: Inject KafkaTemplate
) {
    
    companion object {
        const val USER_REGISTRATION_TOPIC = "user-registration"
    }
    
    // TODO: Implement publishUserRegistered(event: UserRegisteredEvent)
    // TODO: Use kafkaTemplate.send(topic, key, value)
    // TODO: Use event.userId as the key
    // TODO: Add success/failure callbacks
    // TODO: Add logging
    
    // HINT: kafkaTemplate.send returns a CompletableFuture
    // HINT: Use .thenAccept{} for success and .exceptionally{} for errors
}