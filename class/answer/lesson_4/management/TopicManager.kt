package com.learning.KafkaStarter.management

import org.apache.kafka.clients.admin.*
import org.apache.kafka.common.config.ConfigResource
import org.apache.kafka.common.config.TopicConfig
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.kafka.core.KafkaAdmin
import org.springframework.stereotype.Service
import java.util.concurrent.TimeUnit

/**
 * Complete Answer: Topic Management Operations
 * 
 * This demonstrates comprehensive topic management operations
 * for creating, configuring, and monitoring Kafka topics.
 */

@Service
class TopicManager {
    
    @Autowired
    private lateinit var kafkaAdmin: KafkaAdmin
    
    private val logger = org.slf4j.LoggerFactory.getLogger(TopicManager::class.java)
    
    fun createTopicWithConfig(
        topicName: String,
        partitions: Int,
        replicationFactor: Short,
        retentionMs: Long = 86400000, // 24 hours default
        compressionType: String = "lz4"
    ): TopicCreationResult {
        return try {
            logger.info("Creating topic: $topicName with $partitions partitions")
            
            AdminClient.create(kafkaAdmin.configurationProperties).use { adminClient ->
                // Create topic configuration
                val topicConfigs = mapOf(
                    TopicConfig.RETENTION_MS_CONFIG to retentionMs.toString(),
                    TopicConfig.COMPRESSION_TYPE_CONFIG to compressionType,
                    TopicConfig.CLEANUP_POLICY_CONFIG to TopicConfig.CLEANUP_POLICY_DELETE,
                    TopicConfig.MIN_IN_SYNC_REPLICAS_CONFIG to "1"
                )
                
                // Create NewTopic with configuration
                val newTopic = NewTopic(topicName, partitions, replicationFactor)
                newTopic.configs(topicConfigs)
                
                // Execute topic creation
                val result = adminClient.createTopics(listOf(newTopic))
                result.all().get(10, TimeUnit.SECONDS)
                
                logger.info("Successfully created topic: $topicName")
                
                TopicCreationResult(
                    topicName = topicName,
                    success = true,
                    message = "Topic created successfully",
                    partitions = partitions,
                    replicationFactor = replicationFactor.toInt(),
                    configuration = topicConfigs
                )
            }
        } catch (e: Exception) {
            logger.error("Failed to create topic: $topicName", e)
            TopicCreationResult(
                topicName = topicName,
                success = false,
                message = "Failed to create topic: ${e.message}",
                partitions = 0,
                replicationFactor = 0,
                configuration = emptyMap()
            )
        }
    }
    
    fun listTopicsWithDetails(): List<TopicInfo> {
        return try {
            logger.info("Listing topics with details")
            
            AdminClient.create(kafkaAdmin.configurationProperties).use { adminClient ->
                // Get topic names
                val topicNames = adminClient.listTopics().names().get(5, TimeUnit.SECONDS)
                
                if (topicNames.isEmpty()) {
                    return emptyList()
                }
                
                // Get topic descriptions
                val topicDescriptions = adminClient.describeTopics(topicNames)
                    .allTopicNames().get(5, TimeUnit.SECONDS)
                
                // Get topic configurations
                val configResources = topicNames.map { ConfigResource(ConfigResource.Type.TOPIC, it) }
                val topicConfigs = adminClient.describeConfigs(configResources)
                    .all().get(5, TimeUnit.SECONDS)
                
                // Build topic info list
                topicNames.map { topicName ->
                    val description = topicDescriptions[topicName]!!
                    val config = topicConfigs[ConfigResource(ConfigResource.Type.TOPIC, topicName)]!!
                    
                    TopicInfo(
                        name = topicName,
                        partitions = description.partitions().size,
                        replicationFactor = description.partitions().firstOrNull()?.replicas()?.size ?: 0,
                        configuration = config.entries().associate { it.name() to it.value() },
                        isInternal = description.isInternal
                    )
                }
            }
        } catch (e: Exception) {
            logger.error("Failed to list topics", e)
            emptyList()
        }
    }
    
