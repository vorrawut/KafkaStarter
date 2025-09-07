package com.learning.KafkaStarter.model

import com.fasterxml.jackson.annotation.JsonProperty
import java.time.Instant

data class SecureMessage(
    @JsonProperty("messageId")
    val messageId: String,
    
    @JsonProperty("userId")
    val userId: String,
    
    @JsonProperty("userRole")
    val userRole: String,
    
    @JsonProperty("data")
    val data: Map<String, Any>,
    
    @JsonProperty("classification")
    val classification: String, // PUBLIC, INTERNAL, CONFIDENTIAL, SECRET
    
    @JsonProperty("timestamp")
    val timestamp: Long = Instant.now().toEpochMilli(),
    
    @JsonProperty("signature")
    val signature: String? = null
)

data class UserCredentials(
    @JsonProperty("username")
    val username: String,
    
    @JsonProperty("password")
    val password: String,
    
    @JsonProperty("roles")
    val roles: List<String>,
    
    @JsonProperty("permissions")
    val permissions: List<String>,
    
    @JsonProperty("expiresAt")
    val expiresAt: Long
)

data class AclEntry(
    @JsonProperty("principal")
    val principal: String,
    
    @JsonProperty("resourceType")
    val resourceType: String, // TOPIC, GROUP, CLUSTER
    
    @JsonProperty("resourceName")
    val resourceName: String,
    
    @JsonProperty("operation")
    val operation: String, // READ, WRITE, CREATE, DELETE, ALTER, DESCRIBE
    
    @JsonProperty("permission")
    val permission: String, // ALLOW, DENY
    
    @JsonProperty("host")
    val host: String = "*",
    
    @JsonProperty("createdAt")
    val createdAt: Long = Instant.now().toEpochMilli()
)

data class SecurityAuditEvent(
    @JsonProperty("eventId")
    val eventId: String,
    
    @JsonProperty("eventType")
    val eventType: String, // AUTHENTICATION, AUTHORIZATION, ACCESS_DENIED
    
    @JsonProperty("principal")
    val principal: String,
    
    @JsonProperty("resource")
    val resource: String,
    
    @JsonProperty("operation")
    val operation: String,
    
    @JsonProperty("result")
    val result: String, // SUCCESS, FAILURE
    
    @JsonProperty("clientIp")
    val clientIp: String,
    
    @JsonProperty("timestamp")
    val timestamp: Long = Instant.now().toEpochMilli(),
    
    @JsonProperty("details")
    val details: Map<String, Any> = emptyMap()
)