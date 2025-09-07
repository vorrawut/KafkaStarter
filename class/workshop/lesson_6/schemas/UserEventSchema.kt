package com.learning.KafkaStarter.schemas

import com.fasterxml.jackson.annotation.JsonProperty
import org.apache.avro.Schema
import org.apache.avro.SchemaBuilder

/**
 * Workshop Exercise: Schema Definition & Registration
 * 
 * Complete the schema definitions for different serialization formats
 * and implement schema registration with the Schema Registry.
 */

// TODO: Complete the base event model
data class UserEvent(
    @JsonProperty("event_id")
    val eventId: String,
    
    @JsonProperty("event_type") 
    val eventType: String,
    
    @JsonProperty("user_id")
    val userId: String,
    
    // TODO: Add remaining fields
    // HINT: username, email, timestamp, metadata
    val username: String = "", // TODO: Remove default, make required
    val email: String = "", // TODO: Remove default, make required
    val timestamp: Long = 0L, // TODO: Remove default, make required
    val metadata: Map<String, String> = emptyMap() // TODO: Keep optional with default
)

class UserEventSchemaDefinitions {
    
    // TODO: Define Avro schema programmatically
    fun getAvroSchema(): Schema {
        return SchemaBuilder.record("UserEvent")
            .namespace("com.learning.KafkaStarter.avro")
            .fields()
            // TODO: Add eventId field as string
            .requiredString("eventId")
            // TODO: Add eventType field as string  
            .requiredString("eventType")
            // TODO: Add userId field as string
            .requiredString("userId")
            // TODO: Add remaining required fields
            // HINT: username (string), email (string), timestamp (long)
            // TODO: Add optional metadata field as map
            // HINT: Use .optionalMap("metadata").values().stringType()
            .endRecord()
    }
    
    // TODO: Define Avro schema as JSON string (alternative approach)
    fun getAvroSchemaJson(): String {
        return """
        {
          "type": "record",
          "name": "UserEvent",
          "namespace": "com.learning.KafkaStarter.avro",
          "fields": [
            {"name": "eventId", "type": "string"},
            {"name": "eventType", "type": "string"},
            {"name": "userId", "type": "string"}
            // TODO: Add remaining fields in JSON format
            // HINT: username, email, timestamp, metadata
          ]
        }
        """.trimIndent()
    }
    
    // TODO: Define schema evolution example
    fun getAvroSchemaV2(): Schema {
        return SchemaBuilder.record("UserEvent")
            .namespace("com.learning.KafkaStarter.avro")
            .fields()
            .requiredString("eventId")
            .requiredString("eventType") 
            .requiredString("userId")
            .requiredString("username")
            .requiredString("email")
            .requiredLong("timestamp")
            
            // TODO: Add new optional fields for v2 (backward compatible)
            // HINT: Add fields with default values
            // Examples: source, version, correlation_id
            .optionalString("source") // TODO: Add default value
            .optionalString("version") // TODO: Add default value
            
            .endRecord()
    }
    
    // TODO: Generate Protobuf schema definition
    fun getProtobufSchemaDefinition(): String {
        return """
            syntax = "proto3";
            
            package com.learning.KafkaStarter.protobuf;
            
            message UserEvent {
              string event_id = 1;
              string event_type = 2;
              string user_id = 3;
              // TODO: Add remaining fields with proper field numbers
              // HINT: username=4, email=5, timestamp=6, metadata=7
              
              // TODO: Define metadata as map
              // HINT: map<string, string> metadata = 7;
            }
        """.trimIndent()
    }
}

// TODO: Implement schema registration service
class SchemaRegistryService {
    
    // TODO: Register schema with Schema Registry
    fun registerSchema(
        subject: String,
        schema: String,
        schemaType: String = "AVRO"
    ): SchemaRegistrationResult {
        // TODO: Implement HTTP call to Schema Registry
        // HINT: POST to /subjects/{subject}/versions
        // HINT: Content-Type: application/vnd.schemaregistry.v1+json
        
        return SchemaRegistrationResult(
            success = false, // TODO: Set based on actual registration
            schemaId = 0, // TODO: Extract from response
            version = 0, // TODO: Extract from response
            message = "TODO: Implement schema registration"
        )
    }
    
    // TODO: Retrieve schema by ID
    fun getSchemaById(schemaId: Int): SchemaRetrievalResult {
        // TODO: Implement HTTP call to get schema
        // HINT: GET /schemas/ids/{id}
        
        return SchemaRetrievalResult(
            success = false, // TODO: Set based on actual retrieval
            schema = "", // TODO: Extract schema from response
            message = "TODO: Implement schema retrieval"
        )
    }
    
    // TODO: Check schema compatibility
    fun checkCompatibility(
        subject: String,
        newSchema: String,
        version: String = "latest"
    ): CompatibilityResult {
        // TODO: Implement compatibility check
        // HINT: POST /compatibility/subjects/{subject}/versions/{version}
        
        return CompatibilityResult(
            isCompatible = false, // TODO: Set based on actual check
            compatibilityLevel = "UNKNOWN", // TODO: Extract from response
            message = "TODO: Implement compatibility check"
        )
    }
    
    // TODO: List all subjects
    fun listSubjects(): List<String> {
        // TODO: Implement subject listing
        // HINT: GET /subjects
        
        return emptyList() // TODO: Return actual subjects
    }
    
    // TODO: Get schema versions for subject
    fun getVersions(subject: String): List<Int> {
        // TODO: Implement version listing
        // HINT: GET /subjects/{subject}/versions
        
        return emptyList() // TODO: Return actual versions
    }
}

// Data classes for results
data class SchemaRegistrationResult(
    val success: Boolean,
    val schemaId: Int,
    val version: Int,
    val message: String
)

data class SchemaRetrievalResult(
    val success: Boolean,
    val schema: String,
    val message: String
)

data class CompatibilityResult(
    val isCompatible: Boolean,
    val compatibilityLevel: String,
    val message: String
)

// TODO: Schema evolution strategy enum
enum class CompatibilityMode {
    BACKWARD,    // New schema can read old data
    FORWARD,     // Old schema can read new data  
    FULL,        // Both backward and forward
    NONE         // No compatibility checking
}