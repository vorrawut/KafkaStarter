package com.learning.KafkaStarter.config

import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.common.serialization.StringDeserializer
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory
import org.springframework.kafka.core.ConsumerFactory
import org.springframework.kafka.core.DefaultKafkaConsumerFactory
import org.springframework.kafka.listener.ContainerProperties
import org.springframework.kafka.support.serializer.JsonDeserializer

@Configuration
class ManualAckKafkaConfig {
    
    @Value("\${spring.kafka.bootstrap-servers}")
    private lateinit var bootstrapServers: String
    
    @Bean
    fun manualAckConsumerFactory(): ConsumerFactory<String, Any> {
        val props = mutableMapOf<String, Any>()
        
        // Basic consumer configuration
        props[ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG] = bootstrapServers
        props[ConsumerConfig.GROUP_ID_CONFIG] = "lesson9-manual-ack-group"
        props[ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG] = StringDeserializer::class.java
        props[ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG] = JsonDeserializer::class.java
        
        // Manual acknowledgment configuration
        props[ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG] = false // Disable auto-commit
        props[ConsumerConfig.AUTO_OFFSET_RESET_CONFIG] = "earliest"
        
        // Session and heartbeat configuration for manual ack
        props[ConsumerConfig.SESSION_TIMEOUT_MS_CONFIG] = 30000
        props[ConsumerConfig.HEARTBEAT_INTERVAL_MS_CONFIG] = 3000
        props[ConsumerConfig.MAX_POLL_INTERVAL_MS_CONFIG] = 300000
        
        // Performance and reliability settings
        props[ConsumerConfig.MAX_POLL_RECORDS_CONFIG] = 100
        props[ConsumerConfig.FETCH_MIN_BYTES_CONFIG] = 1
        props[ConsumerConfig.FETCH_MAX_WAIT_MS_CONFIG] = 500
        
        // JSON deserializer configuration
        props[JsonDeserializer.TRUSTED_PACKAGES] = "com.learning.KafkaStarter.model"
        props[JsonDeserializer.VALUE_DEFAULT_TYPE] = "com.learning.KafkaStarter.model.BankTransferEvent"
        
        return DefaultKafkaConsumerFactory(props)
    }
    
    @Bean
    fun manualAckContainerFactory(): ConcurrentKafkaListenerContainerFactory<String, Any> {
        val factory = ConcurrentKafkaListenerContainerFactory<String, Any>()
        
        // Set consumer factory
        factory.consumerFactory = manualAckConsumerFactory()
        
        // Configure for manual acknowledgment
        factory.containerProperties.ackMode = ContainerProperties.AckMode.MANUAL_IMMEDIATE
        
        // Error handling configuration
        factory.setCommonErrorHandler { exception, data ->
            // Log error but don't acknowledge - message will be redelivered
            println("Manual ACK Error Handler: ${exception.message}")
        }
        
        // Performance settings
        factory.setConcurrency(3) // 3 consumer threads
        factory.containerProperties.pollTimeout = 3000
        
        return factory
    }
    
