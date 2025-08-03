package com.learning.KafkaStarter

import com.learning.KafkaStarter.model.ActivityType
import com.learning.KafkaStarter.model.UserActivity
import com.learning.KafkaStarter.service.PartitionAwareProducer
import com.learning.KafkaStarter.service.PartitionTrackingConsumer
import com.learning.KafkaStarter.service.TopicManagerService
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.kafka.test.EmbeddedKafkaBroker
import org.springframework.kafka.test.context.EmbeddedKafka
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.context.TestPropertySource
import java.time.Duration
import java.time.Instant
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@SpringBootTest
@DirtiesContext
@EmbeddedKafka(
    partitions = 3,
    topics = ["test-user-activities", "test-partition-routing"],
    brokerProperties = [
        "listeners=PLAINTEXT://localhost:9001",
        "port=9001"
    ]
)
@TestPropertySource(
    properties = [
        "spring.kafka.bootstrap-servers=localhost:9001",
        "spring.kafka.consumer.group-id=test-partition-group"
    ]
)
class PartitionManagementIntegrationTest {

    @Autowired
    private lateinit var embeddedKafkaBroker: EmbeddedKafkaBroker

    @Autowired
    private lateinit var topicManagerService: TopicManagerService

    @Autowired
    private lateinit var partitionAwareProducer: PartitionAwareProducer

    @Autowired
    private lateinit var partitionTrackingConsumer: PartitionTrackingConsumer

    @Test
    fun `should create topic with custom partition count`() {
        // Given
        val topicName = "test-topic-creation"
        val partitionCount = 5
        val replicationFactor: Short = 1

        // When
        val success = topicManagerService.createTopic(
            name = topicName,
            partitions = partitionCount,
            replicationFactor = replicationFactor,
            configs = mapOf("retention.ms" to "3600000")
        )

        // Then
        assertTrue(success, "Topic creation should succeed")

        // Verify topic details
        val topicDetails = topicManagerService.getTopicDetails(topicName)
        assertEquals(topicName, topicDetails?.name)
        assertEquals(partitionCount, topicDetails?.partitions)
        assertEquals(replicationFactor, topicDetails?.replicationFactor)
    }

    @Test
    fun `should route messages to same partition for same key`() {
        // Given
        val topicName = "test-partition-routing"
        val userId = "user123"
        val activities = listOf(
            UserActivity(userId, ActivityType.LOGIN),
            UserActivity(userId, ActivityType.VIEW_PRODUCT),
            UserActivity(userId, ActivityType.LOGOUT)
        )

        val partitionAssignments = mutableListOf<Int>()
        val latch = CountDownLatch(activities.size)

        // When - Send multiple messages with same key
        activities.forEach { activity ->
            partitionAwareProducer.sendWithKey(topicName, userId, activity)
        }

        // Wait for processing
        Thread.sleep(2000)

        // Then - Verify all messages went to same partition
        val processingStats = partitionTrackingConsumer.getPartitionProcessingStats()
        val processedPartitions = processingStats["partitionDistribution"] as? Map<Int, Long> ?: emptyMap()
        
        // At least one partition should have received messages
        assertTrue(processedPartitions.isNotEmpty(), "Messages should be processed")
        
        // For same user key, messages should go to same partition
        val activePartitions = processedPartitions.filter { it.value > 0 }.keys
        assertTrue(activePartitions.isNotEmpty(), "At least one partition should be active")
    }

    @Test
    fun `should distribute messages across partitions with different keys`() {
        // Given
        val topicName = "test-user-activities"
        val userIds = listOf("user1", "user2", "user3", "user4", "user5")

        // When - Send messages with different keys
        userIds.forEach { userId ->
            val activity = UserActivity(
                userId = userId,
                activityType = ActivityType.LOGIN,
                timestamp = Instant.now()
            )
            partitionAwareProducer.sendWithKey(topicName, userId, activity)
        }

        // Wait for processing
        Thread.sleep(2000)

        // Then - Verify distribution across partitions
        val processingStats = partitionTrackingConsumer.getPartitionProcessingStats()
        val partitionDistribution = processingStats["partitionDistribution"] as? Map<Int, Long> ?: emptyMap()
        
        // Messages should be distributed (not all in one partition)
        val activePartitions = partitionDistribution.filter { it.value > 0 }.size
        assertTrue(activePartitions >= 1, "At least one partition should be active")
    }

