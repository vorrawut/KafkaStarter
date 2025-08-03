package com.learning.KafkaStarter.config

import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.clients.producer.ProducerConfig
import org.apache.kafka.common.serialization.StringDeserializer
import org.apache.kafka.common.serialization.StringSerializer
import org.springframework.boot.autoconfigure.kafka.KafkaProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.kafka.annotation.EnableKafka
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory
import org.springframework.kafka.core.*
import org.springframework.kafka.listener.ContainerProperties
import org.springframework.kafka.support.serializer.ErrorHandlingDeserializer
import org.springframework.kafka.support.serializer.JsonDeserializer
import org.springframework.kafka.support.serializer.JsonSerializer

@Configuration
@EnableKafka
class KafkaConfig(
    private val kafkaProperties: KafkaProperties
) {
    
    @Bean
    fun kafkaTemplate(): KafkaTemplate<String, Any> {
        return KafkaTemplate(producerFactory()).apply {
            setDefaultTopic("default-topic")
        }
    }
    
    @Bean
    fun producerFactory(): ProducerFactory<String, Any> {
        val props = mutableMapOf<String, Any>()
        props[ProducerConfig.BOOTSTRAP_SERVERS_CONFIG] = kafkaProperties.bootstrapServers
        props[ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG] = StringSerializer::class.java
        props[ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG] = JsonSerializer::class.java
        props[ProducerConfig.ACKS_CONFIG] = "all"
        props[ProducerConfig.RETRIES_CONFIG] = 3
        props[ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG] = true
        
        return DefaultKafkaProducerFactory(props)
    }
    
    @Bean
    fun consumerFactory(): ConsumerFactory<String, Any> {
        val props = mutableMapOf<String, Any>()
        props[ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG] = kafkaProperties.bootstrapServers
        props[ConsumerConfig.GROUP_ID_CONFIG] = kafkaProperties.consumer.groupId
        props[ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG] = StringDeserializer::class.java
        props[ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG] = ErrorHandlingDeserializer::class.java
        props[ErrorHandlingDeserializer.VALUE_DESERIALIZER_CLASS] = JsonDeserializer::class.java
        props[ConsumerConfig.AUTO_OFFSET_RESET_CONFIG] = "earliest"
        props[ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG] = false
        props[JsonDeserializer.TRUSTED_PACKAGES] = "com.learning.KafkaStarter.model"
        
        return DefaultKafkaConsumerFactory(props)
    }
    
    @Bean
    fun kafkaListenerContainerFactory(): ConcurrentKafkaListenerContainerFactory<String, Any> {
        val factory = ConcurrentKafkaListenerContainerFactory<String, Any>()
        factory.consumerFactory = consumerFactory()
        factory.containerProperties.ackMode = ContainerProperties.AckMode.MANUAL_IMMEDIATE
        factory.setConcurrency(1) // Start with single consumer
        factory.setCommonErrorHandler(
            org.springframework.kafka.listener.DefaultErrorHandler()
        )
        return factory
    }
}