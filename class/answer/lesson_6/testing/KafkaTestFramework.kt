package com.learning.KafkaStarter.testing

import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.clients.producer.ProducerConfig
import org.apache.kafka.common.serialization.StringDeserializer
import org.apache.kafka.common.serialization.StringSerializer
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Primary
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory
import org.springframework.kafka.core.*
import org.springframework.kafka.support.serializer.JsonDeserializer
import org.springframework.kafka.support.serializer.JsonSerializer
import org.springframework.kafka.test.EmbeddedKafkaBroker
import org.springframework.kafka.test.utils.KafkaTestUtils
import org.springframework.stereotype.Component
import java.time.Instant
import java.util.*
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlin.random.Random

@TestConfiguration
class KafkaTestConfiguration {
    
    @Bean
    @Primary
    fun testProducerFactory(embeddedKafka: EmbeddedKafkaBroker): ProducerFactory<String, Any> {
        val props = KafkaTestUtils.producerProps(embeddedKafka)
        props[ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG] = StringSerializer::class.java
        props[ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG] = JsonSerializer::class.java
        return DefaultKafkaProducerFactory(props)
    }
    
    @Bean
    @Primary
    fun testConsumerFactory(embeddedKafka: EmbeddedKafkaBroker): ConsumerFactory<String, Any> {
        val props = KafkaTestUtils.consumerProps("test-group", "true", embeddedKafka)
        props[ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG] = StringDeserializer::class.java
        props[ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG] = JsonDeserializer::class.java
        props[JsonDeserializer.TRUSTED_PACKAGES] = "*"
        return DefaultKafkaConsumerFactory(props)
    }
    
    @Bean
    @Primary
    fun testKafkaTemplate(testProducerFactory: ProducerFactory<String, Any>): KafkaTemplate<String, Any> {
        return KafkaTemplate(testProducerFactory)
    }
    
    @Bean
    @Primary
    fun testKafkaListenerContainerFactory(
        testConsumerFactory: ConsumerFactory<String, Any>
    ): ConcurrentKafkaListenerContainerFactory<String, Any> {
        val factory = ConcurrentKafkaListenerContainerFactory<String, Any>()
        factory.consumerFactory = testConsumerFactory
        return factory
    }
}

@Component
class KafkaTestFramework {
    
    data class TestMessage(
        val id: String = UUID.randomUUID().toString(),
        val content: String,
        val timestamp: Long = Instant.now().toEpochMilli(),
        val metadata: Map<String, Any> = emptyMap()
    )
    
    data class TestResult(
        val success: Boolean,
        val messagesSent: Int,
        val messagesReceived: Int,
        val duration: Long,
        val errors: List<String> = emptyList()
    )
    
    fun createTestMessage(content: String, metadata: Map<String, Any> = emptyMap()): TestMessage {
        return TestMessage(
            content = content,
            metadata = metadata
        )
    }
    
    fun generateTestMessages(count: Int, prefix: String = "test"): List<TestMessage> {
        return (1..count).map { i ->
            TestMessage(
                content = "$prefix-message-$i",
                metadata = mapOf(
                    "sequenceNumber" to i,
                    "batchId" to UUID.randomUUID().toString(),
                    "testData" to generateRandomTestData()
                )
            )
        }
    }
    
    fun sendMessagesAndWait(
        kafkaTemplate: KafkaTemplate<String, Any>,
        topic: String,
        messages: List<TestMessage>,
        expectedReceiveCount: Int,
        timeoutSeconds: Long = 30
    ): TestResult {
        val latch = CountDownLatch(expectedReceiveCount)
        val errors = mutableListOf<String>()
        val startTime = System.currentTimeMillis()
        
        // Send messages
        messages.forEach { message ->
            try {
                kafkaTemplate.send(topic, message.id, message).get(5, TimeUnit.SECONDS)
            } catch (e: Exception) {
                errors.add("Failed to send message ${message.id}: ${e.message}")
            }
        }
        
        // Wait for consumption (would be handled by test consumer in real test)
        val received = latch.await(timeoutSeconds, TimeUnit.SECONDS)
        val duration = System.currentTimeMillis() - startTime
        
        return TestResult(
            success = received && errors.isEmpty(),
            messagesSent = messages.size,
            messagesReceived = if (received) expectedReceiveCount else 0,
            duration = duration,
            errors = errors
        )
    }
    
    fun createLoadTestScenario(
        messagesPerSecond: Int,
        durationSeconds: Int,
        messageSize: Int = 1024
    ): List<TestMessage> {
        val totalMessages = messagesPerSecond * durationSeconds
        val largeContent = "x".repeat(messageSize)
        
        return generateTestMessages(totalMessages, "load-test").map { message ->
            message.copy(
                content = largeContent,
                metadata = message.metadata + mapOf(
                    "loadTest" to true,
                    "targetRate" to messagesPerSecond,
                    "messageSize" to messageSize
                )
            )
        }
    }
    
