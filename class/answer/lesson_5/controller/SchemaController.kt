package com.learning.KafkaStarter.controller

import com.learning.KafkaStarter.service.SchemaAwareProducer
import com.learning.KafkaStarter.service.SchemaEvolutionService
import com.learning.KafkaStarter.service.SerializationBenchmarkService
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.util.UUID

@RestController
@RequestMapping("/api/schema")
class SchemaController {
    
    private val logger = LoggerFactory.getLogger(SchemaController::class.java)
    
    @Autowired
    private lateinit var schemaAwareProducer: SchemaAwareProducer
    
    @Autowired
    private lateinit var schemaEvolutionService: SchemaEvolutionService
    
    @Autowired
    private lateinit var serializationBenchmarkService: SerializationBenchmarkService
    
    @PostMapping("/avro/user")
    fun createUserWithAvro(
        @RequestParam userId: String = UUID.randomUUID().toString(),
        @RequestParam email: String,
        @RequestParam method: String = "api"
    ): ResponseEntity<Map<String, Any>> {
        
        return try {
            schemaAwareProducer.sendAvroMessage(userId, email, method)
            
            ResponseEntity.ok(mapOf(
                "status" to "success",
                "message" to "Avro user event sent",
                "userId" to userId,
                "email" to email,
                "method" to method,
                "format" to "avro-specific"
            ))
        } catch (e: Exception) {
            logger.error("Failed to send Avro message", e)
            ResponseEntity.status(500).body(mapOf(
                "status" to "error",
                "message" to "Failed to send Avro message: ${e.message}"
            ))
        }
    }
    
    @PostMapping("/avro/generic/user")
    fun createUserWithGenericAvro(
        @RequestParam userId: String = UUID.randomUUID().toString(),
        @RequestParam email: String
    ): ResponseEntity<Map<String, Any>> {
        
        return try {
            schemaAwareProducer.sendGenericAvroMessage(userId, email)
            
            ResponseEntity.ok(mapOf(
                "status" to "success",
                "message" to "Generic Avro user event sent",
                "userId" to userId,
                "email" to email,
                "format" to "avro-generic"
            ))
        } catch (e: Exception) {
            logger.error("Failed to send Generic Avro message", e)
            ResponseEntity.status(500).body(mapOf(
                "status" to "error",
                "message" to "Failed to send Generic Avro message: ${e.message}"
            ))
        }
    }
    
    @PostMapping("/protobuf/user")
    fun createUserWithProtobuf(
        @RequestParam userId: String = UUID.randomUUID().toString(),
        @RequestParam email: String
    ): ResponseEntity<Map<String, Any>> {
        
        return try {
            schemaAwareProducer.sendProtobufMessage(userId, email)
            
            ResponseEntity.ok(mapOf(
                "status" to "success",
                "message" to "Protobuf user event sent",
                "userId" to userId,
                "email" to email,
                "format" to "protobuf"
            ))
        } catch (e: Exception) {
            logger.error("Failed to send Protobuf message", e)
            ResponseEntity.status(500).body(mapOf(
                "status" to "error",
                "message" to "Failed to send Protobuf message: ${e.message}"
            ))
        }
    }
    
    @PostMapping("/avro/batch")
    fun createBatchUsers(@RequestParam count: Int = 10): ResponseEntity<Map<String, Any>> {
        
        return try {
            schemaAwareProducer.sendBatchAvroMessages(count)
            
            ResponseEntity.ok(mapOf(
                "status" to "success",
                "message" to "Batch Avro messages sent",
                "count" to count,
                "format" to "avro-batch"
            ))
        } catch (e: Exception) {
            logger.error("Failed to send batch Avro messages", e)
            ResponseEntity.status(500).body(mapOf(
                "status" to "error",
                "message" to "Failed to send batch messages: ${e.message}"
            ))
        }
    }
    
    @PostMapping("/evolution/demo")
    fun demonstrateSchemaEvolution(): ResponseEntity<Map<String, Any>> {
        
        return try {
            schemaEvolutionService.demonstrateSchemaEvolution()
            
            ResponseEntity.ok(mapOf(
                "status" to "success",
                "message" to "Schema evolution demonstration completed",
                "description" to "Check logs for detailed evolution steps"
            ))
        } catch (e: Exception) {
            logger.error("Failed to demonstrate schema evolution", e)
            ResponseEntity.status(500).body(mapOf(
                "status" to "error",
                "message" to "Schema evolution demo failed: ${e.message}"
            ))
        }
    }
    
    @PostMapping("/compatibility/demo")
    fun demonstrateCompatibility(): ResponseEntity<Map<String, Any>> {
        
        return try {
            schemaEvolutionService.demonstrateCompatibilityTypes()
            
            ResponseEntity.ok(mapOf(
                "status" to "success",
                "message" to "Schema compatibility demonstration completed",
                "types" to listOf("backward", "forward", "full")
            ))
        } catch (e: Exception) {
            logger.error("Failed to demonstrate compatibility", e)
            ResponseEntity.status(500).body(mapOf(
                "status" to "error",
                "message" to "Compatibility demo failed: ${e.message}"
            ))
        }
    }
    
    @PostMapping("/conflict/simulate")
    fun simulateSchemaConflict(): ResponseEntity<Map<String, Any>> {
        
        return try {
            schemaEvolutionService.simulateSchemaConflict()
            
            ResponseEntity.ok(mapOf(
                "status" to "success",
                "message" to "Schema conflict simulation completed",
                "note" to "Check logs for conflict details"
            ))
        } catch (e: Exception) {
            logger.error("Schema conflict simulation error", e)
            ResponseEntity.status(500).body(mapOf(
                "status" to "error",
                "message" to "Conflict simulation failed: ${e.message}"
            ))
        }
    }
    
    @GetMapping("/benchmark")
    fun runSerializationBenchmark(
        @RequestParam messageCount: Int = 1000,
        @RequestParam iterations: Int = 3
    ): ResponseEntity<Map<String, Any>> {
        
        return try {
            val results = serializationBenchmarkService.runComprehensiveBenchmark(messageCount, iterations)
            
            ResponseEntity.ok(mapOf(
                "status" to "success",
                "message" to "Serialization benchmark completed",
                "results" to results,
                "messageCount" to messageCount,
                "iterations" to iterations
            ))
        } catch (e: Exception) {
            logger.error("Failed to run serialization benchmark", e)
            ResponseEntity.status(500).body(mapOf(
                "status" to "error",
                "message" to "Benchmark failed: ${e.message}"
            ))
        }
    }
    
    @GetMapping("/health")
    fun healthCheck(): ResponseEntity<Map<String, Any>> {
        return ResponseEntity.ok(mapOf(
            "status" to "healthy",
            "service" to "schema-controller",
            "timestamp" to System.currentTimeMillis(),
            "features" to mapOf(
                "avro" to "enabled",
                "protobuf" to "enabled",
                "schemaRegistry" to "enabled",
                "evolution" to "enabled",
                "benchmarking" to "enabled"
            )
        ))
    }
}