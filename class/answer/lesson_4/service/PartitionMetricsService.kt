package com.learning.KafkaStarter.service

import org.apache.kafka.clients.admin.AdminClient
import org.apache.kafka.clients.consumer.OffsetAndMetadata
import org.apache.kafka.common.TopicPartition
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.util.concurrent.TimeUnit

data class PartitionHealth(
    val partition: Int,
    val status: String,
    val consumerLag: Long,
    val isHealthy: Boolean,
    val issues: List<String>
)

@Service
class PartitionMetricsService(
    private val adminClient: AdminClient
) {
    
    private val logger = LoggerFactory.getLogger(PartitionMetricsService::class.java)
    private val lagThreshold = 100L // Configurable threshold for unhealthy lag
    
    fun getConsumerLagByPartition(groupId: String, topic: String): Map<Int, Long> {
        return try {
            // Get consumer group offsets
            val groupOffsets = adminClient.listConsumerGroupOffsets(groupId)
                .partitionsToOffsetAndMetadata()
                .get(10, TimeUnit.SECONDS)
            
            // Get latest offsets for the topic
            val topicPartitions = groupOffsets.keys.filter { it.topic() == topic }
            val latestOffsets = adminClient.listOffsets(
                topicPartitions.associateWith { 
                    org.apache.kafka.clients.admin.OffsetSpec.latest() 
                }
            ).all().get(10, TimeUnit.SECONDS)
            
            // Calculate lag per partition
            topicPartitions.associate { topicPartition ->
                val partition = topicPartition.partition()
                val consumerOffset = groupOffsets[topicPartition]?.offset() ?: 0L
                val latestOffset = latestOffsets[topicPartition]?.offset() ?: 0L
                val lag = latestOffset - consumerOffset
                
                partition to maxOf(0L, lag) // Ensure lag is not negative
            }
            
        } catch (e: Exception) {
            logger.error("Failed to get consumer lag for group $groupId topic $topic", e)
            emptyMap()
        }
    }
    
    fun getPartitionMessageCount(topic: String): Map<Int, Long> {
        return try {
            // Get topic partition info
            val topicDescription = adminClient.describeTopics(listOf(topic))
                .all()
                .get(10, TimeUnit.SECONDS)[topic]
            
            if (topicDescription == null) {
                logger.warn("Topic $topic not found")
                return emptyMap()
            }
            
            // Get earliest and latest offsets for each partition
            val topicPartitions = topicDescription.partitions().map { 
                TopicPartition(topic, it.partition()) 
            }
            
            val earliestOffsets = adminClient.listOffsets(
                topicPartitions.associateWith { 
                    org.apache.kafka.clients.admin.OffsetSpec.earliest() 
                }
            ).all().get(10, TimeUnit.SECONDS)
            
            val latestOffsets = adminClient.listOffsets(
                topicPartitions.associateWith { 
                    org.apache.kafka.clients.admin.OffsetSpec.latest() 
                }
            ).all().get(10, TimeUnit.SECONDS)
            
            // Calculate message count per partition
            topicPartitions.associate { topicPartition ->
                val partition = topicPartition.partition()
                val earliest = earliestOffsets[topicPartition]?.offset() ?: 0L
                val latest = latestOffsets[topicPartition]?.offset() ?: 0L
                val messageCount = latest - earliest
                
                partition to messageCount
            }
            
        } catch (e: Exception) {
            logger.error("Failed to get message count for topic $topic", e)
            emptyMap()
        }
    }
    
    fun getPartitionLeaderDistribution(topic: String): Map<Int, String> {
        return try {
            val topicDescription = adminClient.describeTopics(listOf(topic))
                .all()
                .get(10, TimeUnit.SECONDS)[topic]
            
            topicDescription?.partitions()?.associate { partition ->
                partition.partition() to (partition.leader()?.host() ?: "unknown")
            } ?: emptyMap()
            
        } catch (e: Exception) {
            logger.error("Failed to get partition leader distribution for topic $topic", e)
            emptyMap()
        }
    }
    
    fun getPartitionHealthStatus(topic: String): Map<Int, PartitionHealth> {
        return try {
            val consumerLag = getConsumerLagByPartition("lesson4-partition-group", topic)
            val messageCounts = getPartitionMessageCount(topic)
            val leaderDistribution = getPartitionLeaderDistribution(topic)
            
            consumerLag.map { (partition, lag) ->
                val issues = mutableListOf<String>()
                var status = "HEALTHY"
                
                // Check for high consumer lag
                if (lag > lagThreshold) {
                    issues.add("High consumer lag: $lag messages")
                    status = "WARNING"
                }
                
                // Check for missing leader
                if (leaderDistribution[partition] == "unknown") {
                    issues.add("No leader assigned")
                    status = "CRITICAL"
                }
                
                // Check for no messages
                val messageCount = messageCounts[partition] ?: 0L
                if (messageCount == 0L) {
                    issues.add("No messages in partition")
                }
                
                partition to PartitionHealth(
                    partition = partition,
                    status = status,
                    consumerLag = lag,
                    isHealthy = status == "HEALTHY",
                    issues = issues
                )
            }.toMap()
            
        } catch (e: Exception) {
            logger.error("Failed to get partition health status for topic $topic", e)
            emptyMap()
        }
    }
    
    fun getTopicOverallHealth(topic: String): Map<String, Any> {
        val partitionHealth = getPartitionHealthStatus(topic)
        val totalPartitions = partitionHealth.size
        val healthyPartitions = partitionHealth.values.count { it.isHealthy }
        val totalLag = partitionHealth.values.sumOf { it.consumerLag }
        val avgLag = if (totalPartitions > 0) totalLag / totalPartitions else 0L
        
        val overallStatus = when {
            healthyPartitions == totalPartitions -> "HEALTHY"
            healthyPartitions >= totalPartitions * 0.8 -> "WARNING"
            else -> "CRITICAL"
        }
        
        return mapOf(
            "overallStatus" to overallStatus,
            "totalPartitions" to totalPartitions,
            "healthyPartitions" to healthyPartitions,
            "unhealthyPartitions" to (totalPartitions - healthyPartitions),
            "totalLag" to totalLag,
            "averageLag" to avgLag,
            "partitionHealth" to partitionHealth
        )
    }
    
    fun getConsumerGroupMetrics(groupId: String): Map<String, Any> {
        return try {
            val groupDescription = adminClient.describeConsumerGroups(listOf(groupId))
                .all()
                .get(10, TimeUnit.SECONDS)[groupId]
            
            val groupOffsets = adminClient.listConsumerGroupOffsets(groupId)
                .partitionsToOffsetAndMetadata()
                .get(10, TimeUnit.SECONDS)
            
            mapOf(
                "groupId" to groupId,
                "state" to (groupDescription?.state()?.toString() ?: "UNKNOWN"),
                "memberCount" to (groupDescription?.members()?.size ?: 0),
                "assignedPartitions" to groupOffsets.keys.map { "${it.topic()}-${it.partition()}" },
                "totalAssignedPartitions" to groupOffsets.size
            )
            
        } catch (e: Exception) {
            logger.error("Failed to get consumer group metrics for $groupId", e)
            mapOf(
                "groupId" to groupId,
                "error" to e.message
            )
        }
    }
    
    fun monitorPartitionBalance(topic: String): Map<String, Any> {
        val messageCounts = getPartitionMessageCount(topic)
        
        if (messageCounts.isEmpty()) {
            return mapOf("error" to "No partition data available")
        }
        
        val totalMessages = messageCounts.values.sum()
        val avgMessages = totalMessages / messageCounts.size
        val maxMessages = messageCounts.values.maxOrNull() ?: 0L
        val minMessages = messageCounts.values.minOrNull() ?: 0L
        
        // Calculate balance ratio (0.0 = perfectly balanced, 1.0 = completely unbalanced)
        val balanceRatio = if (maxMessages > 0) {
            (maxMessages - minMessages).toDouble() / maxMessages
        } else 0.0
        
        val isBalanced = balanceRatio < 0.3 // Consider balanced if difference is less than 30%
        
        return mapOf(
            "isBalanced" to isBalanced,
            "balanceRatio" to balanceRatio,
            "totalMessages" to totalMessages,
            "averageMessages" to avgMessages,
            "maxMessages" to maxMessages,
            "minMessages" to minMessages,
            "partitionDistribution" to messageCounts,
            "recommendation" to if (!isBalanced) "Consider reviewing partition key strategy" else "Partition distribution looks good"
        )
    }
}