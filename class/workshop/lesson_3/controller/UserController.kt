package com.learning.KafkaStarter.controller

import com.learning.KafkaStarter.model.UserRegisteredEvent
import com.learning.KafkaStarter.service.UserEventProducer
import org.springframework.web.bind.annotation.*

// TODO: Add @RestController annotation
// TODO: Add @RequestMapping("/api/users")
// TODO: Inject UserEventProducer
// TODO: Create POST endpoint for user registration

data class RegisterUserRequest(
    val userId: String,
    val email: String,
    val source: String = "api"
)

data class RegisterUserResponse(
    val message: String,
    val userId: String
)

class UserController(
    // TODO: Inject UserEventProducer
) {
    
    // TODO: Create @PostMapping("/register") endpoint
    // TODO: Accept @RequestBody RegisterUserRequest
    // TODO: Convert request to UserRegisteredEvent
    // TODO: Call producer.publishUserRegistered()
    // TODO: Return RegisterUserResponse
    // TODO: Add error handling
    
    // HINT: Return ResponseEntity<RegisterUserResponse>
    // HINT: Use HTTP 200 for success, 500 for errors
}