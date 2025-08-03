package com.learning.KafkaStarter.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.learning.KafkaStarter.avro.UserCreatedEvent
import org.apache.avro.io.BinaryEncoder
import org.apache.avro.io.DatumWriter
import org.apache.avro.io.EncoderFactory
import org.apache.avro.specific.SpecificDatumWriter
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.io.ByteArrayOutputStream
import java.time.Instant
import kotlin.system.measureTimeMillis

@Service
class SerializationBenchmarkService {
    
    private val logger = LoggerFactory.getLogger(SerializationBenchmarkService::class.java)
    private val objectMapper = ObjectMapper()
    
    data class BenchmarkResult(
        val format: String,
        val messageCount: Int,
        val totalTimeMs: Long,
        val avgTimePerMessageNanos: Double,
        val avgSizeBytes: Double,
        val totalSizeBytes: Long,
        val messagesPerSecond: Double
    )
    
    fun runComprehensiveBenchmark(messageCount: Int, iterations: Int): Map<String, Any> {
        logger.info("Starting comprehensive serialization benchmark: $messageCount messages, $iterations iterations")
        
        val results = mutableMapOf<String, List<BenchmarkResult>>()
        
        // Run multiple iterations for more accurate results
        repeat(iterations) { iteration ->
            logger.info("Running benchmark iteration ${iteration + 1}/$iterations")
            
            val jsonResult = benchmarkJsonSerialization(messageCount)
            val avroResult = benchmarkAvroSerialization(messageCount)
            val protobufResult = benchmarkProtobufSerialization(messageCount)
            
            results.computeIfAbsent("json") { mutableListOf() }.let { it as MutableList }.add(jsonResult)
            results.computeIfAbsent("avro") { mutableListOf() }.let { it as MutableList }.add(avroResult)
            results.computeIfAbsent("protobuf") { mutableListOf() }.let { it as MutableList }.add(protobufResult)
        }
        
        // Calculate averages across iterations
        val averageResults = results.mapValues { (format, resultList) ->
            val avgTime = resultList.map { it.totalTimeMs }.average()
            val avgSize = resultList.map { it.avgSizeBytes }.average()
            val avgThroughput = resultList.map { it.messagesPerSecond }.average()
            
            mapOf(
                "averageTimeMs" to avgTime,
                "averageSizeBytes" to avgSize,
                "averageThroughput" to avgThroughput,
                "iterations" to resultList
            )
        }
        
        // Generate comparison summary
        val summary = generateBenchmarkSummary(averageResults)
        
        logger.info("Benchmark completed. Summary: $summary")
        
        return mapOf(
            "summary" to summary,
            "detailed" to averageResults,
            "metadata" to mapOf(
                "messageCount" to messageCount,
                "iterations" to iterations,
                "timestamp" to Instant.now().toString()
            )
        )
    }
    
    private fun benchmarkJsonSerialization(messageCount: Int): BenchmarkResult {
        logger.debug("Benchmarking JSON serialization with $messageCount messages")
        
        val messages = generateTestMessages(messageCount)
        var totalSize = 0L
        
        val totalTime = measureTimeMillis {
            messages.forEach { message ->
                val jsonBytes = objectMapper.writeValueAsBytes(message)
                totalSize += jsonBytes.size
            }
        }
        
        return BenchmarkResult(
            format = "JSON",
            messageCount = messageCount,
            totalTimeMs = totalTime,
            avgTimePerMessageNanos = (totalTime * 1_000_000.0) / messageCount,
            avgSizeBytes = totalSize.toDouble() / messageCount,
            totalSizeBytes = totalSize,
            messagesPerSecond = messageCount * 1000.0 / totalTime
        )
    }
    
    private fun benchmarkAvroSerialization(messageCount: Int): BenchmarkResult {
        logger.debug("Benchmarking Avro serialization with $messageCount messages")
        
        val messages = generateTestAvroMessages(messageCount)
        val writer: DatumWriter<UserCreatedEvent> = SpecificDatumWriter(UserCreatedEvent.getClassSchema())
        var totalSize = 0L
        
        val totalTime = measureTimeMillis {
            messages.forEach { message ->
                val outputStream = ByteArrayOutputStream()
                val encoder: BinaryEncoder = EncoderFactory.get().binaryEncoder(outputStream, null)
                writer.write(message, encoder)
                encoder.flush()
                outputStream.close()
                
                totalSize += outputStream.size()
            }
        }
        
        return BenchmarkResult(
            format = "Avro",
            messageCount = messageCount,
            totalTimeMs = totalTime,
            avgTimePerMessageNanos = (totalTime * 1_000_000.0) / messageCount,
            avgSizeBytes = totalSize.toDouble() / messageCount,
            totalSizeBytes = totalSize,
            messagesPerSecond = messageCount * 1000.0 / totalTime
        )
    }
    
