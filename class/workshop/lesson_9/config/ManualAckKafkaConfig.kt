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
        // TODO: Configure consumer factory for manual acknowledgment
        // HINT: Set enable.auto.commit to false and configure other manual ack settings
        TODO("Configure manual ack consumer factory")
        
        return DefaultKafkaConsumerFactory(props)
    }
    
    @Bean
    fun manualAckContainerFactory(): ConcurrentKafkaListenerContainerFactory<String, Any> {
        val factory = ConcurrentKafkaListenerContainerFactory<String, Any>()
        // TODO: Configure container factory for manual acknowledgment
        // HINT: Set ack mode to MANUAL_IMMEDIATE and configure consumer factory
        TODO("Configure manual ack container factory")
        
        return factory
    }
    
    @Bean
    fun idempotentContainerFactory(): ConcurrentKafkaListenerContainerFactory<String, Any> {
        val factory = ConcurrentKafkaListenerContainerFactory<String, Any>()
        // TODO: Configure container factory for idempotent processing
        // HINT: Configure for exactly-once semantics with manual processing
        TODO("Configure idempotent container factory")
        
        return factory
    }
}