    fun deleteTopic(topicName: String): TopicDeletionResult {
        return try {
            logger.info("Deleting topic: $topicName")
            
            AdminClient.create(kafkaAdmin.configurationProperties).use { adminClient ->
                // Check if topic exists first
                val existingTopics = adminClient.listTopics().names().get(5, TimeUnit.SECONDS)
                
                if (!existingTopics.contains(topicName)) {
                    return TopicDeletionResult(
                        topicName = topicName,
                        success = false,
                        message = "Topic does not exist"
                    )
                }
                
                // Delete topic
                val result = adminClient.deleteTopics(listOf(topicName))
                result.all().get(10, TimeUnit.SECONDS)
                
                logger.info("Successfully deleted topic: $topicName")
                
                TopicDeletionResult(
                    topicName = topicName,
                    success = true,
                    message = "Topic deleted successfully"
                )
            }
        } catch (e: Exception) {
            logger.error("Failed to delete topic: $topicName", e)
            TopicDeletionResult(
                topicName = topicName,
                success = false,
                message = "Failed to delete topic: ${e.message}"
            )
        }
    }
    
    fun updateTopicConfig(
        topicName: String,
        configUpdates: Map<String, String>
    ): ConfigUpdateResult {
        return try {
            logger.info("Updating configuration for topic: $topicName")
            
            AdminClient.create(kafkaAdmin.configurationProperties).use { adminClient ->
                val configResource = ConfigResource(ConfigResource.Type.TOPIC, topicName)
                
                // Create configuration alterations
                val alterations = configUpdates.map { (key, value) ->
                    AlterConfigOp(ConfigEntry(key, value), AlterConfigOp.OpType.SET)
                }
                
                val alterConfigs = mapOf(configResource to alterations)
                
                // Execute configuration update
                val result = adminClient.incrementalAlterConfigs(alterConfigs)
                result.all().get(10, TimeUnit.SECONDS)
                
                logger.info("Successfully updated configuration for topic: $topicName")
                
                ConfigUpdateResult(
                    topicName = topicName,
                    success = true,
                    message = "Configuration updated successfully",
                    updatedConfigs = configUpdates
                )
            }
        } catch (e: Exception) {
            logger.error("Failed to update config for topic: $topicName", e)
            ConfigUpdateResult(
                topicName = topicName,
                success = false,
                message = "Failed to update config: ${e.message}",
                updatedConfigs = emptyMap()
            )
        }
    }
    
    fun increasePartitions(topicName: String, newPartitionCount: Int): PartitionUpdateResult {
        return try {
            logger.info("Increasing partitions for topic: $topicName to $newPartitionCount")
            
            AdminClient.create(kafkaAdmin.configurationProperties).use { adminClient ->
                // Get current partition count
                val topicDescription = adminClient.describeTopics(listOf(topicName))
                    .allTopicNames().get(5, TimeUnit.SECONDS)[topicName]!!
                
                val currentPartitionCount = topicDescription.partitions().size
                
                if (newPartitionCount <= currentPartitionCount) {
                    return PartitionUpdateResult(
                        topicName = topicName,
                        success = false,
                        message = "New partition count ($newPartitionCount) must be greater than current ($currentPartitionCount)",
                        oldPartitionCount = currentPartitionCount,
                        newPartitionCount = newPartitionCount
                    )
                }
                
                // Create partition increase request
                val partitionUpdate = mapOf(topicName to NewPartitions.increaseTo(newPartitionCount))
                
                // Execute partition increase
                val result = adminClient.createPartitions(partitionUpdate)
                result.all().get(10, TimeUnit.SECONDS)
                
                logger.info("Successfully increased partitions for topic: $topicName")
                
                PartitionUpdateResult(
                    topicName = topicName,
                    success = true,
                    message = "Partitions increased successfully",
                    oldPartitionCount = currentPartitionCount,
                    newPartitionCount = newPartitionCount
                )
            }
        } catch (e: Exception) {
            logger.error("Failed to increase partitions for topic: $topicName", e)
            PartitionUpdateResult(
                topicName = topicName,
                success = false,
                message = "Failed to increase partitions: ${e.message}",
                oldPartitionCount = 0,
                newPartitionCount = newPartitionCount
            )
        }
    }
    
