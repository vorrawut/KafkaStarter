package com.learning.KafkaStarter.controller

import com.learning.KafkaStarter.model.NotificationTrigger
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/fanout")
class FanoutController {
    
    @PostMapping("/trigger")
    fun triggerNotification(@RequestBody trigger: NotificationTrigger): ResponseEntity<Map<String, Any>> {
        return try {
            // TODO: Send notification trigger for fan-out processing
            // HINT: Send trigger to notification-triggers topic
            TODO("Send notification trigger")
        } catch (e: Exception) {
            // TODO: Handle trigger send error
            TODO("Handle trigger send error")
        }
    }
    
    @PostMapping("/trigger/urgent")
    fun triggerUrgentNotification(@RequestBody trigger: NotificationTrigger): ResponseEntity<Map<String, Any>> {
        return try {
            // TODO: Send urgent notification trigger
            // HINT: Send to urgent-notifications topic for expedited processing
            TODO("Send urgent notification trigger")
        } catch (e: Exception) {
            // TODO: Handle urgent trigger send error
            TODO("Handle urgent trigger send error")
        }
    }
    
    @PostMapping("/bulk-trigger")
    fun triggerBulkNotifications(
        @RequestBody triggers: List<NotificationTrigger>
    ): ResponseEntity<Map<String, Any>> {
        return try {
            // TODO: Send bulk notification triggers
            // HINT: Send multiple triggers for batch fan-out processing
            TODO("Send bulk notification triggers")
        } catch (e: Exception) {
            // TODO: Handle bulk trigger send error
            TODO("Handle bulk trigger send error")
        }
    }
    
    @GetMapping("/stats")
    fun getFanoutStats(): ResponseEntity<Map<String, Any>> {
        return try {
            // TODO: Get fan-out processing statistics
            // HINT: Return metrics about notifications sent to different channels
            TODO("Get fan-out statistics")
        } catch (e: Exception) {
            // TODO: Handle stats retrieval error
            TODO("Handle stats error")
        }
    }
}