package com.learning.KafkaStarter.controller

import com.learning.KafkaStarter.service.TopicManagerService
import com.learning.KafkaStarter.service.PartitionMetricsService
import jakarta.validation.Valid
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

data class CreateTopicRequest(
    @field:NotBlank(message = "Topic name cannot be blank")
    val topicName: String,
    
    @field:Min(value = 1, message = "Partitions must be at least 1")
    val partitions: Int,
    
    @field:Min(value = 1, message = "Replication factor must be at least 1")
    val replicationFactor: Short = 1,
    
    val configs: Map<String, String> = emptyMap()
)

data class TopicDetailsResponse(
    val name: String,
    val partitions: Int,
    val replicationFactor: Short,
    val configs: Map<String, String>,
    val partitionInfo: List<PartitionInfoResponse>
)

data class PartitionInfoResponse(
    val partition: Int,
    val leader: String,
    val replicas: List<String>,
    val inSyncReplicas: List<String>,
    val messageCount: Long? = null,
    val consumerLag: Long? = null
)

data class PartitionScaleRequest(
    @field:Min(value = 1, message = "Partitions must be at least 1")
    val newPartitionCount: Int
)

data class ApiResponse<T>(
    val success: Boolean,
    val message: String,
    val data: T? = null
)

@RestController
@RequestMapping("/api/topics")
class TopicAdminController(
    private val topicManagerService: TopicManagerService,
    private val partitionMetricsService: PartitionMetricsService
) {
    
    private val logger = LoggerFactory.getLogger(TopicAdminController::class.java)
    
    @PostMapping
    fun createTopic(@Valid @RequestBody request: CreateTopicRequest): ResponseEntity<ApiResponse<String>> {
        logger.info("Creating topic: ${request.topicName} with ${request.partitions} partitions")
        
        return try {
            val success = topicManagerService.createTopic(
                name = request.topicName,
                partitions = request.partitions,
                replicationFactor = request.replicationFactor,
                configs = request.configs
            )
            
            if (success) {
                ResponseEntity.ok(
                    ApiResponse(
                        success = true,
                        message = "Topic '${request.topicName}' created successfully",
                        data = request.topicName
                    )
                )
            } else {
                ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    ApiResponse(
                        success = false,
                        message = "Failed to create topic '${request.topicName}'"
                    )
                )
            }
            
        } catch (e: Exception) {
            logger.error("Error creating topic: ${request.topicName}", e)
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse(
                    success = false,
                    message = "Error creating topic: ${e.message}"
                )
            )
        }
    }
    
    @GetMapping
    fun listTopics(): ResponseEntity<ApiResponse<List<String>>> {
        logger.info("Listing all topics")
        
        return try {
            val topics = topicManagerService.listTopics()
            ResponseEntity.ok(
                ApiResponse(
                    success = true,
                    message = "Retrieved ${topics.size} topics",
                    data = topics
                )
            )
            
        } catch (e: Exception) {
            logger.error("Error listing topics", e)
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse(
                    success = false,
                    message = "Error listing topics: ${e.message}"
                )
            )
        }
    }
    
    @GetMapping("/{topicName}")
    fun getTopicDetails(@PathVariable topicName: String): ResponseEntity<ApiResponse<TopicDetailsResponse>> {
        logger.info("Getting details for topic: $topicName")
        
        return try {
            val details = topicManagerService.getTopicDetails(topicName)
            
            if (details != null) {
                // Enrich with metrics data
                val consumerLag = partitionMetricsService.getConsumerLagByPartition("lesson4-partition-group", topicName)
                val messageCounts = partitionMetricsService.getPartitionMessageCount(topicName)
                
                val enrichedPartitionInfo = details.partitionInfo.map { partition ->
                    PartitionInfoResponse(
                        partition = partition.partition,
                        leader = partition.leader,
                        replicas = partition.replicas,
                        inSyncReplicas = partition.inSyncReplicas,
                        messageCount = messageCounts[partition.partition],
                        consumerLag = consumerLag[partition.partition]
                    )
                }
                
                val response = TopicDetailsResponse(
                    name = details.name,
                    partitions = details.partitions,
                    replicationFactor = details.replicationFactor,
                    configs = details.configs,
                    partitionInfo = enrichedPartitionInfo
                )
                
                ResponseEntity.ok(
                    ApiResponse(
                        success = true,
                        message = "Topic details retrieved successfully",
                        data = response
                    )
                )
            } else {
                ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                    ApiResponse(
                        success = false,
                        message = "Topic '$topicName' not found"
                    )
                )
            }
            
        } catch (e: Exception) {
            logger.error("Error getting topic details: $topicName", e)
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse(
                    success = false,
                    message = "Error getting topic details: ${e.message}"
                )
            )
        }
    }
    
    @DeleteMapping("/{topicName}")
    fun deleteTopic(@PathVariable topicName: String): ResponseEntity<ApiResponse<String>> {
        logger.info("Deleting topic: $topicName")
        
        return try {
            val success = topicManagerService.deleteTopics(listOf(topicName))
            
            if (success) {
                ResponseEntity.ok(
                    ApiResponse(
                        success = true,
                        message = "Topic '$topicName' deleted successfully",
                        data = topicName
                    )
                )
            } else {
                ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    ApiResponse(
                        success = false,
                        message = "Failed to delete topic '$topicName'"
                    )
                )
            }
            
        } catch (e: Exception) {
            logger.error("Error deleting topic: $topicName", e)
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse(
                    success = false,
                    message = "Error deleting topic: ${e.message}"
                )
            )
        }
    }
    
    @PutMapping("/{topicName}/partitions")
    fun scalePartitions(
        @PathVariable topicName: String,
        @Valid @RequestBody request: PartitionScaleRequest
    ): ResponseEntity<ApiResponse<String>> {
        logger.info("Scaling topic $topicName to ${request.newPartitionCount} partitions")
        
        return try {
            val success = topicManagerService.updateTopicPartitions(topicName, request.newPartitionCount)
            
            if (success) {
                ResponseEntity.ok(
                    ApiResponse(
                        success = true,
                        message = "Topic '$topicName' scaled to ${request.newPartitionCount} partitions",
                        data = topicName
                    )
                )
            } else {
                ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    ApiResponse(
                        success = false,
                        message = "Failed to scale topic '$topicName'"
                    )
                )
            }
            
        } catch (e: Exception) {
            logger.error("Error scaling topic: $topicName", e)
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse(
                    success = false,
                    message = "Error scaling topic: ${e.message}"
                )
            )
        }
    }
    
    @GetMapping("/{topicName}/metrics")
    fun getTopicMetrics(@PathVariable topicName: String): ResponseEntity<ApiResponse<Map<String, Any>>> {
        logger.info("Getting metrics for topic: $topicName")
        
        return try {
            val metrics = mapOf(
                "consumerLag" to partitionMetricsService.getConsumerLagByPartition("lesson4-partition-group", topicName),
                "messageCounts" to partitionMetricsService.getPartitionMessageCount(topicName),
                "leaderDistribution" to partitionMetricsService.getPartitionLeaderDistribution(topicName),
                "healthStatus" to partitionMetricsService.getPartitionHealthStatus(topicName)
            )
            
            ResponseEntity.ok(
                ApiResponse(
                    success = true,
                    message = "Topic metrics retrieved successfully",
                    data = metrics
                )
            )
            
        } catch (e: Exception) {
            logger.error("Error getting topic metrics: $topicName", e)
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse(
                    success = false,
                    message = "Error getting topic metrics: ${e.message}"
                )
            )
        }
    }
    
    @PostMapping("/quick-create/{topicName}")
    fun quickCreateTopic(@PathVariable topicName: String): ResponseEntity<ApiResponse<String>> {
        logger.info("Quick creating topic with defaults: $topicName")
        
        return try {
            val success = topicManagerService.createTopicWithDefaults(topicName)
            
            if (success) {
                ResponseEntity.ok(
                    ApiResponse(
                        success = true,
                        message = "Topic '$topicName' created with default settings",
                        data = topicName
                    )
                )
            } else {
                ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    ApiResponse(
                        success = false,
                        message = "Failed to create topic '$topicName' with defaults"
                    )
                )
            }
            
        } catch (e: Exception) {
            logger.error("Error quick creating topic: $topicName", e)
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse(
                    success = false,
                    message = "Error creating topic: ${e.message}"
                )
            )
        }
    }
}