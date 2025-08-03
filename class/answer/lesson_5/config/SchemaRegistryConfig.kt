package com.learning.KafkaStarter.config

import io.confluent.kafka.serializers.KafkaAvroDeserializer
import io.confluent.kafka.serializers.KafkaAvroSerializer
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.clients.producer.ProducerConfig
import org.apache.kafka.common.serialization.StringDeserializer
import org.apache.kafka.common.serialization.StringSerializer
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory
import org.springframework.kafka.core.*

@Configuration
class SchemaRegistryConfig {
    
    @Value("\${spring.kafka.bootstrap-servers}")
    private lateinit var bootstrapServers: String
    
    @Value("\${spring.kafka.producer.properties.schema.registry.url}")
    private lateinit var schemaRegistryUrl: String
    
    @Bean
    fun avroProducerFactory(): ProducerFactory<String, Any> {
        val props = HashMap<String, Any>()
        props[ProducerConfig.BOOTSTRAP_SERVERS_CONFIG] = bootstrapServers
        props[ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG] = StringSerializer::class.java
        props[ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG] = KafkaAvroSerializer::class.java
        props["schema.registry.url"] = schemaRegistryUrl
        props["auto.register.schemas"] = true
        props["use.latest.version"] = true
        
        // Performance optimizations
        props[ProducerConfig.ACKS_CONFIG] = "all"
        props[ProducerConfig.RETRIES_CONFIG] = 3
        props[ProducerConfig.BATCH_SIZE_CONFIG] = 16384
        props[ProducerConfig.LINGER_MS_CONFIG] = 1
        props[ProducerConfig.BUFFER_MEMORY_CONFIG] = 33554432
        
        return DefaultKafkaProducerFactory(props)
    }
    
    @Bean
    fun avroKafkaTemplate(): KafkaTemplate<String, Any> {
        return KafkaTemplate(avroProducerFactory())
    }
    
    @Bean
    fun avroConsumerFactory(): ConsumerFactory<String, Any> {
        val props = HashMap<String, Any>()
        props[ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG] = bootstrapServers
        props[ConsumerConfig.GROUP_ID_CONFIG] = "schema-lesson-group"
        props[ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG] = StringDeserializer::class.java
        props[ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG] = KafkaAvroDeserializer::class.java
        props[ConsumerConfig.AUTO_OFFSET_RESET_CONFIG] = "earliest"
        props["schema.registry.url"] = schemaRegistryUrl
        props["specific.avro.reader"] = true
        
        // Consumer performance optimizations
        props[ConsumerConfig.FETCH_MIN_BYTES_CONFIG] = 1
        props[ConsumerConfig.FETCH_MAX_WAIT_MS_CONFIG] = 500
        props[ConsumerConfig.MAX_PARTITION_FETCH_BYTES_CONFIG] = 1048576
        
        return DefaultKafkaConsumerFactory(props)
    }
    
    @Bean
    fun avroKafkaListenerContainerFactory(): ConcurrentKafkaListenerContainerFactory<String, Any> {
        val factory = ConcurrentKafkaListenerContainerFactory<String, Any>()
        factory.consumerFactory = avroConsumerFactory()
        
        // Error handling configuration
        factory.setCommonErrorHandler { exception, data ->
            println("Error in consumer: ${exception.message}")
            println("Failed data: $data")
        }
        
        return factory
    }
    
    // Protobuf configuration (alternative to Avro)
    @Bean
    fun protobufProducerFactory(): ProducerFactory<String, Any> {
        val props = HashMap<String, Any>()
        props[ProducerConfig.BOOTSTRAP_SERVERS_CONFIG] = bootstrapServers
        props[ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG] = StringSerializer::class.java
        props[ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG] = "io.confluent.kafka.serializers.protobuf.KafkaProtobufSerializer"
        props["schema.registry.url"] = schemaRegistryUrl
        props["auto.register.schemas"] = true
        
        return DefaultKafkaProducerFactory(props)
    }
    
    @Bean
    fun protobufKafkaTemplate(): KafkaTemplate<String, Any> {
        return KafkaTemplate(protobufProducerFactory())
    }
}