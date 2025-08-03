package com.learning.KafkaStarter.model

import com.fasterxml.jackson.annotation.JsonProperty
import java.time.Instant

data class PriceRequest(
    @JsonProperty("requestId")
    val requestId: String,
    
    @JsonProperty("productId")
    val productId: String,
    
    @JsonProperty("quantity")
    val quantity: Int,
    
    @JsonProperty("customerId")
    val customerId: String,
    
    @JsonProperty("timestamp")
    val timestamp: Long = Instant.now().toEpochMilli(),
    
    @JsonProperty("replyTo")
    val replyTo: String,
    
    @JsonProperty("timeoutMs")
    val timeoutMs: Long = 30000
)

data class PriceResponse(
    @JsonProperty("requestId")
    val requestId: String,
    
    @JsonProperty("productId")
    val productId: String,
    
    @JsonProperty("unitPrice")
    val unitPrice: Double,
    
    @JsonProperty("totalPrice")
    val totalPrice: Double,
    
    @JsonProperty("discount")
    val discount: Double = 0.0,
    
    @JsonProperty("currency")
    val currency: String = "USD",
    
    @JsonProperty("timestamp")
    val timestamp: Long = Instant.now().toEpochMilli(),
    
    @JsonProperty("status")
    val status: String,
    
    @JsonProperty("message")
    val message: String? = null
)

data class ValidationRequest(
    @JsonProperty("requestId")
    val requestId: String,
    
    @JsonProperty("dataType")
    val dataType: String,
    
    @JsonProperty("data")
    val data: Map<String, Any>,
    
    @JsonProperty("rules")
    val rules: List<String> = emptyList(),
    
    @JsonProperty("timestamp")
    val timestamp: Long = Instant.now().toEpochMilli(),
    
    @JsonProperty("replyTo")
    val replyTo: String,
    
    @JsonProperty("timeoutMs")
    val timeoutMs: Long = 15000
)

data class ValidationResponse(
    @JsonProperty("requestId")
    val requestId: String,
    
    @JsonProperty("isValid")
    val isValid: Boolean,
    
    @JsonProperty("errors")
    val errors: List<String> = emptyList(),
    
    @JsonProperty("warnings")
    val warnings: List<String> = emptyList(),
    
    @JsonProperty("processedRules")
    val processedRules: Int,
    
    @JsonProperty("timestamp")
    val timestamp: Long = Instant.now().toEpochMilli()
)