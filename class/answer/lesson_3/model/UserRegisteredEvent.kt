package com.learning.KafkaStarter.model

import com.fasterxml.jackson.annotation.JsonFormat
import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import java.time.Instant

data class UserRegisteredEvent(
    @field:NotBlank(message = "User ID cannot be blank")
    val userId: String,
    
    @field:Email(message = "Email must be valid")
    @field:NotBlank(message = "Email cannot be blank")
    val email: String,
    
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSXXX", timezone = "UTC")
    val timestamp: Instant = Instant.now(),
    
    @field:NotBlank(message = "Source cannot be blank")
    val source: String
) {
    // Secondary constructor for easy creation
    constructor(userId: String, email: String, source: String) : 
        this(userId, email, Instant.now(), source)
}