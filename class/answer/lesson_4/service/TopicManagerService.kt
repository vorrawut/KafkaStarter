package com.learning.KafkaStarter.service

import org.apache.kafka.clients.admin.AdminClient
import org.apache.kafka.clients.admin.NewTopic
import org.apache.kafka.clients.admin.TopicDescription
import org.apache.kafka.common.config.TopicConfig
import org.slf4j.LoggerFactory
import org.springframework.kafka.core.KafkaAdmin
import org.springframework.stereotype.Service
import java.util.concurrent.TimeUnit

data class TopicDetails(
    val name: String,
    val partitions: Int,
    val replicationFactor: Short,
    val configs: Map<String, String>,
    val partitionInfo: List<PartitionInfo>
)

data class PartitionInfo(
    val partition: Int,
    val leader: String,
    val replicas: List<String>,
    val inSyncReplicas: List<String>
)

@Service
class TopicManagerService(
    private val kafkaAdmin: KafkaAdmin,
    private val adminClient: AdminClient
) {
    
    private val logger = LoggerFactory.getLogger(TopicManagerService::class.java)
    
    fun createTopic(
        name: String, 
        partitions: Int, 
        replicationFactor: Short, 
        configs: Map<String, String> = emptyMap()
    ): Boolean {
        logger.info("Creating topic: $name with $partitions partitions")
        
        return try {
            val topic = NewTopic(name, partitions, replicationFactor).apply {
                configs(configs)
            }
            
            val result = adminClient.createTopics(listOf(topic))
            result.all().get(30, TimeUnit.SECONDS)
            
            logger.info("Successfully created topic: $name")
            true
            
        } catch (e: Exception) {
            logger.error("Failed to create topic: $name", e)
            false
        }
    }
    
    fun getTopicDetails(topicName: String): TopicDetails? {
        return try {
            val descriptions = adminClient.describeTopics(listOf(topicName))
                .all()
                .get(10, TimeUnit.SECONDS)
            
            val description = descriptions[topicName] ?: return null
            
            val configs = adminClient.describeConfigs(
                listOf(org.apache.kafka.common.config.ConfigResource(
                    org.apache.kafka.common.config.ConfigResource.Type.TOPIC, 
                    topicName
                ))
            ).all().get(10, TimeUnit.SECONDS)
            
            val topicConfigs = configs.values.firstOrNull()?.entries()?.associate { 
                it.name() to it.value() 
            } ?: emptyMap()
            
            TopicDetails(
                name = description.name(),
                partitions = description.partitions().size,
                replicationFactor = description.partitions().firstOrNull()?.replicas()?.size?.toShort() ?: 1,
                configs = topicConfigs,
                partitionInfo = description.partitions().map { partition ->
                    PartitionInfo(
                        partition = partition.partition(),
                        leader = partition.leader()?.host() ?: "unknown",
                        replicas = partition.replicas().map { it.host() },
                        inSyncReplicas = partition.isr().map { it.host() }
                    )
                }
            )
            
        } catch (e: Exception) {
            logger.error("Failed to get topic details for: $topicName", e)
            null
        }
    }
    
    fun listTopics(): List<String> {
        return try {
            adminClient.listTopics()
                .names()
                .get(10, TimeUnit.SECONDS)
                .filter { !it.startsWith("_") } // Filter out internal topics
                .sorted()
                
        } catch (e: Exception) {
            logger.error("Failed to list topics", e)
            emptyList()
        }
    }
    
    fun deleteTopics(topicNames: List<String>): Boolean {
        logger.info("Deleting topics: $topicNames")
        
        return try {
            val result = adminClient.deleteTopics(topicNames)
            result.all().get(30, TimeUnit.SECONDS)
            
            logger.info("Successfully deleted topics: $topicNames")
            true
            
        } catch (e: Exception) {
            logger.error("Failed to delete topics: $topicNames", e)
            false
        }
    }
    
    fun updateTopicPartitions(topicName: String, partitions: Int): Boolean {
        logger.info("Updating topic $topicName to $partitions partitions")
        
        return try {
            val partitionsMap = mapOf(topicName to org.apache.kafka.clients.admin.NewPartitions.increaseTo(partitions))
            val result = adminClient.createPartitions(partitionsMap)
            result.all().get(30, TimeUnit.SECONDS)
            
            logger.info("Successfully updated partitions for topic: $topicName")
            true
            
        } catch (e: Exception) {
            logger.error("Failed to update partitions for topic: $topicName", e)
            false
        }
    }
    
    fun createTopicWithDefaults(name: String): Boolean {
        val defaultConfigs = mapOf(
            TopicConfig.RETENTION_MS_CONFIG to "604800000", // 7 days
            TopicConfig.CLEANUP_POLICY_CONFIG to TopicConfig.CLEANUP_POLICY_DELETE,
            TopicConfig.COMPRESSION_TYPE_CONFIG to "snappy"
        )
        
        return createTopic(name, 3, 1, defaultConfigs)
    }
}