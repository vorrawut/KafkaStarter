package com.learning.KafkaStarter.model

import com.fasterxml.jackson.annotation.JsonProperty
import java.time.Instant

data class UserEvent(
    @JsonProperty("userId")
    val userId: String,
    
    @JsonProperty("eventType")
    val eventType: String,
    
    @JsonProperty("timestamp")
    val timestamp: Long = Instant.now().toEpochMilli(),
    
    @JsonProperty("data")
    val data: Map<String, Any> = emptyMap(),
    
    @JsonProperty("sessionId")
    val sessionId: String? = null
)

data class EnrichedUserEvent(
    @JsonProperty("userId")
    val userId: String,
    
    @JsonProperty("eventType") 
    val eventType: String,
    
    @JsonProperty("timestamp")
    val timestamp: Long,
    
    @JsonProperty("originalData")
    val originalData: Map<String, Any>,
    
    @JsonProperty("enrichedData")
    val enrichedData: Map<String, Any>,
    
    @JsonProperty("processingTimestamp")
    val processingTimestamp: Long = Instant.now().toEpochMilli()
)