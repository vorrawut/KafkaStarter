package com.learning.KafkaStarter.schemas

import com.fasterxml.jackson.annotation.JsonProperty
import org.apache.avro.Schema
import org.apache.avro.SchemaBuilder
import org.springframework.stereotype.Service
import org.springframework.web.client.RestTemplate
import org.springframework.http.*
import org.slf4j.LoggerFactory

/**
 * Complete Answer: Schema Definition & Registration
 * 
 * This demonstrates comprehensive schema management with Avro, Protobuf,
 * and Schema Registry integration for production-ready applications.
 */

data class UserEvent(
    @JsonProperty("event_id")
    val eventId: String,
    
    @JsonProperty("event_type") 
    val eventType: String,
    
    @JsonProperty("user_id")
    val userId: String,
    
    val username: String,
    val email: String,
    val timestamp: Long,
    val metadata: Map<String, String> = emptyMap()
)

@Service
class UserEventSchemaDefinitions {
    
    private val logger = LoggerFactory.getLogger(UserEventSchemaDefinitions::class.java)
    
    fun getAvroSchema(): Schema {
        return SchemaBuilder.record("UserEvent")
            .namespace("com.learning.KafkaStarter.avro")
            .fields()
            .requiredString("eventId")
            .requiredString("eventType")
            .requiredString("userId")
            .requiredString("username")
            .requiredString("email")
            .requiredLong("timestamp")
            .optionalMap("metadata").values().stringType().mapDefault(emptyMap<String, String>())
            .endRecord()
    }
    
    fun getAvroSchemaJson(): String {
        return """
        {
          "type": "record",
          "name": "UserEvent",
          "namespace": "com.learning.KafkaStarter.avro",
          "fields": [
            {"name": "eventId", "type": "string"},
            {"name": "eventType", "type": "string"},
            {"name": "userId", "type": "string"},
            {"name": "username", "type": "string"},
            {"name": "email", "type": "string"},
            {"name": "timestamp", "type": "long"},
            {
              "name": "metadata", 
              "type": {
                "type": "map",
                "values": "string"
              },
              "default": {}
            }
          ]
        }
        """.trimIndent()
    }
    
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
            .optionalMap("metadata").values().stringType().mapDefault(emptyMap<String, String>())
            
            // New optional fields for v2 (backward compatible)
            .optionalString("source").stringDefault("unknown")
            .optionalString("version").stringDefault("1.0")
            .optionalString("correlationId").stringDefault("")
            .optionalLong("processingTime").longDefault(0L)
            
            .endRecord()
    }
    
    fun getProtobufSchemaDefinition(): String {
        return """
            syntax = "proto3";
            
            package com.learning.KafkaStarter.protobuf;
            
            message UserEvent {
              string event_id = 1;
              string event_type = 2;
              string user_id = 3;
              string username = 4;
              string email = 5;
              int64 timestamp = 6;
              map<string, string> metadata = 7;
              
              // Optional fields for evolution
              string source = 8;
              string version = 9;
              string correlation_id = 10;
              int64 processing_time = 11;
            }
        """.trimIndent()
    }
    
    fun getProtobufSchemaV2Definition(): String {
        return """
            syntax = "proto3";
            
            package com.learning.KafkaStarter.protobuf;
            
            message UserEvent {
              string event_id = 1;
              string event_type = 2;
              string user_id = 3;
              string username = 4;
              string email = 5;
              int64 timestamp = 6;
              map<string, string> metadata = 7;
              
              // V2 additions (field numbers must not conflict)
              string source = 8;
              string version = 9;
              string correlation_id = 10;
              int64 processing_time = 11;
              
              // New in v2
              repeated string tags = 12;
              UserProfile user_profile = 13;
            }
            
            message UserProfile {
              string first_name = 1;
              string last_name = 2;
              string phone_number = 3;
              Address address = 4;
            }
            
            message Address {
              string street = 1;
              string city = 2;
              string state = 3;
              string zip_code = 4;
              string country = 5;
            }
        """.trimIndent()
    }
}

