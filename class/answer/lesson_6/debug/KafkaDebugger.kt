package com.learning.KafkaStarter.debug

import com.fasterxml.jackson.databind.ObjectMapper
import org.apache.kafka.clients.consumer.Consumer
import org.apache.kafka.common.header.Headers
import org.slf4j.LoggerFactory
import org.slf4j.MDC
import org.springframework.stereotype.Component
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Component
class KafkaDebugger {
    
    private val logger = LoggerFactory.getLogger(KafkaDebugger::class.java)
    private val objectMapper = ObjectMapper()
    private val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS")
    
    fun inspectMessage(
        topic: String,
        partition: Int,
        offset: Long,
        key: String?,
        value: Any?,
        headers: Headers,
        correlationId: String = generateCorrelationId()
    ) {
        // Set correlation ID for tracking
        MDC.put("correlationId", correlationId)
        
        try {
            val headersMap = headers.associate { 
                it.key() to String(it.value()) 
            }
            
            val messageInfo = mapOf(
                "messageMetadata" to mapOf(
                    "topic" to topic,
                    "partition" to partition,
                    "offset" to offset,
                    "timestamp" to Instant.now().toString(),
                    "correlationId" to correlationId
                ),
                "messageContent" to mapOf(
                    "key" to key,
                    "keyType" to (key?.javaClass?.simpleName ?: "null"),
                    "value" to value,
                    "valueType" to (value?.javaClass?.simpleName ?: "null"),
                    "valueSize" to calculateMessageSize(value)
                ),
                "headers" to headersMap,
                "headerCount" to headersMap.size
            )
            
            logger.debug("📨 Message Inspection:\n{}", 
                objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(messageInfo))
            
            // Additional analysis
            analyzeMessagePattern(topic, key, value)
            validateMessageStructure(value)
            
        } catch (e: Exception) {
            logger.error("Failed to inspect message", e)
        } finally {
            MDC.remove("correlationId")
        }
    }
    
    fun inspectConsumerState(consumer: Consumer<*, *>, consumerGroupId: String) {
        try {
            val assignment = consumer.assignment()
            val positions = assignment.associate { partition ->
                "${partition.topic()}-${partition.partition()}" to consumer.position(partition)
            }
            val committed = consumer.committed(assignment).mapKeys { 
                "${it.key.topic()}-${it.key.partition()}" 
            }
            
            val consumerInfo = mapOf(
                "consumerGroup" to consumerGroupId,
                "assignment" to assignment.map { "${it.topic()}-${it.partition()}" },
                "assignmentCount" to assignment.size,
                "positions" to positions,
                "committed" to committed.mapValues { it.value?.offset() ?: -1 },
                "lag" to positions.mapValues { (key, position) ->
                    val committedOffset = committed[key]?.offset() ?: 0
                    maxOf(0, position - committedOffset)
                }
            )
            
            logger.debug("🔍 Consumer State Inspection:\n{}", 
                objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(consumerInfo))
            
            // Alert on high lag
            val totalLag = consumerInfo["lag"] as Map<*, *>
            val maxLag = totalLag.values.maxOfOrNull { it as Long } ?: 0
            if (maxLag > 1000) {
                logger.warn("🚨 High consumer lag detected: $maxLag messages")
            }
            
        } catch (e: Exception) {
            logger.error("Failed to inspect consumer state", e)
        }
    }
    
    fun traceMessageFlow(
        messageId: String,
        stage: String,
        details: Map<String, Any> = emptyMap()
    ) {
        val traceInfo = mapOf(
            "messageId" to messageId,
            "stage" to stage,
            "timestamp" to Instant.now().toString(),
            "details" to details
        )
        
        logger.info("🔄 Message Flow Trace: messageId=$messageId, stage=$stage, details=${objectMapper.writeValueAsString(details)}")
    }
    
