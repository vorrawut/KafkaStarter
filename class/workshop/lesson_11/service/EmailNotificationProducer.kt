package com.learning.KafkaStarter.service

import com.learning.KafkaStarter.model.EmailNotification
import com.learning.KafkaStarter.model.NotificationTrigger
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.stereotype.Service

@Service
class EmailNotificationProducer {
    
    @Autowired
    private lateinit var kafkaTemplate: KafkaTemplate<String, EmailNotification>
    
    fun sendEmailNotification(trigger: NotificationTrigger, recipientEmail: String) {
        try {
            // TODO: Create and send email notification
            // HINT: Transform trigger into email notification and send to email topic
            TODO("Create and send email notification")
        } catch (e: Exception) {
            // TODO: Handle email send exceptions
            TODO("Handle email send exception")
        }
    }
    
    fun createEmailFromTrigger(trigger: NotificationTrigger, recipientEmail: String): EmailNotification {
        // TODO: Create email notification from trigger
        // HINT: Generate subject, body, and template based on event type
        TODO("Create email from trigger")
    }
    
    fun getEmailTemplate(eventType: String): String {
        // TODO: Get email template based on event type
        // HINT: Return appropriate template name for different event types
        TODO("Get email template")
    }
}