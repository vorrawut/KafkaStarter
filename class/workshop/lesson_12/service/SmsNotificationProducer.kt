package com.learning.KafkaStarter.service

import com.learning.KafkaStarter.model.SmsNotification
import com.learning.KafkaStarter.model.NotificationTrigger
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.stereotype.Service

@Service
class SmsNotificationProducer {
    
    @Autowired
    private lateinit var kafkaTemplate: KafkaTemplate<String, SmsNotification>
    
    fun sendSmsNotification(trigger: NotificationTrigger, recipientPhone: String) {
        try {
            // TODO: Create and send SMS notification
            // HINT: Transform trigger into SMS notification and send to SMS topic
            TODO("Create and send SMS notification")
        } catch (e: Exception) {
            // TODO: Handle SMS send exceptions
            TODO("Handle SMS send exception")
        }
    }
    
    fun createSmsFromTrigger(trigger: NotificationTrigger, recipientPhone: String): SmsNotification {
        // TODO: Create SMS notification from trigger
        // HINT: Generate concise message based on event type
        TODO("Create SMS from trigger")
    }
    
    fun getSmsMessage(eventType: String, eventData: Map<String, Any>): String {
        // TODO: Generate SMS message based on event type and data
        // HINT: Create short, informative messages for different event types
        TODO("Generate SMS message")
    }
}