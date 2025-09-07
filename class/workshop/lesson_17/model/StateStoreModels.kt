package com.learning.KafkaStarter.model

import com.fasterxml.jackson.annotation.JsonProperty
import java.time.Instant

data class UserProfile(
    @JsonProperty("userId")
    val userId: String = "",
    
    @JsonProperty("eventCount")
    val eventCount: Long = 0,
    
    @JsonProperty("lastSeen")
    val lastSeen: Long = 0,
    
    @JsonProperty("firstSeen")
    val firstSeen: Long = Instant.now().toEpochMilli(),
    
    @JsonProperty("preferences")
    val preferences: Map<String, String> = emptyMap(),
    
    @JsonProperty("totalValue")
    val totalValue: Double = 0.0
)

data class SessionData(
    @JsonProperty("sessionId")
    val sessionId: String = "",
    
    @JsonProperty("userId")
    val userId: String = "",
    
    @JsonProperty("startTime")
    val startTime: Long = 0,
    
    @JsonProperty("lastActivity")
    val lastActivity: Long = 0,
    
    @JsonProperty("eventCount")
    val eventCount: Int = 0,
    
    @JsonProperty("pageViews")
    val pageViews: Int = 0,
    
    @JsonProperty("actions")
    val actions: List<String> = emptyList()
)

data class BackupMetadata(
    @JsonProperty("backupId")
    val backupId: String,
    
    @JsonProperty("timestamp")
    val timestamp: Long,
    
    @JsonProperty("storeNames")
    val storeNames: List<String>,
    
    @JsonProperty("backupSize")
    val backupSize: Long
)