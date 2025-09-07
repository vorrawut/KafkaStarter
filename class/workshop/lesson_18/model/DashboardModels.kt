package com.learning.KafkaStarter.model

import com.fasterxml.jackson.annotation.JsonProperty
import java.time.Instant

data class WebEvent(
    @JsonProperty("eventId")
    val eventId: String,
    
    @JsonProperty("userId")
    val userId: String,
    
    @JsonProperty("sessionId") 
    val sessionId: String,
    
    @JsonProperty("action")
    val action: String,
    
    @JsonProperty("timestamp")
    val timestamp: Long = Instant.now().toEpochMilli(),
    
    @JsonProperty("page")
    val page: String = "",
    
    @JsonProperty("userAgent")
    val userAgent: String = "",
    
    @JsonProperty("customData")
    val customData: Map<String, Any> = emptyMap()
)

data class TransactionEvent(
    @JsonProperty("transactionId")
    val transactionId: String,
    
    @JsonProperty("userId")
    val userId: String,
    
    @JsonProperty("amount")
    val amount: Double,
    
    @JsonProperty("currency")
    val currency: String = "USD",
    
    @JsonProperty("type")
    val type: String,
    
    @JsonProperty("timestamp")
    val timestamp: Long = Instant.now().toEpochMilli(),
    
    @JsonProperty("metadata")
    val metadata: Map<String, Any> = emptyMap()
)

data class DashboardMetric(
    @JsonProperty("metricType")
    val metricType: String,
    
    @JsonProperty("value")
    val value: Double,
    
    @JsonProperty("timestamp")
    val timestamp: Long,
    
    @JsonProperty("breakdown")
    val breakdown: Map<String, Any> = emptyMap()
)

data class Alert(
    @JsonProperty("alertId")
    val alertId: String,
    
    @JsonProperty("alertType")
    val alertType: String,
    
    @JsonProperty("severity")
    val severity: String,
    
    @JsonProperty("message")
    val message: String,
    
    @JsonProperty("timestamp")
    val timestamp: Long,
    
    @JsonProperty("data")
    val data: Map<String, Any> = emptyMap()
)