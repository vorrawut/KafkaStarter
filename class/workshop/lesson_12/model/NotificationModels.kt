package com.learning.KafkaStarter.model

import com.fasterxml.jackson.annotation.JsonProperty
import java.time.Instant

data class NotificationTrigger(
    @JsonProperty("triggerId")
    val triggerId: String,
    
    @JsonProperty("eventType")
    val eventType: String,
    
    @JsonProperty("userId")
    val userId: String,
    
    @JsonProperty("eventData")
    val eventData: Map<String, Any>,
    
    @JsonProperty("timestamp")
    val timestamp: Long = Instant.now().toEpochMilli(),
    
    @JsonProperty("priority")
    val priority: String = "NORMAL",
    
    @JsonProperty("metadata")
    val metadata: Map<String, Any> = emptyMap()
)

data class EmailNotification(
    @JsonProperty("notificationId")
    val notificationId: String,
    
    @JsonProperty("triggerId")
    val triggerId: String,
    
    @JsonProperty("recipientEmail")
    val recipientEmail: String,
    
    @JsonProperty("subject")
    val subject: String,
    
    @JsonProperty("body")
    val body: String,
    
    @JsonProperty("templateName")
    val templateName: String,
    
    @JsonProperty("priority")
    val priority: String,
    
    @JsonProperty("timestamp")
    val timestamp: Long = Instant.now().toEpochMilli()
)

data class SmsNotification(
    @JsonProperty("notificationId")
    val notificationId: String,
    
    @JsonProperty("triggerId")
    val triggerId: String,
    
    @JsonProperty("recipientPhone")
    val recipientPhone: String,
    
    @JsonProperty("message")
    val message: String,
    
    @JsonProperty("priority")
    val priority: String,
    
    @JsonProperty("timestamp")
    val timestamp: Long = Instant.now().toEpochMilli()
)

data class PushNotification(
    @JsonProperty("notificationId")
    val notificationId: String,
    
    @JsonProperty("triggerId")
    val triggerId: String,
    
    @JsonProperty("deviceToken")
    val deviceToken: String,
    
    @JsonProperty("title")
    val title: String,
    
    @JsonProperty("body")
    val body: String,
    
    @JsonProperty("data")
    val data: Map<String, Any> = emptyMap(),
    
    @JsonProperty("priority")
    val priority: String,
    
    @JsonProperty("timestamp")
    val timestamp: Long = Instant.now().toEpochMilli()
)