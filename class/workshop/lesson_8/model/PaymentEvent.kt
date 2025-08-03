package com.learning.KafkaStarter.model

import com.fasterxml.jackson.annotation.JsonProperty
import java.time.Instant

data class PaymentEvent(
    @JsonProperty("paymentId")
    val paymentId: String,
    
    @JsonProperty("orderId")
    val orderId: String,
    
    @JsonProperty("customerId")
    val customerId: String,
    
    @JsonProperty("amount")
    val amount: Double,
    
    @JsonProperty("currency")
    val currency: String = "USD",
    
    @JsonProperty("paymentMethod")
    val paymentMethod: String,
    
    @JsonProperty("status")
    val status: String,
    
    @JsonProperty("timestamp")
    val timestamp: Long = Instant.now().toEpochMilli(),
    
    @JsonProperty("retryCount")
    val retryCount: Int = 0,
    
    @JsonProperty("metadata")
    val metadata: Map<String, Any> = emptyMap()
)

data class PaymentFailure(
    @JsonProperty("paymentId")
    val paymentId: String,
    
    @JsonProperty("originalEvent")
    val originalEvent: PaymentEvent,
    
    @JsonProperty("errorType")
    val errorType: String,
    
    @JsonProperty("errorMessage")
    val errorMessage: String,
    
    @JsonProperty("failureCount")
    val failureCount: Int,
    
    @JsonProperty("timestamp")
    val timestamp: Long = Instant.now().toEpochMilli(),
    
    @JsonProperty("stackTrace")
    val stackTrace: String? = null
)