    fun checkTopicHealth(topicName: String): TopicHealthCheck {
        return try {
            logger.info("Checking health for topic: $topicName")
            
            AdminClient.create(kafkaAdmin.configurationProperties).use { adminClient ->
                val issues = mutableListOf<String>()
                
                // Check if topic exists
                val existingTopics = adminClient.listTopics().names().get(5, TimeUnit.SECONDS)
                if (!existingTopics.contains(topicName)) {
                    return TopicHealthCheck(
                        topicName = topicName,
                        exists = false,
                        allPartitionsHealthy = false,
                        issues = listOf("Topic does not exist"),
                        partitionCount = 0,
                        replicationFactor = 0
                    )
                }
                
                // Get topic description
                val topicDescription = adminClient.describeTopics(listOf(topicName))
                    .allTopicNames().get(5, TimeUnit.SECONDS)[topicName]!!
                
                val partitionCount = topicDescription.partitions().size
                val replicationFactor = topicDescription.partitions().firstOrNull()?.replicas()?.size ?: 0
                
                // Check partition health
                topicDescription.partitions().forEach { partition ->
                    // Check if partition has a leader
                    if (partition.leader() == null) {
                        issues.add("Partition ${partition.partition()} has no leader")
                    }
                    
                    // Check for under-replicated partitions
                    val inSyncReplicas = partition.isr().size
                    val totalReplicas = partition.replicas().size
                    if (inSyncReplicas < totalReplicas) {
                        issues.add("Partition ${partition.partition()} is under-replicated ($inSyncReplicas/$totalReplicas in sync)")
                    }
                }
                
                logger.info("Health check completed for topic: $topicName")
                
                TopicHealthCheck(
                    topicName = topicName,
                    exists = true,
                    allPartitionsHealthy = issues.isEmpty(),
                    issues = issues,
                    partitionCount = partitionCount,
                    replicationFactor = replicationFactor
                )
            }
        } catch (e: Exception) {
            logger.error("Failed to check health for topic: $topicName", e)
            TopicHealthCheck(
                topicName = topicName,
                exists = false,
                allPartitionsHealthy = false,
                issues = listOf("Health check failed: ${e.message}"),
                partitionCount = 0,
                replicationFactor = 0
            )
        }
    }
    
    // Utility method for topic existence check
    fun topicExists(topicName: String): Boolean {
        return try {
            AdminClient.create(kafkaAdmin.configurationProperties).use { adminClient ->
                val existingTopics = adminClient.listTopics().names().get(5, TimeUnit.SECONDS)
                existingTopics.contains(topicName)
            }
        } catch (e: Exception) {
            logger.error("Failed to check topic existence: $topicName", e)
            false
        }
    }
    
    // Bulk topic operations
    fun createMultipleTopics(topicConfigs: List<TopicConfig>): List<TopicCreationResult> {
        return topicConfigs.map { config ->
            createTopicWithConfig(
                topicName = config.name,
                partitions = config.partitions,
                replicationFactor = config.replicationFactor,
                retentionMs = config.retentionMs,
                compressionType = config.compressionType
            )
        }
    }
}

// Additional data classes for bulk operations
data class TopicConfig(
    val name: String,
    val partitions: Int,
    val replicationFactor: Short,
    val retentionMs: Long = 86400000,
    val compressionType: String = "lz4"
)

// Existing data classes
data class TopicCreationResult(
    val topicName: String,
    val success: Boolean,
    val message: String,
    val partitions: Int,
    val replicationFactor: Int,
    val configuration: Map<String, String>
)

data class TopicInfo(
    val name: String,
    val partitions: Int,
    val replicationFactor: Int,
    val configuration: Map<String, String>,
    val isInternal: Boolean
)

data class TopicDeletionResult(
    val topicName: String,
    val success: Boolean,
    val message: String
)

data class ConfigUpdateResult(
    val topicName: String,
    val success: Boolean,
    val message: String,
    val updatedConfigs: Map<String, String>
)

data class PartitionUpdateResult(
    val topicName: String,
    val success: Boolean,
    val message: String,
    val oldPartitionCount: Int,
    val newPartitionCount: Int
)

data class TopicHealthCheck(
    val topicName: String,
    val exists: Boolean,
    val allPartitionsHealthy: Boolean,
    val issues: List<String>,
    val partitionCount: Int,
    val replicationFactor: Int
)