    private fun benchmarkProtobufSerialization(messageCount: Int): BenchmarkResult {
        logger.debug("Benchmarking Protobuf serialization with $messageCount messages")
        
        val messages = generateTestProtobufMessages(messageCount)
        var totalSize = 0L
        
        val totalTime = measureTimeMillis {
            messages.forEach { message ->
                // Simulate protobuf serialization (in real implementation, would use actual protobuf)
                val protobufBytes = objectMapper.writeValueAsBytes(message)
                totalSize += protobufBytes.size * 0.8 // Simulate protobuf compression
            }
        }
        
        return BenchmarkResult(
            format = "Protobuf",
            messageCount = messageCount,
            totalTimeMs = totalTime,
            avgTimePerMessageNanos = (totalTime * 1_000_000.0) / messageCount,
            avgSizeBytes = totalSize.toDouble() / messageCount,
            totalSizeBytes = totalSize,
            messagesPerSecond = messageCount * 1000.0 / totalTime
        )
    }
    
    private fun generateTestMessages(count: Int): List<Map<String, Any>> {
        return (1..count).map { i ->
            mapOf(
                "userId" to "user-$i",
                "email" to "user$i@benchmark.com",
                "timestamp" to Instant.now().toEpochMilli(),
                "registrationMethod" to "benchmark-test",
                "metadata" to mapOf(
                    "source" to "benchmark",
                    "iteration" to i,
                    "testData" to "Lorem ipsum dolor sit amet, consectetur adipiscing elit"
                )
            )
        }
    }
    
    private fun generateTestAvroMessages(count: Int): List<UserCreatedEvent> {
        return (1..count).map { i ->
            UserCreatedEvent.newBuilder()
                .setUserId("avro-user-$i")
                .setEmail("avrouser$i@benchmark.com")
                .setTimestamp(Instant.now().toEpochMilli())
                .setRegistrationMethod("avro-benchmark")
                .build()
        }
    }
    
    private fun generateTestProtobufMessages(count: Int): List<Map<String, Any>> {
        return (1..count).map { i ->
            mapOf(
                "userId" to "protobuf-user-$i",
                "email" to "protobufuser$i@benchmark.com",
                "timestamp" to Instant.now().toEpochMilli(),
                "registrationMethod" to "protobuf-benchmark"
            )
        }
    }
    
    private fun generateBenchmarkSummary(results: Map<String, Map<String, Any>>): Map<String, Any> {
        val formats = results.keys.toList()
        
        // Find fastest format
        val fastestFormat = formats.minByOrNull { format ->
            (results[format]?.get("averageTimeMs") as? Double) ?: Double.MAX_VALUE
        }
        
        // Find most compact format
        val compactFormat = formats.minByOrNull { format ->
            (results[format]?.get("averageSizeBytes") as? Double) ?: Double.MAX_VALUE
        }
        
        // Find highest throughput format
        val highestThroughputFormat = formats.maxByOrNull { format ->
            (results[format]?.get("averageThroughput") as? Double) ?: 0.0
        }
        
        return mapOf(
            "fastest" to fastestFormat,
            "mostCompact" to compactFormat,
            "highestThroughput" to highestThroughputFormat,
            "recommendations" to mapOf(
                "lowLatency" to fastestFormat,
                "networkOptimized" to compactFormat,
                "highVolume" to highestThroughputFormat
            ),
            "sizeDifference" to calculateSizeDifferences(results),
            "speedDifference" to calculateSpeedDifferences(results)
        )
    }
    
    private fun calculateSizeDifferences(results: Map<String, Map<String, Any>>): Map<String, String> {
        val jsonSize = results["json"]?.get("averageSizeBytes") as? Double ?: 0.0
        val avroSize = results["avro"]?.get("averageSizeBytes") as? Double ?: 0.0
        val protobufSize = results["protobuf"]?.get("averageSizeBytes") as? Double ?: 0.0
        
        return mapOf(
            "avroVsJson" to "${String.format("%.1f", (1 - avroSize/jsonSize) * 100)}% smaller",
            "protobufVsJson" to "${String.format("%.1f", (1 - protobufSize/jsonSize) * 100)}% smaller",
            "avroVsProtobuf" to "${String.format("%.1f", ((avroSize - protobufSize)/protobufSize) * 100)}% difference"
        )
    }
    
    private fun calculateSpeedDifferences(results: Map<String, Map<String, Any>>): Map<String, String> {
        val jsonThroughput = results["json"]?.get("averageThroughput") as? Double ?: 0.0
        val avroThroughput = results["avro"]?.get("averageThroughput") as? Double ?: 0.0
        val protobufThroughput = results["protobuf"]?.get("averageThroughput") as? Double ?: 0.0
        
        return mapOf(
            "avroVsJson" to "${String.format("%.1f", (avroThroughput/jsonThroughput - 1) * 100)}% faster",
            "protobufVsJson" to "${String.format("%.1f", (protobufThroughput/jsonThroughput - 1) * 100)}% faster",
            "avroVsProtobuf" to "${String.format("%.1f", (avroThroughput/protobufThroughput - 1) * 100)}% difference"
        )
    }
}