    @Bean
    fun idempotentContainerFactory(): ConcurrentKafkaListenerContainerFactory<String, Any> {
        val factory = ConcurrentKafkaListenerContainerFactory<String, Any>()
        
        // Create consumer factory with exactly-once settings
        val props = mutableMapOf<String, Any>()
        props[ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG] = bootstrapServers
        props[ConsumerConfig.GROUP_ID_CONFIG] = "lesson9-idempotent-group"
        props[ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG] = StringDeserializer::class.java
        props[ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG] = JsonDeserializer::class.java
        
        // Exactly-once semantics configuration
        props[ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG] = true // Auto-commit for idempotent processing
        props[ConsumerConfig.AUTO_OFFSET_RESET_CONFIG] = "earliest"
        props[ConsumerConfig.ISOLATION_LEVEL_CONFIG] = "read_committed" // Read only committed messages
        
        // Session and heartbeat configuration
        props[ConsumerConfig.SESSION_TIMEOUT_MS_CONFIG] = 30000
        props[ConsumerConfig.HEARTBEAT_INTERVAL_MS_CONFIG] = 3000
        
        // Performance settings for idempotent processing
        props[ConsumerConfig.MAX_POLL_RECORDS_CONFIG] = 50 // Smaller batches for better control
        props[ConsumerConfig.FETCH_MIN_BYTES_CONFIG] = 1
        props[ConsumerConfig.FETCH_MAX_WAIT_MS_CONFIG] = 500
        
        // JSON configuration
        props[JsonDeserializer.TRUSTED_PACKAGES] = "com.learning.KafkaStarter.model"
        props[JsonDeserializer.VALUE_DEFAULT_TYPE] = "com.learning.KafkaStarter.model.BankTransferEvent"
        
        val idempotentConsumerFactory = DefaultKafkaConsumerFactory<String, Any>(props)
        factory.consumerFactory = idempotentConsumerFactory
        
        // Configure for exactly-once processing
        factory.containerProperties.ackMode = ContainerProperties.AckMode.RECORD // Commit after each record
        
        // Error handling for idempotent processing
        factory.setCommonErrorHandler { exception, data ->
            // Log error and continue - idempotent processing should handle duplicates
            println("Idempotent Processing Error Handler: ${exception.message}")
        }
        
        // Performance settings
        factory.setConcurrency(2) // 2 consumer threads for idempotent processing
        factory.containerProperties.pollTimeout = 3000
        
        return factory
    }
    
    @Bean
    fun exactlyOnceContainerFactory(): ConcurrentKafkaListenerContainerFactory<String, Any> {
        val factory = ConcurrentKafkaListenerContainerFactory<String, Any>()
        
        // Create consumer factory with strict exactly-once settings
        val props = mutableMapOf<String, Any>()
        props[ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG] = bootstrapServers
        props[ConsumerConfig.GROUP_ID_CONFIG] = "lesson9-exactly-once-group"
        props[ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG] = StringDeserializer::class.java
        props[ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG] = JsonDeserializer::class.java
        
        // Strict exactly-once configuration
        props[ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG] = false // Manual control
        props[ConsumerConfig.AUTO_OFFSET_RESET_CONFIG] = "earliest"
        props[ConsumerConfig.ISOLATION_LEVEL_CONFIG] = "read_committed"
        
        // Conservative settings for exactly-once
        props[ConsumerConfig.MAX_POLL_RECORDS_CONFIG] = 1 // Process one record at a time
        props[ConsumerConfig.SESSION_TIMEOUT_MS_CONFIG] = 30000
        props[ConsumerConfig.HEARTBEAT_INTERVAL_MS_CONFIG] = 3000
        props[ConsumerConfig.MAX_POLL_INTERVAL_MS_CONFIG] = 600000 // Longer timeout for processing
        
        // JSON configuration
        props[JsonDeserializer.TRUSTED_PACKAGES] = "com.learning.KafkaStarter.model"
        props[JsonDeserializer.VALUE_DEFAULT_TYPE] = "com.learning.KafkaStarter.model.BankTransferEvent"
        
        val exactlyOnceConsumerFactory = DefaultKafkaConsumerFactory<String, Any>(props)
        factory.consumerFactory = exactlyOnceConsumerFactory
        
        // Configure for manual acknowledgment with exactly-once semantics
        factory.containerProperties.ackMode = ContainerProperties.AckMode.MANUAL_IMMEDIATE
        
        // Strict error handling
        factory.setCommonErrorHandler { exception, data ->
            // Log error and stop processing to prevent data loss
            println("Exactly-Once Error Handler: ${exception.message}")
            // In production, you might want to send to DLT or alert monitoring
        }
        
        // Single-threaded for strict ordering and exactly-once semantics
        factory.setConcurrency(1)
        factory.containerProperties.pollTimeout = 5000
        
        return factory
    }
}