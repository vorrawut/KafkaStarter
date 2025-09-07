package com.learning.KafkaStarter.controller

import com.learning.KafkaStarter.service.TopicManagerService
import org.springframework.web.bind.annotation.*

// TODO: Add @RestController annotation
// TODO: Add @RequestMapping for topic admin endpoints
// TODO: Inject TopicManagerService

data class CreateTopicRequest(
    val topicName: String,
    val partitions: Int,
    val replicationFactor: Short = 1,
    val configs: Map<String, String> = emptyMap()
)

data class TopicDetailsResponse(
    val name: String,
    val partitions: Int,
    val replicationFactor: Short,
    val configs: Map<String, String>
)

class TopicAdminController(
    // TODO: Inject TopicManagerService
) {
    
    // TODO: Create @PostMapping("/topics") for topic creation
    // TODO: Create @GetMapping("/topics") for listing topics
    // TODO: Create @GetMapping("/topics/{name}") for topic details
    // TODO: Create @DeleteMapping("/topics/{name}") for topic deletion
    // TODO: Create @PutMapping("/topics/{name}/partitions") for partition scaling
    
    // HINT: Use @RequestBody for CreateTopicRequest
    // HINT: Use @PathVariable for topic name
    // HINT: Return appropriate HTTP status codes
}