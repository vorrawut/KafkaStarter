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
        
        // TODO: Configure basic Kafka Streams properties
        // TODO: Set application ID for this streams application
        // TODO: Configure bootstrap servers
        // TODO: Set default key and value serdes
        // TODO: Configure processing guarantees (exactly-once vs at-least-once)
        // TODO: Set state directory for local state stores
        // TODO: Configure number of stream threads
        // TODO: Set commit interval and cache settings
        
        return KafkaStreamsConfiguration(props)
    }
}