@Service
class SchemaRegistryService {
    
    private val logger = LoggerFactory.getLogger(SchemaRegistryService::class.java)
    private val restTemplate = RestTemplate()
    private val schemaRegistryUrl = "http://localhost:8081"
    
    fun registerSchema(
        subject: String,
        schema: String,
        schemaType: String = "AVRO"
    ): SchemaRegistrationResult {
        return try {
            logger.info("Registering schema for subject: $subject")
            
            val url = "$schemaRegistryUrl/subjects/$subject/versions"
            val headers = HttpHeaders().apply {
                contentType = MediaType.parseMediaType("application/vnd.schemaregistry.v1+json")
            }
            
            val requestBody = mapOf(
                "schema" to schema,
                "schemaType" to schemaType
            )
            
            val request = HttpEntity(requestBody, headers)
            val response = restTemplate.postForObject(url, request, Map::class.java)
            
            val schemaId = response?.get("id") as? Int ?: 0
            val version = response?.get("version") as? Int ?: 0
            
            logger.info("Successfully registered schema: ID=$schemaId, Version=$version")
            
            SchemaRegistrationResult(
                success = true,
                schemaId = schemaId,
                version = version,
                message = "Schema registered successfully"
            )
        } catch (e: Exception) {
            logger.error("Failed to register schema for subject: $subject", e)
            SchemaRegistrationResult(
                success = false,
                schemaId = 0,
                version = 0,
                message = "Registration failed: ${e.message}"
            )
        }
    }
    
    fun getSchemaById(schemaId: Int): SchemaRetrievalResult {
        return try {
            logger.info("Retrieving schema by ID: $schemaId")
            
            val url = "$schemaRegistryUrl/schemas/ids/$schemaId"
            val response = restTemplate.getForObject(url, Map::class.java)
            
            val schema = response?.get("schema") as? String ?: ""
            
            SchemaRetrievalResult(
                success = true,
                schema = schema,
                message = "Schema retrieved successfully"
            )
        } catch (e: Exception) {
            logger.error("Failed to retrieve schema by ID: $schemaId", e)
            SchemaRetrievalResult(
                success = false,
                schema = "",
                message = "Retrieval failed: ${e.message}"
            )
        }
    }
    
    fun checkCompatibility(
        subject: String,
        newSchema: String,
        version: String = "latest"
    ): CompatibilityResult {
        return try {
            logger.info("Checking compatibility for subject: $subject, version: $version")
            
            val url = "$schemaRegistryUrl/compatibility/subjects/$subject/versions/$version"
            val headers = HttpHeaders().apply {
                contentType = MediaType.parseMediaType("application/vnd.schemaregistry.v1+json")
            }
            
            val requestBody = mapOf("schema" to newSchema)
            val request = HttpEntity(requestBody, headers)
            
            val response = restTemplate.postForObject(url, request, Map::class.java)
            val isCompatible = response?.get("is_compatible") as? Boolean ?: false
            
            // Get compatibility level
            val levelResponse = restTemplate.getForObject(
                "$schemaRegistryUrl/config/$subject", 
                Map::class.java
            )
            val compatibilityLevel = levelResponse?.get("compatibilityLevel") as? String ?: "UNKNOWN"
            
            CompatibilityResult(
                isCompatible = isCompatible,
                compatibilityLevel = compatibilityLevel,
                message = if (isCompatible) "Schema is compatible" else "Schema is not compatible"
            )
        } catch (e: Exception) {
            logger.error("Failed to check compatibility for subject: $subject", e)
            CompatibilityResult(
                isCompatible = false,
                compatibilityLevel = "UNKNOWN",
                message = "Compatibility check failed: ${e.message}"
            )
        }
    }
    
    fun listSubjects(): List<String> {
        return try {
            logger.info("Listing all subjects")
            
            val url = "$schemaRegistryUrl/subjects"
            val response = restTemplate.getForObject(url, Array<String>::class.java)
            
            response?.toList() ?: emptyList()
        } catch (e: Exception) {
            logger.error("Failed to list subjects", e)
            emptyList()
        }
    }
    
