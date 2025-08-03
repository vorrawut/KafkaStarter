package com.learning.KafkaStarter.config

import org.apache.kafka.common.serialization.Serdes
import org.apache.kafka.streams.StreamsConfig
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.kafka.annotation.EnableKafkaStreams
import org.springframework.kafka.config.KafkaStreamsConfiguration
import org.springframework.kafka.support.serializer.JsonSerde

@Configuration
@EnableKafkaStreams
class KafkaStreamsConfig {
    
    @Bean(name = ["defaultKafkaStreamsConfig"])
    fun defaultKafkaStreamsConfig(): KafkaStreamsConfiguration {
        val props = HashMap<String, Any>()
        
        // Basic Kafka Streams configuration
        props[StreamsConfig.APPLICATION_ID_CONFIG] = "lesson14-streams-app"
        props[StreamsConfig.BOOTSTRAP_SERVERS_CONFIG] = "localhost:9092"
        
        // Serialization configuration
        props[StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG] = Serdes.String()::class.java
        props[StreamsConfig.DEFAULT_VALUE_SERDE_CLASS_CONFIG] = JsonSerde::class.java
        
        // Processing guarantees
        props[StreamsConfig.PROCESSING_GUARANTEE_CONFIG] = StreamsConfig.EXACTLY_ONCE_V2
        
        // State and performance configuration
        props[StreamsConfig.STATE_DIR_CONFIG] = "/tmp/kafka-streams/lesson14"
        props[StreamsConfig.NUM_STREAM_THREADS_CONFIG] = 2
        props[StreamsConfig.COMMIT_INTERVAL_MS_CONFIG] = 1000 // 1 second
        props[StreamsConfig.CACHE_MAX_BYTES_BUFFERING_CONFIG] = 1024 * 1024 // 1MB
        
        // Error handling
        props[StreamsConfig.DEFAULT_DESERIALIZATION_EXCEPTION_HANDLER_CLASS_CONFIG] = 
            "org.apache.kafka.streams.errors.LogAndContinueExceptionHandler"
        
        // Replication factor
        props[StreamsConfig.REPLICATION_FACTOR_CONFIG] = 1
        
        return KafkaStreamsConfiguration(props)
    }
}