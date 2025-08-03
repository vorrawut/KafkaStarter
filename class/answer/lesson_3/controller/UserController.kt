package com.learning.KafkaStarter.controller

import com.learning.KafkaStarter.model.UserRegisteredEvent
import com.learning.KafkaStarter.service.UserEventProducer
import jakarta.validation.Valid
import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import org.slf4j.LoggerFactory
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/users")
class UserController(
    private val userEventProducer: UserEventProducer
) {
    
    private val logger = LoggerFactory.getLogger(UserController::class.java)
    
    @PostMapping("/register")
    fun registerUser(@Valid @RequestBody request: RegisterUserRequest): ResponseEntity<RegisterUserResponse> {
        logger.info("Received user registration request: userId=${request.userId}, email=${request.email}")
        
        return try {
            // Convert request to event
            val event = UserRegisteredEvent(
                userId = request.userId,
                email = request.email,
                source = request.source
            )
            
            // Publish event to Kafka
            userEventProducer.publishUserRegistered(event)
            
            // Return immediate response
            val response = RegisterUserResponse(
                message = "User registration initiated successfully",
                userId = request.userId
            )
            
            logger.info("User registration initiated successfully: userId=${request.userId}")
            ResponseEntity.ok(response)
            
        } catch (e: Exception) {
            logger.error("Failed to initiate user registration: userId=${request.userId}", e)
            
            val errorResponse = RegisterUserResponse(
                message = "Failed to initiate user registration: ${e.message}",
                userId = request.userId
            )
            
            ResponseEntity.internalServerError().body(errorResponse)
        }
    }
    
    @GetMapping("/health")
    fun health(): ResponseEntity<Map<String, String>> {
        return ResponseEntity.ok(mapOf(
            "status" to "UP",
            "service" to "User Registration Service"
        ))
    }
}

data class RegisterUserRequest(
    @field:NotBlank(message = "User ID cannot be blank")
    val userId: String,
    
    @field:Email(message = "Email must be valid")
    @field:NotBlank(message = "Email cannot be blank")
    val email: String,
    
    @field:NotBlank(message = "Source cannot be blank")
    val source: String = "api"
)

data class RegisterUserResponse(
    val message: String,
    val userId: String
)