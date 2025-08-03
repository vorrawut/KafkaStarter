package com.learning.KafkaStarter.model

import com.fasterxml.jackson.annotation.JsonFormat
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import java.time.Instant

enum class ActivityType {
    LOGIN, LOGOUT, PURCHASE, UPDATE_PROFILE, VIEW_PRODUCT, ADD_TO_CART, REMOVE_FROM_CART
}

data class UserActivity(
    @field:NotBlank(message = "User ID cannot be blank")
    val userId: String,
    
    @field:NotNull(message = "Activity type cannot be null")
    val activityType: ActivityType,
    
    val metadata: Map<String, Any> = emptyMap(),
    
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSXXX", timezone = "UTC")
    val timestamp: Instant = Instant.now(),
    
    val sessionId: String? = null,
    val ipAddress: String? = null,
    val userAgent: String? = null
) {
    // Helper function for partition key
    fun getPartitionKey(): String = userId
    
    // Helper function for activity categorization
    fun getCategory(): String = when (activityType) {
        ActivityType.LOGIN, ActivityType.LOGOUT -> "authentication"
        ActivityType.PURCHASE -> "transaction"
        ActivityType.UPDATE_PROFILE -> "profile"
        ActivityType.VIEW_PRODUCT, ActivityType.ADD_TO_CART, ActivityType.REMOVE_FROM_CART -> "shopping"
    }
}