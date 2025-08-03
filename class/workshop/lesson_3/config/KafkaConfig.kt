package com.learning.KafkaStarter.config

import org.springframework.context.annotation.Configuration
import org.springframework.kafka.annotation.EnableKafka

// TODO: Add @Configuration and @EnableKafka annotations
// TODO: Create beans for KafkaTemplate, ProducerFactory, ConsumerFactory
// TODO: Configure JSON serializers/deserializers
// TODO: Set up error handling configuration

@Configuration
@EnableKafka
class KafkaConfig {
    
    // TODO: Create @Bean for KafkaTemplate<String, Any>
    // TODO: Create @Bean for ProducerFactory<String, Any>
    // TODO: Create @Bean for ConsumerFactory<String, Any>
    
    // HINT: Use DefaultKafkaProducerFactory and DefaultKafkaConsumerFactory
    // HINT: Use JsonSerializer and JsonDeserializer
    // HINT: Set up proper error handling with SeekToCurrentErrorHandler
}