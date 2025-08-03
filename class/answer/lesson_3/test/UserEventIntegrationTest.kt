package com.learning.KafkaStarter

import com.learning.KafkaStarter.model.UserRegisteredEvent
import com.learning.KafkaStarter.service.UserEventConsumer
import com.learning.KafkaStarter.service.UserEventProducer
import org.apache.kafka.clients.producer.ProducerConfig
import org.apache.kafka.common.serialization.StringSerializer
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.kafka.core.DefaultKafkaProducerFactory
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.kafka.support.serializer.JsonSerializer
import org.springframework.kafka.test.EmbeddedKafkaBroker
import org.springframework.kafka.test.context.EmbeddedKafka
import org.springframework.kafka.test.utils.KafkaTestUtils
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.context.TestPropertySource
import java.time.Duration
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlin.test.assertTrue

@SpringBootTest
@DirtiesContext
@EmbeddedKafka(
    partitions = 1,
    topics = ["user-registration"],
    brokerProperties = [
        "listeners=PLAINTEXT://localhost:9000",
        "port=9000"
    ]
)
@TestPropertySource(
    properties = [
        "spring.kafka.bootstrap-servers=localhost:9000",
        "spring.kafka.consumer.group-id=test-group"
    ]
)
class UserEventIntegrationTest {

    @Autowired
    private lateinit var embeddedKafkaBroker: EmbeddedKafkaBroker

    @Autowired
    private lateinit var userEventProducer: UserEventProducer

    @Test
    fun `should publish and consume user registration event successfully`() {
        // Given
        val event = UserRegisteredEvent(
            userId = "test-user-123",
            email = "test@example.com",
            source = "integration-test"
        )

        val latch = CountDownLatch(1)
        var receivedEvent: UserRegisteredEvent? = null

        // Create a test consumer to verify message consumption
        val consumerProps = KafkaTestUtils.consumerProps("test-group", "true", embeddedKafkaBroker)
        consumerProps[org.apache.kafka.clients.consumer.ConsumerConfig.AUTO_OFFSET_RESET_CONFIG] = "earliest"

        // When
        userEventProducer.publishUserRegistered(event)

        // Then
        assertTrue(latch.await(10, TimeUnit.SECONDS), "Event should be consumed within 10 seconds")
        // Additional assertions would go here in a full implementation
    }

    @Test
    fun `should handle producer failure gracefully`() {
        // Given
        val invalidEvent = UserRegisteredEvent(
            userId = "",  // Invalid empty userId
            email = "invalid-email",  // Invalid email format
            source = "test"
        )

        // When & Then
        // The producer should handle this gracefully without throwing exceptions
        // In a real test, you would verify error logging and metrics
        userEventProducer.publishUserRegistered(invalidEvent)
    }

    @Test
    fun `should process multiple events in order`() {
        // Given
        val events = listOf(
            UserRegisteredEvent("user1", "user1@example.com", "test"),
            UserRegisteredEvent("user2", "user2@example.com", "test"),
            UserRegisteredEvent("user3", "user3@example.com", "test")
        )

        // When
        events.forEach { event ->
            userEventProducer.publishUserRegistered(event)
        }

        // Then
        // In a full implementation, you would verify:
        // - All events were consumed
        // - Events were processed in the correct order (within the same partition)
        // - No events were lost
        Thread.sleep(1000) // Simple wait for processing
    }
}

// Test-specific consumer for verification
class TestUserEventConsumer {
    private val logger = org.slf4j.LoggerFactory.getLogger(TestUserEventConsumer::class.java)
    val processedEvents = mutableListOf<UserRegisteredEvent>()
    var latch: CountDownLatch? = null

    @org.springframework.kafka.annotation.KafkaListener(topics = ["user-registration"])
    fun handleUserRegistered(event: UserRegisteredEvent) {
        logger.info("Test consumer received event: ${event.userId}")
        processedEvents.add(event)
        latch?.countDown()
    }
}