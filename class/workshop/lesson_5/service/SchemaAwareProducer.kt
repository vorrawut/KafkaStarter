package com.learning.KafkaStarter.service

import org.springframework.stereotype.Service

// TODO: Add necessary imports for Avro and Protobuf
// TODO: Import KafkaTemplate for different message types
// TODO: Import generated Avro and Protobuf classes

@Service
class SchemaAwareProducer(
    // TODO: Inject KafkaTemplate for Avro messages
    // TODO: Inject KafkaTemplate for Protobuf messages
    // TODO: Inject regular KafkaTemplate for JSON messages
) {
    
    // TODO: Implement sendAvroUserRegistered method
    // TODO: Create Avro record using builder pattern
    // TODO: Send with proper topic and key
    // TODO: Add callback handling for success/failure
    
    // TODO: Implement sendProtobufUserRegistered method
    // TODO: Create Protobuf message using builder pattern
    // TODO: Send with proper topic and key
    // TODO: Add callback handling for success/failure
    
    // TODO: Implement sendJsonUserRegistered method (for comparison)
    // TODO: Create JSON payload
    // TODO: Send with proper topic and key
    // TODO: Add callback handling for success/failure
    
    // TODO: Implement compareFormats method
    // TODO: Send same message in all three formats
    // TODO: Measure and log payload sizes
    // TODO: Record performance metrics
    
    // TODO: Add method to demonstrate schema evolution
    // TODO: Send messages with old and new schema versions
    // TODO: Verify compatibility handling
    
    // HINT: Use .newBuilder() for Avro records
    // HINT: Use .newBuilder() for Protobuf messages
    // HINT: Measure System.currentTimeMillis() for performance
}