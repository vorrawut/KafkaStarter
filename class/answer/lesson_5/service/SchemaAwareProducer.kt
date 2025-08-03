package com.learning.KafkaStarter.service

import com.learning.KafkaStarter.avro.UserCreatedEvent
import org.apache.avro.generic.GenericData
import org.apache.avro.generic.GenericRecord
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.kafka.support.SendResult
import org.springframework.stereotype.Service
import java.time.Instant
import java.util.UUID
import java.util.concurrent.CompletableFuture

@Service
class SchemaAwareProducer {
    
    private val logger = LoggerFactory.getLogger(SchemaAwareProducer::class.java)
    
    @Autowired
    private lateinit var avroKafkaTemplate: KafkaTemplate<String, Any>
    
    fun sendAvroMessage(userId: String, email: String, registrationMethod: String) {
        val avroEvent = UserCreatedEvent.newBuilder()
            .setUserId(userId)
            .setEmail(email)
            .setTimestamp(Instant.now().toEpochMilli())
            .setRegistrationMethod(registrationMethod)
            .build()
        
        logger.info("Sending Avro message for user: $userId")
        
        val future: CompletableFuture<SendResult<String, Any>> = avroKafkaTemplate.send("user-events-avro", userId, avroEvent)
        
        future.whenComplete { result, ex ->
            if (ex == null) {
                logger.info(
                    "Successfully sent Avro message for user $userId to partition ${result.recordMetadata.partition()} " +
                    "at offset ${result.recordMetadata.offset()}"
                )
            } else {
                logger.error("Failed to send Avro message for user $userId", ex)
            }
        }
    }
    
    fun sendGenericAvroMessage(userId: String, email: String) {
        // Create Generic Avro Record (without generated classes)
        val schema = avroKafkaTemplate.producerFactory.createProducer().use { producer ->
            // This would typically come from Schema Registry
            org.apache.avro.Schema.Parser().parse("""
                {
                  "type": "record",
                  "name": "UserCreatedEvent",
                  "namespace": "com.learning.KafkaStarter.avro",
                  "fields": [
                    {"name": "userId", "type": "string"},
                    {"name": "email", "type": "string"},
                    {"name": "timestamp", "type": "long"},
                    {"name": "registrationMethod", "type": "string"}
                  ]
                }
            """.trimIndent())
        }
        
        val genericRecord: GenericRecord = GenericData.Record(schema).apply {
            put("userId", userId)
            put("email", email)
            put("timestamp", Instant.now().toEpochMilli())
            put("registrationMethod", "generic-avro")
        }
        
        logger.info("Sending Generic Avro message for user: $userId")
        
        avroKafkaTemplate.send("user-events-generic", userId, genericRecord)
            .whenComplete { result, ex ->
                if (ex == null) {
                    logger.info("Successfully sent Generic Avro message for user $userId")
                } else {
                    logger.error("Failed to send Generic Avro message for user $userId", ex)
                }
            }
    }
    
    fun sendProtobufMessage(userId: String, email: String) {
        // This would use generated Protobuf classes
        // For now, we'll simulate with a map since we don't have full protobuf setup
        val protobufEvent = mapOf(
            "userId" to userId,
            "email" to email,
            "timestamp" to Instant.now().toEpochMilli(),
            "registrationMethod" to "protobuf"
        )
        
        logger.info("Sending Protobuf message for user: $userId")
        
        // Note: In a real implementation, this would use protobufKafkaTemplate
        avroKafkaTemplate.send("user-events-protobuf", userId, protobufEvent)
            .whenComplete { result, ex ->
                if (ex == null) {
                    logger.info("Successfully sent Protobuf message for user $userId")
                } else {
                    logger.error("Failed to send Protobuf message for user $userId", ex)
                }
            }
    }
    
    fun sendBatchAvroMessages(userCount: Int) {
        logger.info("Sending batch of $userCount Avro messages")
        
        val futures = mutableListOf<CompletableFuture<SendResult<String, Any>>>()
        
        repeat(userCount) { i ->
            val userId = "batch-user-${UUID.randomUUID()}"
            val email = "user$i@batch-test.com"
            
            val avroEvent = UserCreatedEvent.newBuilder()
                .setUserId(userId)
                .setEmail(email)
                .setTimestamp(Instant.now().toEpochMilli())
                .setRegistrationMethod("batch-creation")
                .build()
            
            val future = avroKafkaTemplate.send("user-events-avro", userId, avroEvent)
            futures.add(future)
        }
        
        // Wait for all messages to complete
        CompletableFuture.allOf(*futures.toTypedArray())
            .whenComplete { _, ex ->
                if (ex == null) {
                    logger.info("Successfully sent all $userCount batch messages")
                } else {
                    logger.error("Failed to send some batch messages", ex)
                }
            }
    }
}