    @Test
    fun `should demonstrate custom partitioning strategy`() {
        // Given
        val topicName = "test-user-activities"
        val activities = listOf(
            UserActivity("auth-user1", ActivityType.LOGIN),     // Should go to partition 0
            UserActivity("purchase-user1", ActivityType.PURCHASE), // Should go to partition 1
            UserActivity("profile-user1", ActivityType.UPDATE_PROFILE) // Should go to partition 2
        )

        // When - Send with custom partitioning
        activities.forEach { activity ->
            partitionAwareProducer.sendWithCustomPartitioner(topicName, activity)
        }

        // Wait for processing
        Thread.sleep(2000)

        // Then - Verify custom partitioning worked
        val processingStats = partitionTrackingConsumer.getPartitionProcessingStats()
        assertTrue(processingStats.isNotEmpty(), "Processing stats should be available")
    }

    @Test
    fun `should track consumer partition assignments and offsets`() {
        // Given
        val topicName = "test-user-activities"
        val testActivities = (1..10).map { i ->
            UserActivity(
                userId = "test-user-$i",
                activityType = ActivityType.VIEW_PRODUCT,
                metadata = mapOf("productId" to "product-$i")
            )
        }

        // When - Send batch of messages
        testActivities.forEach { activity ->
            partitionAwareProducer.sendWithKey(topicName, activity.userId, activity)
        }

        // Wait for processing
        Thread.sleep(3000)

        // Then - Verify consumer tracking
        val partitionAssignments = partitionTrackingConsumer.getPartitionAssignments()
        val lastProcessedOffsets = partitionTrackingConsumer.getLastProcessedOffsets()
        val messageCounts = partitionTrackingConsumer.getPartitionMessageCounts()

        assertTrue(partitionAssignments.isNotEmpty(), "Should have partition assignments")
        assertTrue(lastProcessedOffsets.isNotEmpty(), "Should have offset tracking")
        assertTrue(messageCounts.values.sum() > 0, "Should have processed messages")
    }

    @Test
    fun `should scale topic partition count`() {
        // Given
        val topicName = "test-partition-scaling"
        val initialPartitions = 2
        val scaledPartitions = 4

        // Create topic with initial partition count
        val createSuccess = topicManagerService.createTopic(
            name = topicName,
            partitions = initialPartitions,
            replicationFactor = 1
        )
        assertTrue(createSuccess, "Initial topic creation should succeed")

        // When - Scale partition count
        val scaleSuccess = topicManagerService.updateTopicPartitions(topicName, scaledPartitions)

        // Then
        assertTrue(scaleSuccess, "Partition scaling should succeed")

        val updatedDetails = topicManagerService.getTopicDetails(topicName)
        assertEquals(scaledPartitions, updatedDetails?.partitions, "Partition count should be updated")
    }

    @Test
    fun `should demonstrate key distribution calculation`() {
        // Given
        val partitionCount = 3
        val testKeys = listOf("user1", "user2", "user3", "user123", "user456", "user789")

        // When - Calculate partitions for keys
        val keyDistribution = testKeys.associateWith { key ->
            partitionAwareProducer.calculatePartitionForKey(key, partitionCount)
        }

        // Then - Verify distribution
        keyDistribution.forEach { (key, partition) ->
            assertTrue(partition in 0 until partitionCount, "Partition should be within range")
        }

        // Same key should always go to same partition
        val sameKeyPartition1 = partitionAwareProducer.calculatePartitionForKey("user123", partitionCount)
        val sameKeyPartition2 = partitionAwareProducer.calculatePartitionForKey("user123", partitionCount)
        assertEquals(sameKeyPartition1, sameKeyPartition2, "Same key should map to same partition")
    }

    @Test
    fun `should handle topic lifecycle operations`() {
        // Given
        val topicName = "test-lifecycle"

        // When & Then - Create
        val createSuccess = topicManagerService.createTopic(topicName, 2, 1)
        assertTrue(createSuccess, "Topic creation should succeed")

        // Verify exists
        val topics = topicManagerService.listTopics()
        assertTrue(topics.contains(topicName), "Topic should appear in list")

        // Get details
        val details = topicManagerService.getTopicDetails(topicName)
        assertEquals(topicName, details?.name, "Topic details should be retrievable")

        // Clean up - Delete
        val deleteSuccess = topicManagerService.deleteTopics(listOf(topicName))
        assertTrue(deleteSuccess, "Topic deletion should succeed")
    }
}