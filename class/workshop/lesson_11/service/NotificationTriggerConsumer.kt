package com.learning.KafkaStarter.service

import com.learning.KafkaStarter.model.NotificationTrigger
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.kafka.support.KafkaHeaders
import org.springframework.messaging.handler.annotation.Header
import org.springframework.messaging.handler.annotation.Payload
import org.springframework.stereotype.Service

@Service
class NotificationTriggerConsumer {
    
    @Autowired
    private lateinit var notificationFanoutService: NotificationFanoutService
    
    @KafkaListener(topics = ["notification-triggers"])
    fun processNotificationTrigger(
        @Payload trigger: NotificationTrigger,
        @Header(KafkaHeaders.RECEIVED_PARTITION_ID) partition: Int,
        @Header(KafkaHeaders.OFFSET) offset: Long
    ) {
        try {
            // TODO: Process notification trigger and fan out to multiple channels
            // HINT: Use notificationFanoutService to distribute notifications
            TODO("Process notification trigger")
        } catch (e: Exception) {
            // TODO: Handle processing exceptions
            TODO("Handle notification trigger processing exception")
        }
    }
    
    @KafkaListener(topics = ["urgent-notifications"])
    fun processUrgentNotification(
        @Payload trigger: NotificationTrigger,
        @Header(KafkaHeaders.RECEIVED_TOPIC) topic: String
    ) {
        try {
            // TODO: Process urgent notifications with expedited fan-out
            // HINT: Use different logic for urgent notifications
            TODO("Process urgent notification")
        } catch (e: Exception) {
            // TODO: Handle urgent notification processing exceptions
            TODO("Handle urgent notification processing exception")
        }
    }
}