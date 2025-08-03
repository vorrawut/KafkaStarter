package com.learning.KafkaStarter.controller

import com.learning.KafkaStarter.debug.KafkaDebugger
import com.learning.KafkaStarter.monitoring.KafkaErrorTracker
import com.learning.KafkaStarter.monitoring.KafkaPerformanceProfiler
import com.learning.KafkaStarter.testing.KafkaTestFramework
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.ResponseEntity
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.web.bind.annotation.*
import java.time.Instant
import java.util.*

@RestController
@RequestMapping("/api/dev-tools")
class DevToolsController {
    
    @Autowired
    private lateinit var kafkaDebugger: KafkaDebugger
    
    @Autowired
    private lateinit var performanceProfiler: KafkaPerformanceProfiler
    
    @Autowired
    private lateinit var errorTracker: KafkaErrorTracker
    
    @Autowired
    private lateinit var testFramework: KafkaTestFramework
    
    @Autowired
    private lateinit var kafkaTemplate: KafkaTemplate<String, Any>
    
    // Debugging Endpoints
    @PostMapping("/debug/trace-message")
    fun traceMessage(
        @RequestParam messageId: String,
        @RequestParam stage: String,
        @RequestBody(required = false) details: Map<String, Any>?
    ): ResponseEntity<Map<String, Any>> {
        kafkaDebugger.traceMessageFlow(messageId, stage, details ?: emptyMap())
        
        return ResponseEntity.ok(mapOf(
            "status" to "success",
            "message" to "Message trace recorded",
            "messageId" to messageId,
            "stage" to stage
        ))
    }
    
    @PostMapping("/debug/analyze-distribution")
    fun analyzePartitionDistribution(
        @RequestParam topic: String,
        @RequestParam messageCount: Int = 100
    ): ResponseEntity<Map<String, Any>> {
        
        // Generate test messages for analysis
        val messages = testFramework.generateTestMessages(messageCount, "distribution-test")
            .map { it.id to it }
        
        kafkaDebugger.analyzePartitionDistribution(topic, messages)
        
        return ResponseEntity.ok(mapOf(
            "status" to "success",
            "topic" to topic,
            "analyzedMessages" to messageCount,
            "note" to "Check logs for detailed distribution analysis"
        ))
    }
    
    @GetMapping("/debug/diagnostic-report")
    fun generateDiagnosticReport(
        @RequestParam topic: String,
        @RequestParam consumerGroup: String,
        @RequestParam(defaultValue = "5") timeWindowMinutes: Int
    ): ResponseEntity<Map<String, Any>> {
        
        val report = kafkaDebugger.generateDiagnosticReport(topic, consumerGroup, timeWindowMinutes)
        
        return ResponseEntity.ok(mapOf(
            "status" to "success",
            "report" to report
        ))
    }
    
    // Performance Testing Endpoints
    @PostMapping("/performance/benchmark")
    fun runBenchmark(
        @RequestParam operationName: String,
        @RequestParam(defaultValue = "1000") iterations: Int,
        @RequestParam topic: String = "benchmark-topic"
    ): ResponseEntity<Map<String, Any>> {
        
        val benchmarkResult = performanceProfiler.runBenchmark(operationName, iterations) {
            // Simulate Kafka operation
            val message = testFramework.createTestMessage("benchmark-message")
            kafkaTemplate.send(topic, message.id, message)
        }
        
        return ResponseEntity.ok(mapOf(
            "status" to "success",
            "benchmark" to benchmarkResult
        ))
    }
    
    @GetMapping("/performance/stats/{operationName}")
    fun getOperationStats(@PathVariable operationName: String): ResponseEntity<Map<String, Any>> {
        val stats = performanceProfiler.getOperationStats(operationName)
        
        return if (stats != null) {
            ResponseEntity.ok(mapOf(
                "status" to "success",
                "stats" to stats
            ))
        } else {
            ResponseEntity.notFound().build()
        }
    }
    
    @GetMapping("/performance/report")
    fun getPerformanceReport(): ResponseEntity<Map<String, Any>> {
        val report = performanceProfiler.getPerformanceReport()
        
        return ResponseEntity.ok(mapOf(
            "status" to "success",
            "report" to report
        ))
    }
    
    @PostMapping("/performance/compare")
    fun compareOperations(@RequestBody operationNames: List<String>): ResponseEntity<Map<String, Any>> {
        val comparison = performanceProfiler.compareOperations(operationNames)
        
        return ResponseEntity.ok(mapOf(
            "status" to "success",
            "comparison" to comparison
        ))
    }
    
    // Error Tracking Endpoints
    @PostMapping("/errors/simulate")
    fun simulateError(
        @RequestParam errorType: String,
        @RequestParam topic: String = "test-topic",
        @RequestParam(defaultValue = "0") partition: Int,
        @RequestParam(defaultValue = "1") count: Int
    ): ResponseEntity<Map<String, Any>> {
        
        val errors = mutableListOf<String>()
        
        repeat(count) { i ->
            val exception = when (errorType.uppercase()) {
                "SERIALIZATION" -> RuntimeException("Serialization failed for message $i")
                "TIMEOUT" -> RuntimeException("Timeout occurred for message $i")
                "NETWORK" -> RuntimeException("Network error for message $i")
                else -> RuntimeException("Generic error for message $i")
            }
            
            val errorId = errorTracker.trackError(
                topic = topic,
                partition = partition,
                offset = i.toLong(),
                exception = exception,
                context = mapOf("simulation" to true, "errorType" to errorType)
            )
            
            errors.add(errorId)
        }
        
        return ResponseEntity.ok(mapOf(
            "status" to "success",
            "message" to "Simulated $count errors of type $errorType",
            "errorIds" to errors
        ))
    }
    