    fun simulateNetworkPartition(
        kafkaTemplate: KafkaTemplate<String, Any>,
        topic: String,
        durationMs: Long
    ): TestResult {
        val errors = mutableListOf<String>()
        val startTime = System.currentTimeMillis()
        
        // Simulate network issues by introducing delays and failures
        try {
            val message = createTestMessage("partition-test")
            
            // This would simulate network partition in a real test environment
            Thread.sleep(durationMs)
            
            kafkaTemplate.send(topic, message.id, message).get(1, TimeUnit.SECONDS)
            
        } catch (e: Exception) {
            errors.add("Network partition simulation: ${e.message}")
        }
        
        val duration = System.currentTimeMillis() - startTime
        
        return TestResult(
            success = errors.isEmpty(),
            messagesSent = 1,
            messagesReceived = if (errors.isEmpty()) 1 else 0,
            duration = duration,
            errors = errors
        )
    }
    
    fun validateMessageOrdering(
        receivedMessages: List<TestMessage>,
        expectedOrder: List<String>
    ): Map<String, Any> {
        val actualOrder = receivedMessages.map { it.id }
        val isOrdered = actualOrder == expectedOrder
        
        val outOfOrderMessages = mutableListOf<String>()
        actualOrder.forEachIndexed { index, messageId ->
            if (index < expectedOrder.size && messageId != expectedOrder[index]) {
                outOfOrderMessages.add("Expected ${expectedOrder[index]} at position $index, got $messageId")
            }
        }
        
        return mapOf(
            "isOrdered" to isOrdered,
            "expectedCount" to expectedOrder.size,
            "actualCount" to actualOrder.size,
            "outOfOrderMessages" to outOfOrderMessages,
            "orderingAccuracy" to if (expectedOrder.isNotEmpty()) {
                actualOrder.zip(expectedOrder).count { it.first == it.second }.toDouble() / expectedOrder.size
            } else 1.0
        )
    }
    
    fun createChaosTestScenario(): List<TestMessage> {
        val messages = mutableListOf<TestMessage>()
        
        // Add normal messages
        messages.addAll(generateTestMessages(50, "normal"))
        
        // Add messages with null values
        repeat(5) {
            messages.add(TestMessage(
                content = "",
                metadata = mapOf("chaos" to "empty-content")
            ))
        }
        
        // Add messages with very large content
        repeat(3) {
            messages.add(TestMessage(
                content = "x".repeat(10000),
                metadata = mapOf("chaos" to "large-message")
            ))
        }
        
        // Add messages with special characters
        repeat(5) {
            messages.add(TestMessage(
                content = "Special chars: !@#$%^&*(){}[]|\\:;\"'<>,.?/~`",
                metadata = mapOf("chaos" to "special-characters")
            ))
        }
        
        // Shuffle to create chaos
        return messages.shuffled()
    }
    
    fun measureThroughput(
        kafkaTemplate: KafkaTemplate<String, Any>,
        topic: String,
        messageCount: Int,
        parallelism: Int = 1
    ): Map<String, Any> {
        val messages = generateTestMessages(messageCount, "throughput")
        val startTime = System.currentTimeMillis()
        
        // Send messages with specified parallelism
        val chunks = messages.chunked(messageCount / parallelism)
        val futures = chunks.map { chunk ->
            Thread {
                chunk.forEach { message ->
                    kafkaTemplate.send(topic, message.id, message)
                }
            }.apply { start() }
        }
        
        // Wait for all threads to complete
        futures.forEach { it.join() }
        
        val endTime = System.currentTimeMillis()
        val duration = endTime - startTime
        val throughput = messageCount * 1000.0 / duration // messages per second
        
        return mapOf(
            "messageCount" to messageCount,
            "durationMs" to duration,
            "throughputMsgPerSec" to throughput,
            "parallelism" to parallelism,
            "avgTimePerMessage" to duration.toDouble() / messageCount
        )
    }
    
    fun createDeduplicationTest(): List<TestMessage> {
        val uniqueMessages = generateTestMessages(10, "unique")
        val duplicateMessages = uniqueMessages.take(5) // Duplicate first 5 messages
        
        return (uniqueMessages + duplicateMessages).shuffled()
    }
    
    fun validateDeduplication(
        processedMessages: List<TestMessage>
    ): Map<String, Any> {
        val messageIds = processedMessages.map { it.id }
        val uniqueIds = messageIds.toSet()
        val duplicates = messageIds.groupBy { it }.filter { it.value.size > 1 }
        
        return mapOf(
            "totalProcessed" to messageIds.size,
            "uniqueMessages" to uniqueIds.size,
            "duplicateCount" to duplicates.size,
            "duplicates" to duplicates.keys,
            "deduplicationEfficiency" to uniqueIds.size.toDouble() / messageIds.size
        )
    }
    
    private fun generateRandomTestData(): Map<String, Any> {
        return mapOf(
            "randomString" to UUID.randomUUID().toString(),
            "randomNumber" to Random.nextInt(1, 1000),
            "randomBoolean" to Random.nextBoolean(),
            "timestamp" to Instant.now().toString()
        )
    }
}