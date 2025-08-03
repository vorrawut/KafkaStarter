package com.learning.KafkaStarter.model

import com.fasterxml.jackson.annotation.JsonProperty
import java.time.Instant
import java.util.*

data class BankTransferEvent(
    @JsonProperty("transferId")
    val transferId: String,
    
    @JsonProperty("fromAccount")
    val fromAccount: String,
    
    @JsonProperty("toAccount")
    val toAccount: String,
    
    @JsonProperty("amount")
    val amount: Double,
    
    @JsonProperty("currency")
    val currency: String = "USD",
    
    @JsonProperty("reference")
    val reference: String,
    
    @JsonProperty("status")
    val status: String,
    
    @JsonProperty("timestamp")
    val timestamp: Long = Instant.now().toEpochMilli(),
    
    @JsonProperty("idempotencyKey")
    val idempotencyKey: String = UUID.randomUUID().toString(),
    
    @JsonProperty("metadata")
    val metadata: Map<String, Any> = emptyMap()
)

data class TransferResult(
    @JsonProperty("transferId")
    val transferId: String,
    
    @JsonProperty("status")
    val status: String,
    
    @JsonProperty("resultCode")
    val resultCode: String,
    
    @JsonProperty("message")
    val message: String,
    
    @JsonProperty("processedAt")
    val processedAt: Long = Instant.now().toEpochMilli(),
    
    @JsonProperty("processingTimeMs")
    val processingTimeMs: Long,
    
    @JsonProperty("idempotent")
    val idempotent: Boolean = false
)