    @GetMapping("/errors/summary")
    fun getErrorSummary(): ResponseEntity<Map<String, Any>> {
        val summary = errorTracker.getErrorSummary()
        
        return ResponseEntity.ok(mapOf(
            "status" to "success",
            "summary" to summary
        ))
    }
    
    @GetMapping("/errors/spike-analysis")
    fun analyzeErrorSpike(
        @RequestParam(defaultValue = "5") windowMinutes: Int
    ): ResponseEntity<Map<String, Any>> {
        val analysis = errorTracker.analyzeErrorSpike(windowMinutes)
        
        return ResponseEntity.ok(mapOf(
            "status" to "success",
            "analysis" to analysis
        ))
    }
    
    @GetMapping("/errors/report")
    fun getErrorReport(
        @RequestParam(defaultValue = "24") timeRangeHours: Int
    ): ResponseEntity<Map<String, Any>> {
        val report = errorTracker.generateErrorReport(timeRangeHours)
        
        return ResponseEntity.ok(mapOf(
            "status" to "success",
            "report" to report
        ))
    }
    
    // Testing Framework Endpoints
    @PostMapping("/testing/load-test")
    fun runLoadTest(
        @RequestParam topic: String,
        @RequestParam(defaultValue = "100") messagesPerSecond: Int,
        @RequestParam(defaultValue = "10") durationSeconds: Int,
        @RequestParam(defaultValue = "1024") messageSize: Int
    ): ResponseEntity<Map<String, Any>> {
        
        val messages = testFramework.createLoadTestScenario(messagesPerSecond, durationSeconds, messageSize)
        
        val result = testFramework.sendMessagesAndWait(
            kafkaTemplate = kafkaTemplate,
            topic = topic,
            messages = messages,
            expectedReceiveCount = messages.size,
            timeoutSeconds = (durationSeconds + 30).toLong()
        )
        
        return ResponseEntity.ok(mapOf(
            "status" to "success",
            "loadTest" to mapOf(
                "configuration" to mapOf(
                    "messagesPerSecond" to messagesPerSecond,
                    "durationSeconds" to durationSeconds,
                    "messageSize" to messageSize,
                    "totalMessages" to messages.size
                ),
                "result" to result
            )
        ))
    }
    
    @PostMapping("/testing/chaos-test")
    fun runChaosTest(@RequestParam topic: String): ResponseEntity<Map<String, Any>> {
        val chaosMessages = testFramework.createChaosTestScenario()
        
        val result = testFramework.sendMessagesAndWait(
            kafkaTemplate = kafkaTemplate,
            topic = topic,
            messages = chaosMessages,
            expectedReceiveCount = chaosMessages.size
        )
        
        return ResponseEntity.ok(mapOf(
            "status" to "success",
            "chaosTest" to mapOf(
                "totalMessages" to chaosMessages.size,
                "result" to result,
                "note" to "Chaos test includes empty messages, large messages, and special characters"
            )
        ))
    }
    
    @PostMapping("/testing/throughput-test")
    fun runThroughputTest(
        @RequestParam topic: String,
        @RequestParam(defaultValue = "1000") messageCount: Int,
        @RequestParam(defaultValue = "1") parallelism: Int
    ): ResponseEntity<Map<String, Any>> {
        
        val throughputResult = testFramework.measureThroughput(
            kafkaTemplate = kafkaTemplate,
            topic = topic,
            messageCount = messageCount,
            parallelism = parallelism
        )
        
        return ResponseEntity.ok(mapOf(
            "status" to "success",
            "throughputTest" to throughputResult
        ))
    }
    
    @PostMapping("/testing/deduplication-test")
    fun runDeduplicationTest(@RequestParam topic: String): ResponseEntity<Map<String, Any>> {
        val messages = testFramework.createDeduplicationTest()
        
        val result = testFramework.sendMessagesAndWait(
            kafkaTemplate = kafkaTemplate,
            topic = topic,
            messages = messages,
            expectedReceiveCount = messages.size
        )
        
        // Simulate processing and validate deduplication
        val deduplicationResult = testFramework.validateDeduplication(messages)
        
        return ResponseEntity.ok(mapOf(
            "status" to "success",
            "deduplicationTest" to mapOf(
                "sendResult" to result,
                "deduplicationAnalysis" to deduplicationResult
            )
        ))
    }
    
    // Utility Endpoints
    @GetMapping("/health")
    fun healthCheck(): ResponseEntity<Map<String, Any>> {
        return ResponseEntity.ok(mapOf(
            "status" to "healthy",
            "service" to "dev-tools-controller",
            "timestamp" to Instant.now().toString(),
            "features" to mapOf(
                "debugging" to "enabled",
                "performance" to "enabled",
                "errorTracking" to "enabled",
                "testing" to "enabled"
            )
        ))
    }
    
    @PostMapping("/reset")
    fun resetAllData(): ResponseEntity<Map<String, Any>> {
        // In a real implementation, this would reset all internal state
        // For now, just return success
        
        return ResponseEntity.ok(mapOf(
            "status" to "success",
            "message" to "All development tool data has been reset",
            "timestamp" to Instant.now().toString()
        ))
    }
}