package com.learning.KafkaStarter.service

import org.apache.avro.Schema
import org.apache.avro.generic.GenericData
import org.apache.avro.generic.GenericRecord
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.stereotype.Service
import java.time.Instant

@Service
class SchemaEvolutionService {
    
    private val logger = LoggerFactory.getLogger(SchemaEvolutionService::class.java)
    
    @Autowired
    private lateinit var avroKafkaTemplate: KafkaTemplate<String, Any>
    
    // Version 1 Schema (Original)
    private val schemaV1 = Schema.Parser().parse("""
        {
          "type": "record",
          "name": "UserEvent",
          "namespace": "com.learning.evolution",
          "fields": [
            {"name": "userId", "type": "string"},
            {"name": "email", "type": "string"},
            {"name": "timestamp", "type": "long"}
          ]
        }
    """.trimIndent())
    
    // Version 2 Schema (Backward Compatible - Added optional field)
    private val schemaV2 = Schema.Parser().parse("""
        {
          "type": "record",
          "name": "UserEvent",
          "namespace": "com.learning.evolution",
          "fields": [
            {"name": "userId", "type": "string"},
            {"name": "email", "type": "string"},
            {"name": "timestamp", "type": "long"},
            {"name": "registrationMethod", "type": "string", "default": "unknown"}
          ]
        }
    """.trimIndent())
    
    // Version 3 Schema (Forward Compatible - Added union field)
    private val schemaV3 = Schema.Parser().parse("""
        {
          "type": "record",
          "name": "UserEvent",
          "namespace": "com.learning.evolution",
          "fields": [
            {"name": "userId", "type": "string"},
            {"name": "email", "type": "string"},
            {"name": "timestamp", "type": "long"},
            {"name": "registrationMethod", "type": "string", "default": "unknown"},
            {"name": "metadata", "type": ["null", "string"], "default": null}
          ]
        }
    """.trimIndent())
    
    fun demonstrateSchemaEvolution() {
        logger.info("Starting schema evolution demonstration...")
        
        // Send messages with different schema versions
        sendMessageV1()
        Thread.sleep(1000)
        
        sendMessageV2()
        Thread.sleep(1000)
        
        sendMessageV3()
        
        logger.info("Schema evolution demonstration completed")
    }
    
    fun sendMessageV1() {
        val record = GenericData.Record(schemaV1).apply {
            put("userId", "user-v1-${System.currentTimeMillis()}")
            put("email", "userv1@example.com")
            put("timestamp", Instant.now().toEpochMilli())
        }
        
        logger.info("Sending message with Schema V1")
        avroKafkaTemplate.send("user-evolution-topic", "v1", record)
            .whenComplete { result, ex ->
                if (ex == null) {
                    logger.info("Successfully sent V1 message")
                } else {
                    logger.error("Failed to send V1 message", ex)
                }
            }
    }
    
    fun sendMessageV2() {
        val record = GenericData.Record(schemaV2).apply {
            put("userId", "user-v2-${System.currentTimeMillis()}")
            put("email", "userv2@example.com")
            put("timestamp", Instant.now().toEpochMilli())
            put("registrationMethod", "web-form")
        }
        
        logger.info("Sending message with Schema V2 (added registrationMethod)")
        avroKafkaTemplate.send("user-evolution-topic", "v2", record)
            .whenComplete { result, ex ->
                if (ex == null) {
                    logger.info("Successfully sent V2 message")
                } else {
                    logger.error("Failed to send V2 message", ex)
                }
            }
    }
    
    fun sendMessageV3() {
        val record = GenericData.Record(schemaV3).apply {
            put("userId", "user-v3-${System.currentTimeMillis()}")
            put("email", "userv3@example.com")
            put("timestamp", Instant.now().toEpochMilli())
            put("registrationMethod", "mobile-app")
            put("metadata", """{"source": "mobile", "version": "1.2.3"}""")
        }
        
        logger.info("Sending message with Schema V3 (added metadata)")
        avroKafkaTemplate.send("user-evolution-topic", "v3", record)
            .whenComplete { result, ex ->
                if (ex == null) {
                    logger.info("Successfully sent V3 message")
                } else {
                    logger.error("Failed to send V3 message", ex)
                }
            }
    }
    
    fun demonstrateCompatibilityTypes() {
        logger.info("Demonstrating schema compatibility types...")
        
        // Backward Compatibility: New schema can read old data
        demonstrateBackwardCompatibility()
        
        // Forward Compatibility: Old schema can read new data
        demonstrateForwardCompatibility()
        
        // Full Compatibility: Both directions work
        demonstrateFullCompatibility()
    }
    
    private fun demonstrateBackwardCompatibility() {
        logger.info("=== Backward Compatibility Demo ===")
        
        // Producer uses old schema (V1)
        val oldMessage = GenericData.Record(schemaV1).apply {
            put("userId", "backward-test")
            put("email", "backward@test.com")
            put("timestamp", Instant.now().toEpochMilli())
        }
        
        // Consumer can read with new schema (V2) because of default value
        logger.info("Sending old format message that new consumers can read")
        avroKafkaTemplate.send("compatibility-test", "backward", oldMessage)
    }
    
    private fun demonstrateForwardCompatibility() {
        logger.info("=== Forward Compatibility Demo ===")
        
        // Producer uses new schema (V2)
        val newMessage = GenericData.Record(schemaV2).apply {
            put("userId", "forward-test")
            put("email", "forward@test.com")
            put("timestamp", Instant.now().toEpochMilli())
            put("registrationMethod", "api")
        }
        
        // Old consumers can read this by ignoring new fields
        logger.info("Sending new format message that old consumers can read")
        avroKafkaTemplate.send("compatibility-test", "forward", newMessage)
    }
    
    private fun demonstrateFullCompatibility() {
        logger.info("=== Full Compatibility Demo ===")
        
        // Schema V3 with union types for full compatibility
        val compatibleMessage = GenericData.Record(schemaV3).apply {
            put("userId", "full-compat-test")
            put("email", "fullcompat@test.com")
            put("timestamp", Instant.now().toEpochMilli())
            put("registrationMethod", "social-login")
            put("metadata", null) // Optional field
        }
        
        logger.info("Sending fully compatible message")
        avroKafkaTemplate.send("compatibility-test", "full", compatibleMessage)
    }
    
    fun simulateSchemaConflict() {
        logger.info("Simulating schema compatibility conflict...")
        
        // This would cause an incompatible schema change
        val incompatibleSchema = Schema.Parser().parse("""
            {
              "type": "record",
              "name": "UserEvent",
              "namespace": "com.learning.evolution",
              "fields": [
                {"name": "userId", "type": "int"},
                {"name": "email", "type": "string"},
                {"name": "timestamp", "type": "long"}
              ]
            }
        """.trimIndent())
        
        try {
            val record = GenericData.Record(incompatibleSchema).apply {
                put("userId", 12345) // Changed from string to int - incompatible!
                put("email", "conflict@test.com")
                put("timestamp", Instant.now().toEpochMilli())
            }
            
            logger.warn("Attempting to send incompatible message...")
            avroKafkaTemplate.send("user-evolution-topic", "conflict", record)
            
        } catch (e: Exception) {
            logger.error("Schema conflict detected (expected): ${e.message}")
        }
    }
}