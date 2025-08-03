package com.demo.userservice.model

import com.fasterxml.jackson.annotation.JsonProperty
import java.time.Instant
import javax.persistence.*

@Entity
@Table(name = "users")
data class User(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,
    
    @Column(unique = true)
    val username: String,
    
    @Column(unique = true)
    val email: String,
    
    val password: String,
    
    val firstName: String,
    
    val lastName: String,
    
    val phoneNumber: String? = null,
    
    @Enumerated(EnumType.STRING)
    val status: UserStatus = UserStatus.ACTIVE,
    
    val createdAt: Instant = Instant.now(),
    
    val lastLoginAt: Instant? = null
)

enum class UserStatus {
    ACTIVE, INACTIVE, SUSPENDED, DELETED
}

// Kafka Event Models
data class UserEvent(
    @JsonProperty("eventId")
    val eventId: String,
    
    @JsonProperty("eventType")
    val eventType: String, // REGISTERED, LOGIN, LOGOUT, PROFILE_UPDATED, STATUS_CHANGED
    
    @JsonProperty("userId")
    val userId: Long,
    
    @JsonProperty("username")
    val username: String,
    
    @JsonProperty("email")
    val email: String,
    
    @JsonProperty("metadata")
    val metadata: Map<String, Any> = emptyMap(),
    
    @JsonProperty("timestamp")
    val timestamp: Long = Instant.now().toEpochMilli(),
    
    @JsonProperty("version")
    val version: String = "1.0"
)

data class UserRegisteredEvent(
    @JsonProperty("eventId")
    val eventId: String,
    
    @JsonProperty("userId")
    val userId: Long,
    
    @JsonProperty("username")
    val username: String,
    
    @JsonProperty("email")
    val email: String,
    
    @JsonProperty("firstName")
    val firstName: String,
    
    @JsonProperty("lastName")
    val lastName: String,
    
    @JsonProperty("phoneNumber")
    val phoneNumber: String? = null,
    
    @JsonProperty("registrationSource")
    val registrationSource: String = "WEB",
    
    @JsonProperty("timestamp")
    val timestamp: Long = Instant.now().toEpochMilli()
)

data class UserLoginEvent(
    @JsonProperty("eventId")
    val eventId: String,
    
    @JsonProperty("userId")
    val userId: Long,
    
    @JsonProperty("username")
    val username: String,
    
    @JsonProperty("loginMethod")
    val loginMethod: String = "PASSWORD",
    
    @JsonProperty("ipAddress")
    val ipAddress: String,
    
    @JsonProperty("userAgent")
    val userAgent: String,
    
    @JsonProperty("sessionId")
    val sessionId: String,
    
    @JsonProperty("timestamp")
    val timestamp: Long = Instant.now().toEpochMilli()
)

// DTOs
data class UserRegistrationRequest(
    val username: String,
    val email: String,
    val password: String,
    val firstName: String,
    val lastName: String,
    val phoneNumber: String?
)

data class UserLoginRequest(
    val username: String,
    val password: String
)

data class UserResponse(
    val id: Long,
    val username: String,
    val email: String,
    val firstName: String,
    val lastName: String,
    val phoneNumber: String?,
    val status: UserStatus,
    val createdAt: Instant,
    val lastLoginAt: Instant?
)