    fun getVersions(subject: String): List<Int> {
        return try {
            logger.info("Getting versions for subject: $subject")
            
            val url = "$schemaRegistryUrl/subjects/$subject/versions"
            val response = restTemplate.getForObject(url, Array<Int>::class.java)
            
            response?.toList() ?: emptyList()
        } catch (e: Exception) {
            logger.error("Failed to get versions for subject: $subject", e)
            emptyList()
        }
    }
    
    fun getLatestSchema(subject: String): SchemaRetrievalResult {
        return try {
            logger.info("Getting latest schema for subject: $subject")
            
            val url = "$schemaRegistryUrl/subjects/$subject/versions/latest"
            val response = restTemplate.getForObject(url, Map::class.java)
            
            val schema = response?.get("schema") as? String ?: ""
            val version = response?.get("version") as? Int ?: 0
            val id = response?.get("id") as? Int ?: 0
            
            SchemaRetrievalResult(
                success = true,
                schema = schema,
                message = "Latest schema retrieved (Version: $version, ID: $id)"
            )
        } catch (e: Exception) {
            logger.error("Failed to get latest schema for subject: $subject", e)
            SchemaRetrievalResult(
                success = false,
                schema = "",
                message = "Failed to get latest schema: ${e.message}"
            )
        }
    }
    
    fun setCompatibilityLevel(subject: String, level: CompatibilityMode): Boolean {
        return try {
            logger.info("Setting compatibility level for subject: $subject to $level")
            
            val url = "$schemaRegistryUrl/config/$subject"
            val headers = HttpHeaders().apply {
                contentType = MediaType.APPLICATION_JSON
            }
            
            val requestBody = mapOf("compatibility" to level.name)
            val request = HttpEntity(requestBody, headers)
            
            restTemplate.put(url, request)
            logger.info("Successfully set compatibility level for $subject to $level")
            true
        } catch (e: Exception) {
            logger.error("Failed to set compatibility level for subject: $subject", e)
            false
        }
    }
    
    fun deleteSubject(subject: String, permanent: Boolean = false): Boolean {
        return try {
            logger.info("Deleting subject: $subject (permanent: $permanent)")
            
            val url = if (permanent) {
                "$schemaRegistryUrl/subjects/$subject?permanent=true"
            } else {
                "$schemaRegistryUrl/subjects/$subject"
            }
            
            restTemplate.delete(url)
            logger.info("Successfully deleted subject: $subject")
            true
        } catch (e: Exception) {
            logger.error("Failed to delete subject: $subject", e)
            false
        }
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

enum class CompatibilityMode {
    BACKWARD,    // New schema can read old data
    FORWARD,     // Old schema can read new data  
    FULL,        // Both backward and forward
    NONE         // No compatibility checking
}

// Schema evolution helper
@Service
class SchemaEvolutionHelper {
    
    private val logger = LoggerFactory.getLogger(SchemaEvolutionHelper::class.java)
    
    fun planEvolution(
        currentSchema: String,
        proposedSchema: String
    ): EvolutionPlan {
        // This would contain actual schema parsing and comparison logic
        // For the workshop, we'll provide a simplified version
        
        return EvolutionPlan(
            isBreaking = false, // TODO: Implement actual breaking change detection
            addedFields = listOf("New fields detected"),
            removedFields = listOf("Removed fields detected"),
            modifiedFields = listOf("Modified fields detected"),
            recommendations = listOf(
                "Consider adding default values for new fields",
                "Mark removed fields as deprecated first",
                "Test with existing consumers before deploying"
            )
        )
    }
    
    fun validateEvolution(evolutionPlan: EvolutionPlan): Boolean {
        return !evolutionPlan.isBreaking && evolutionPlan.removedFields.isEmpty()
    }
}

data class EvolutionPlan(
    val isBreaking: Boolean,
    val addedFields: List<String>,
    val removedFields: List<String>,
    val modifiedFields: List<String>,
    val recommendations: List<String>
)