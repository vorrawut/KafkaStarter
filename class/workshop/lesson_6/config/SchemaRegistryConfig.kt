package com.learning.KafkaStarter.config

import org.springframework.context.annotation.Configuration

// TODO: Add necessary imports for Schema Registry
// TODO: Import Avro serializers and deserializers
// TODO: Import Protobuf serializers and deserializers
// TODO: Import Schema Registry client

@Configuration
class SchemaRegistryConfig {
    
    // TODO: Create @Bean for Schema Registry client
    // TODO: Configure connection URL and authentication
    // TODO: Set up connection pooling and timeouts
    
    // TODO: Create @Bean for Avro producer factory
    // TODO: Configure KafkaAvroSerializer
    // TODO: Set specific.avro.reader to true
    
    // TODO: Create @Bean for Avro consumer factory  
    // TODO: Configure KafkaAvroDeserializer
    // TODO: Set up trusted packages for security
    
    // TODO: Create @Bean for Protobuf producer factory
    // TODO: Configure KafkaProtobufSerializer
    // TODO: Set up proper value serializer
    
    // TODO: Create @Bean for Protobuf consumer factory
    // TODO: Configure KafkaProtobufDeserializer
    // TODO: Set up proper value deserializer
    
    // TODO: Create @Bean for KafkaTemplate<String, SpecificRecord> (Avro)
    // TODO: Create @Bean for KafkaTemplate<String, Message> (Protobuf)
    
    // HINT: Use io.confluent.kafka.serializers.KafkaAvroSerializer
    // HINT: Use io.confluent.kafka.serializers.protobuf.KafkaProtobufSerializer
    // HINT: Schema Registry URL is typically "http://localhost:8081"
}