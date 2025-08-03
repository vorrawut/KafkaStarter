package com.learning.KafkaStarter.service

import com.learning.KafkaStarter.model.UserRegisteredEvent
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.stereotype.Service

// TODO: Add @Service annotation
// TODO: Create @KafkaListener method
// TODO: Add proper error handling
// TODO: Add logging

@Service
class UserEventConsumer {
    
    // TODO: Create @KafkaListener method with topic = "user-registration"
    // TODO: Method should accept UserRegisteredEvent parameter
    // TODO: Add logging to show event processing
    // TODO: Add error handling with try-catch
    // TODO: Simulate some business logic processing
    
    // HINT: @KafkaListener(topics = ["user-registration"])
    // HINT: Use logger.info() to show processing
    // HINT: Add Thread.sleep() to simulate processing time
}