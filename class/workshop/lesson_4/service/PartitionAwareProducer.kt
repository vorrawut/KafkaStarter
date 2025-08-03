package com.learning.KafkaStarter.service

import com.learning.KafkaStarter.model.UserActivity
import org.springframework.stereotype.Service

// TODO: Add @Service annotation
// TODO: Inject KafkaTemplate
// TODO: Create partition-aware message sending methods

@Service
class PartitionAwareProducer(
    // TODO: Inject KafkaTemplate<String, UserActivity>
) {
    
    // TODO: Implement sendWithKey(topic: String, key: String, activity: UserActivity)
    // TODO: Implement sendWithCustomPartitioner(topic: String, activity: UserActivity)
    // TODO: Implement sendRoundRobin(topic: String, activity: UserActivity)
    // TODO: Add callback handling to track partition assignments
    
    // TODO: Create method to demonstrate key distribution across partitions
    // TODO: Add metrics tracking for partition usage
    
    // HINT: Use kafkaTemplate.send(topic, key, value) for key-based partitioning
    // HINT: Use kafkaTemplate.send(topic, partition, key, value) for explicit partition
    // HINT: Implement callback with .thenAccept() to capture RecordMetadata
}