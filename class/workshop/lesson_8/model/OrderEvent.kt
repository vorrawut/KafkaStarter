package com.learning.KafkaStarter.model

import com.fasterxml.jackson.annotation.JsonProperty
import java.time.Instant

data class OrderEvent(
    @JsonProperty("orderId")
    val orderId: String,
    
    @JsonProperty("customerId")
    val customerId: String,
    
    @JsonProperty("productId")
    val productId: String,
    
    @JsonProperty("quantity")
    val quantity: Int,
    
    @JsonProperty("price")
    val price: Double,
    
    @JsonProperty("status")
    val status: String,
    
    @JsonProperty("timestamp")
    val timestamp: Long = Instant.now().toEpochMilli(),
    
    @JsonProperty("priority")
    val priority: String = "NORMAL",
    
    @JsonProperty("metadata")
    val metadata: Map<String, Any> = emptyMap()
)