package com.learning.KafkaStarter.service

import org.springframework.stereotype.Service

// TODO: Add @Service annotation
// TODO: Inject KafkaAdmin for topic management
// TODO: Create methods for topic lifecycle management

@Service 
class TopicManagerService(
    // TODO: Inject KafkaAdmin
    // TODO: Inject AdminClient for advanced operations
) {
    
    // TODO: Implement createTopic(name: String, partitions: Int, replicationFactor: Short, configs: Map<String, String>)
    // TODO: Implement getTopicDetails(topicName: String): TopicDetails
    // TODO: Implement listTopics(): List<String>
    // TODO: Implement deleteTopics(topicNames: List<String>)
    // TODO: Implement updateTopicPartitions(topicName: String, partitions: Int)
    
    // HINT: Use NewTopic.builder() for topic creation
    // HINT: AdminClient.describeTopics() for topic inspection
    // HINT: Consider error handling for topic operations
}