    fun analyzePartitionDistribution(topic: String, messages: List<Pair<String?, Any>>) {
        val keyDistribution = messages.groupBy { it.first }
            .mapValues { it.value.size }
            .toList()
            .sortedByDescending { it.second }
        
        val analysis = mapOf(
            "topic" to topic,
            "totalMessages" to messages.size,
            "uniqueKeys" to keyDistribution.size,
            "topKeys" to keyDistribution.take(10),
            "evenDistribution" to isEvenlyDistributed(keyDistribution)
        )
        
        logger.info("📊 Partition Distribution Analysis:\n{}", 
            objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(analysis))
    }
    
    fun generateDiagnosticReport(
        topic: String,
        consumerGroup: String,
        timeWindowMinutes: Int = 5
    ): Map<String, Any> {
        val report = mapOf(
            "topic" to topic,
            "consumerGroup" to consumerGroup,
            "timeWindow" to "${timeWindowMinutes}m",
            "timestamp" to Instant.now().toString(),
            "recommendations" to generateRecommendations(topic, consumerGroup)
        )
        
        logger.info("📋 Diagnostic Report Generated:\n{}", 
            objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(report))
        
        return report
    }
    
    private fun analyzeMessagePattern(topic: String, key: String?, value: Any?) {
        // Analyze common patterns
        when {
            key == null -> logger.debug("⚠️ Message with null key detected on topic $topic")
            key.toString().length > 100 -> logger.debug("📏 Long key detected: ${key.toString().length} characters")
            value == null -> logger.warn("⚠️ Message with null value detected on topic $topic")
        }
        
        // Check for potential issues
        if (value is String && value.length > 1000000) { // 1MB
            logger.warn("📦 Large message detected: ${value.length} bytes")
        }
    }
    
    private fun validateMessageStructure(value: Any?) {
        try {
            when (value) {
                is Map<*, *> -> {
                    if (value.isEmpty()) {
                        logger.debug("📭 Empty message structure detected")
                    }
                    
                    // Check for required fields
                    val requiredFields = listOf("timestamp", "id", "type")
                    val missingFields = requiredFields.filter { !value.containsKey(it) }
                    if (missingFields.isNotEmpty()) {
                        logger.debug("🔍 Missing common fields: $missingFields")
                    }
                }
                is String -> {
                    if (value.startsWith("{") && value.endsWith("}")) {
                        try {
                            objectMapper.readTree(value)
                            logger.debug("✅ Valid JSON string detected")
                        } catch (e: Exception) {
                            logger.debug("❌ Invalid JSON string detected")
                        }
                    }
                }
            }
        } catch (e: Exception) {
            logger.debug("⚠️ Message structure validation failed: ${e.message}")
        }
    }
    
    private fun calculateMessageSize(value: Any?): Int {
        return try {
            when (value) {
                null -> 0
                is String -> value.toByteArray().size
                else -> objectMapper.writeValueAsBytes(value).size
            }
        } catch (e: Exception) {
            -1
        }
    }
    
    private fun isEvenlyDistributed(distribution: List<Pair<String?, Int>>): Boolean {
        if (distribution.isEmpty()) return true
        
        val average = distribution.sumOf { it.second } / distribution.size.toDouble()
        val variance = distribution.sumOf { (it.second - average) * (it.second - average) } / distribution.size
        
        return variance < average * 0.1 // Consider even if variance is less than 10% of average
    }
    
    private fun generateRecommendations(topic: String, consumerGroup: String): List<String> {
        val recommendations = mutableListOf<String>()
        
        // These would be based on actual analysis in a real implementation
        recommendations.add("Consider increasing consumer instances if lag is consistently high")
        recommendations.add("Monitor partition key distribution for hot partitions")
        recommendations.add("Implement proper error handling for poison messages")
        recommendations.add("Use correlation IDs for better message tracing")
        
        return recommendations
    }
    
    private fun generateCorrelationId(): String {
        return "trace-${System.currentTimeMillis()}-${(1000..9999).random()}"
    }
}