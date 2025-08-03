package com.learning.KafkaStarter.service

import com.learning.KafkaStarter.model.UserActivity
import io.micrometer.core.instrument.Counter
import io.micrometer.core.instrument.MeterRegistry
import org.slf4j.LoggerFactory
import org.slf4j.MDC
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.stereotype.Service
import java.util.concurrent.ThreadLocalRandom

@Service
class PartitionAwareProducer(
    private val kafkaTemplate: KafkaTemplate<String, Any>,
    private val meterRegistry: MeterRegistry
) {
    
    private val logger = LoggerFactory.getLogger(PartitionAwareProducer::class.java)
    
    private val messagesSentCounter = Counter.builder("kafka.messages.sent")
        .description("Total messages sent to Kafka")
        .register(meterRegistry)
    
    private val partitionDistributionCounter = Counter.builder("kafka.partition.distribution")
        .description("Message distribution across partitions")
        .register(meterRegistry)
    
    fun sendWithKey(topic: String, key: String, activity: UserActivity) {
        logger.info("Sending message with key '$key' to topic '$topic'")
        
        kafkaTemplate.send(topic, key, activity)
            .thenAccept { result ->
                val metadata = result.recordMetadata
                val partition = metadata.partition()
                val offset = metadata.offset()
                
                // Add partition info to MDC for logging
                MDC.put("partition", partition.toString())
                MDC.put("offset", offset.toString())
                
                logger.info(
                    "Message sent successfully: topic={}, partition={}, offset={}, key={}",
                    topic, partition, offset, key
                )
                
                // Record metrics
                messagesSentCounter.increment()
                partitionDistributionCounter.increment("partition", partition.toString())
                
                MDC.clear()
            }
            .exceptionally { failure ->
                logger.error("Failed to send message with key '$key' to topic '$topic'", failure)
                null
            }
    }
    
    fun sendWithCustomPartitioner(topic: String, activity: UserActivity) {
        // Custom partitioning based on activity type
        val partition = when (activity.activityType.name) {
            "LOGIN", "LOGOUT" -> 0  // Authentication events to partition 0
            "PURCHASE" -> 1         // Purchase events to partition 1
            else -> 2               // Other events to partition 2
        }
        
        logger.info("Sending message to custom partition $partition based on activity type: ${activity.activityType}")
        
        kafkaTemplate.send(topic, partition, activity.userId, activity)
            .thenAccept { result ->
                val metadata = result.recordMetadata
                logger.info(
                    "Custom partition message sent: topic={}, partition={}, offset={}, activityType={}",
                    topic, metadata.partition(), metadata.offset(), activity.activityType
                )
                
                messagesSentCounter.increment()
                partitionDistributionCounter.increment("partition", metadata.partition().toString())
            }
            .exceptionally { failure ->
                logger.error("Failed to send message with custom partitioner", failure)
                null
            }
    }
    
    fun sendRoundRobin(topic: String, activity: UserActivity) {
        // Send without key for round-robin distribution
        logger.info("Sending message with round-robin distribution")
        
        kafkaTemplate.send(topic, activity)
            .thenAccept { result ->
                val metadata = result.recordMetadata
                logger.info(
                    "Round-robin message sent: topic={}, partition={}, offset={}",
                    topic, metadata.partition(), metadata.offset()
                )
                
                messagesSentCounter.increment()
                partitionDistributionCounter.increment("partition", metadata.partition().toString())
            }
            .exceptionally { failure ->
                logger.error("Failed to send round-robin message", failure)
                null
            }
    }
    
    fun demonstrateKeyDistribution(topic: String, userIds: List<String>) {
        logger.info("Demonstrating key distribution across partitions for ${userIds.size} users")
        
        val partitionDistribution = mutableMapOf<Int, MutableList<String>>()
        
        userIds.forEach { userId ->
            val activity = UserActivity(
                userId = userId,
                activityType = com.learning.KafkaStarter.model.ActivityType.LOGIN,
                metadata = mapOf("demo" to "key-distribution")
            )
            
            kafkaTemplate.send(topic, userId, activity)
                .thenAccept { result ->
                    val partition = result.recordMetadata.partition()
                    partitionDistribution.computeIfAbsent(partition) { mutableListOf() }.add(userId)
                    
                    logger.debug("User $userId assigned to partition $partition")
                }
        }
        
        // Log distribution summary after a delay
        Thread.sleep(1000) // Simple delay to let async operations complete
        
        partitionDistribution.forEach { (partition, users) ->
            logger.info("Partition $partition: ${users.size} users: $users")
        }
    }
    
    fun sendBatchWithMetrics(topic: String, activities: List<UserActivity>) {
        logger.info("Sending batch of ${activities.size} activities")
        
        val startTime = System.currentTimeMillis()
        var successCount = 0
        var failureCount = 0
        
        activities.forEach { activity ->
            kafkaTemplate.send(topic, activity.userId, activity)
                .thenAccept { 
                    successCount++
                    messagesSentCounter.increment()
                }
                .exceptionally { 
                    failureCount++
                    null
                }
        }
        
        // Simple delay for async completion
        Thread.sleep(500)
        
        val duration = System.currentTimeMillis() - startTime
        logger.info(
            "Batch sending completed: {} success, {} failures, {}ms duration",
            successCount, failureCount, duration
        )
    }
    
    fun calculatePartitionForKey(key: String, partitionCount: Int): Int {
        // Same calculation as Kafka's default partitioner
        return Math.abs(key.hashCode()) % partitionCount
    }
}