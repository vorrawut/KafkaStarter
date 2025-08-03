package com.learning.KafkaStarter.service

import com.learning.KafkaStarter.model.UserActivity
import io.micrometer.core.instrument.Counter
import io.micrometer.core.instrument.Gauge
import io.micrometer.core.instrument.MeterRegistry
import org.apache.kafka.clients.consumer.ConsumerRebalanceListener
import org.apache.kafka.common.TopicPartition
import org.slf4j.LoggerFactory
import org.slf4j.MDC
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.kafka.listener.ConsumerSeekAware
import org.springframework.kafka.support.Acknowledgment
import org.springframework.kafka.support.KafkaHeaders
import org.springframework.messaging.handler.annotation.Header
import org.springframework.messaging.handler.annotation.Payload
import org.springframework.stereotype.Service
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong

@Service
class PartitionTrackingConsumer(
    private val meterRegistry: MeterRegistry
) : ConsumerSeekAware {
    
    private val logger = LoggerFactory.getLogger(PartitionTrackingConsumer::class.java)
    
    private val messagesProcessedCounter = Counter.builder("kafka.messages.processed")
        .description("Total messages processed by consumer")
        .register(meterRegistry)
    
    private val partitionProcessingCounter = Counter.builder("kafka.partition.processed")
        .description("Messages processed by partition")
        .register(meterRegistry)
    
    // Track partition assignments and offsets
    private val partitionAssignments = ConcurrentHashMap<Int, String>()
    private val lastProcessedOffsets = ConcurrentHashMap<Int, AtomicLong>()
    private val partitionMessageCounts = ConcurrentHashMap<Int, AtomicLong>()
    
    init {
        // Register partition-specific gauges
        Gauge.builder("kafka.consumer.assigned.partitions")
            .description("Number of assigned partitions")
            .register(meterRegistry) { partitionAssignments.size.toDouble() }
    }
    
    @KafkaListener(
        topics = ["user-activities"],
        groupId = "lesson4-partition-group",
        containerFactory = "kafkaListenerContainerFactory"
    )
    fun handleUserActivity(
        @Payload activity: UserActivity,
        @Header(KafkaHeaders.RECEIVED_TOPIC) topic: String,
        @Header(KafkaHeaders.RECEIVED_PARTITION) partition: Int,
        @Header(KafkaHeaders.OFFSET) offset: Long,
        @Header(KafkaHeaders.CONSUMER_ID, required = false) consumerId: String?,
        acknowledgment: Acknowledgment
    ) {
        // Set MDC for structured logging
        MDC.put("partition", partition.toString())
        MDC.put("offset", offset.toString())
        MDC.put("userId", activity.userId)
        
        logger.info(
            "Processing activity: userId={}, type={}, partition={}, offset={}",
            activity.userId, activity.activityType, partition, offset
        )
        
        try {
            // Simulate business logic processing
            processActivity(activity, partition, offset)
            
            // Update tracking metrics
            lastProcessedOffsets.computeIfAbsent(partition) { AtomicLong(0) }.set(offset)
            partitionMessageCounts.computeIfAbsent(partition) { AtomicLong(0) }.incrementAndGet()
            
            // Record metrics
            messagesProcessedCounter.increment()
            partitionProcessingCounter.increment("partition", partition.toString())
            
            // Manually acknowledge after successful processing
            acknowledgment.acknowledge()
            
            logger.debug("Successfully processed message at partition {} offset {}", partition, offset)
            
        } catch (retryableException: Exception) {
            logger.warn("Retryable error processing message: ${retryableException.message}")
            // Don't acknowledge - message will be retried
            throw retryableException
            
        } catch (poisonException: Exception) {
            logger.error("Poison message detected, skipping: ${poisonException.message}")
            // Acknowledge to skip poison message
            acknowledgment.acknowledge()
            
        } finally {
            MDC.clear()
        }
    }
    
    private fun processActivity(activity: UserActivity, partition: Int, offset: Long) {
        // Simulate processing time based on activity type
        val processingTime = when (activity.activityType) {
            com.learning.KafkaStarter.model.ActivityType.PURCHASE -> 200L // Slower for purchases
            com.learning.KafkaStarter.model.ActivityType.LOGIN -> 50L    // Fast for auth
            else -> 100L // Default processing time
        }
        
        Thread.sleep(processingTime)
        
        // Log processing details
        logger.debug(
            "Processed {} activity for user {} in {}ms",
            activity.activityType, activity.userId, processingTime
        )
    }
    
    // Custom rebalance listener to track partition assignments
    inner class PartitionRebalanceListener : ConsumerRebalanceListener {
        
        override fun onPartitionsRevoked(partitions: Collection<TopicPartition>) {
            logger.info("Partitions revoked: {}", partitions.map { "${it.topic()}-${it.partition()}" })
            
            partitions.forEach { topicPartition ->
                val partition = topicPartition.partition()
                partitionAssignments.remove(partition)
                logger.info("Released partition {}, last processed offset: {}", 
                    partition, lastProcessedOffsets[partition]?.get() ?: "unknown")
            }
        }
        
        override fun onPartitionsAssigned(partitions: Collection<TopicPartition>) {
            logger.info("Partitions assigned: {}", partitions.map { "${it.topic()}-${it.partition()}" })
            
            partitions.forEach { topicPartition ->
                val partition = topicPartition.partition()
                partitionAssignments[partition] = "ASSIGNED"
                lastProcessedOffsets.computeIfAbsent(partition) { AtomicLong(0) }
                partitionMessageCounts.computeIfAbsent(partition) { AtomicLong(0) }
                
                logger.info("Assigned to partition {}", partition)
            }
        }
    }
    
    // Expose partition tracking information
    fun getPartitionAssignments(): Map<Int, String> = partitionAssignments.toMap()
    
    fun getLastProcessedOffsets(): Map<Int, Long> = 
        lastProcessedOffsets.mapValues { it.value.get() }
    
    fun getPartitionMessageCounts(): Map<Int, Long> = 
        partitionMessageCounts.mapValues { it.value.get() }
    
    fun getPartitionProcessingStats(): Map<String, Any> {
        return mapOf(
            "assignedPartitions" to partitionAssignments.keys.sorted(),
            "totalMessagesProcessed" to partitionMessageCounts.values.sumOf { it.get() },
            "partitionDistribution" to partitionMessageCounts.mapValues { it.value.get() },
            "lastProcessedOffsets" to getLastProcessedOffsets()
        )
    }
    
    // Reset consumer to specific offsets (useful for replay scenarios)
    fun resetToOffset(partition: Int, offset: Long) {
        logger.info("Resetting partition {} to offset {}", partition, offset)
        // This would typically be handled through the ConsumerSeekAware interface
        // in a real implementation
    }
}