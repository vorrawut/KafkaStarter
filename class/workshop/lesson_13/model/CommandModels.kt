package com.learning.KafkaStarter.model

import com.fasterxml.jackson.annotation.JsonProperty
import java.time.Instant

data class InventoryCommand(
    @JsonProperty("commandId")
    val commandId: String,
    
    @JsonProperty("commandType")
    val commandType: String,
    
    @JsonProperty("productId")
    val productId: String,
    
    @JsonProperty("quantity")
    val quantity: Int,
    
    @JsonProperty("reason")
    val reason: String,
    
    @JsonProperty("timestamp")
    val timestamp: Long = Instant.now().toEpochMilli(),
    
    @JsonProperty("metadata")
    val metadata: Map<String, Any> = emptyMap()
)

data class CommandResult(
    @JsonProperty("commandId")
    val commandId: String,
    
    @JsonProperty("status")
    val status: String,
    
    @JsonProperty("result")
    val result: Map<String, Any>,
    
    @JsonProperty("message")
    val message: String,
    
    @JsonProperty("timestamp")
    val timestamp: Long = Instant.now().toEpochMilli(),
    
    @JsonProperty("processingTimeMs")
    val processingTimeMs: Long
)

data class RestApiCall(
    @JsonProperty("callId")
    val callId: String,
    
    @JsonProperty("method")
    val method: String,
    
    @JsonProperty("url")
    val url: String,
    
    @JsonProperty("headers")
    val headers: Map<String, String> = emptyMap(),
    
    @JsonProperty("body")
    val body: String? = null,
    
    @JsonProperty("triggeredBy")
    val triggeredBy: String,
    
    @JsonProperty("timestamp")
    val timestamp: Long = Instant.now().toEpochMilli()
)

data class WebhookEvent(
    @JsonProperty("webhookId")
    val webhookId: String,
    
    @JsonProperty("eventType")
    val eventType: String,
    
    @JsonProperty("targetUrl")
    val targetUrl: String,
    
    @JsonProperty("payload")
    val payload: Map<String, Any>,
    
    @JsonProperty("timestamp")
    val timestamp: Long = Instant.now().toEpochMilli(),
    
    @JsonProperty("retryCount")
    val retryCount: Int = 0
)