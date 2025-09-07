package com.learning.KafkaStarter.model

import com.fasterxml.jackson.annotation.JsonProperty
import java.time.Instant

data class RawCustomerEvent(
    @JsonProperty("eventId")
    val eventId: String,
    
    @JsonProperty("customerId")
    val customerId: String,
    
    @JsonProperty("eventType")
    val eventType: String,
    
    @JsonProperty("rawData")
    val rawData: Map<String, Any>,
    
    @JsonProperty("timestamp")
    val timestamp: Long = Instant.now().toEpochMilli(),
    
    @JsonProperty("source")
    val source: String,
    
    @JsonProperty("version")
    val version: String = "1.0"
)

data class EnrichedCustomerEvent(
    @JsonProperty("eventId")
    val eventId: String,
    
    @JsonProperty("customerId")
    val customerId: String,
    
    @JsonProperty("customerSegment")
    val customerSegment: String,
    
    @JsonProperty("eventType")
    val eventType: String,
    
    @JsonProperty("enrichedData")
    val enrichedData: Map<String, Any>,
    
    @JsonProperty("transformationMetadata")
    val transformationMetadata: TransformationMetadata,
    
    @JsonProperty("timestamp")
    val timestamp: Long,
    
    @JsonProperty("processedAt")
    val processedAt: Long = Instant.now().toEpochMilli()
)

data class FilteredEvent(
    @JsonProperty("eventId")
    val eventId: String,
    
    @JsonProperty("filteredReason")
    val filteredReason: String,
    
    @JsonProperty("originalEvent")
    val originalEvent: RawCustomerEvent,
    
    @JsonProperty("filterCriteria")
    val filterCriteria: Map<String, Any>,
    
    @JsonProperty("timestamp")
    val timestamp: Long = Instant.now().toEpochMilli()
)

data class TransformationMetadata(
    @JsonProperty("transformedAt")
    val transformedAt: Long = Instant.now().toEpochMilli(),
    
    @JsonProperty("transformationType")
    val transformationType: String,
    
    @JsonProperty("enrichmentSources")
    val enrichmentSources: List<String>,
    
    @JsonProperty("transformationRules")
    val transformationRules: List<String>,
    
    @JsonProperty("processingTimeMs")
    val processingTimeMs: Long
)