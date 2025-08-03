package com.learning.KafkaStarter.monitoring

import org.springframework.stereotype.Component
import java.time.Instant
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.atomic.LongAdder
import kotlin.math.sqrt

@Component
class KafkaPerformanceProfiler {
    
    private val operationTimings = ConcurrentHashMap<String, ConcurrentLinkedQueue<Long>>()
    private val operationCounts = ConcurrentHashMap<String, LongAdder>()
    private val operationErrors = ConcurrentHashMap<String, AtomicLong>()
    private val memorySnapshots = ConcurrentLinkedQueue<MemorySnapshot>()
    
    data class MemorySnapshot(
        val timestamp: Instant,
        val heapUsed: Long,
        val heapMax: Long,
        val nonHeapUsed: Long
    )
    
    data class OperationStats(
        val operationName: String,
        val count: Long,
        val errors: Long,
        val averageTimeNanos: Double,
        val minTimeNanos: Long,
        val maxTimeNanos: Long,
        val percentile95Nanos: Long,
        val percentile99Nanos: Long,
        val throughputPerSecond: Double,
        val errorRate: Double
    )
    
    fun <T> profileOperation(operationName: String, operation: () -> T): T {
        val startTime = System.nanoTime()
        val startMemory = Runtime.getRuntime().let { 
            it.totalMemory() - it.freeMemory() 
        }
        
        try {
            val result = operation()
            
            val duration = System.nanoTime() - startTime
            recordTiming(operationName, duration)
            recordMemoryUsage(startMemory)
            
            // Log slow operations
            if (duration > 1_000_000_000) { // 1 second
                println("⚠️ Slow operation detected: $operationName took ${duration / 1_000_000}ms")
            }
            
            return result
            
        } catch (e: Exception) {
            val duration = System.nanoTime() - startTime
            recordTiming(operationName, duration)
            recordError(operationName)
            recordMemoryUsage(startMemory)
            throw e
        }
    }
    
    fun recordTiming(operationName: String, durationNanos: Long) {
        operationTimings.computeIfAbsent(operationName) { 
            ConcurrentLinkedQueue() 
        }.offer(durationNanos)
        
        operationCounts.computeIfAbsent(operationName) { 
            LongAdder() 
        }.increment()
        
        // Keep only recent timings (last 1000 operations)
        val timings = operationTimings[operationName]!!
        while (timings.size > 1000) {
            timings.poll()
        }
    }
    
    fun recordError(operationName: String) {
        operationErrors.computeIfAbsent(operationName) { 
            AtomicLong(0) 
        }.incrementAndGet()
    }
    
    fun recordMemoryUsage(heapUsed: Long) {
        val runtime = Runtime.getRuntime()
        val snapshot = MemorySnapshot(
            timestamp = Instant.now(),
            heapUsed = heapUsed,
            heapMax = runtime.maxMemory(),
            nonHeapUsed = 0 // Simplified for this example
        )
        
        memorySnapshots.offer(snapshot)
        
        // Keep only recent snapshots (last 100)
        while (memorySnapshots.size > 100) {
            memorySnapshots.poll()
        }
    }
    
    fun getOperationStats(operationName: String): OperationStats? {
        val timings = operationTimings[operationName] ?: return null
        val timingsList = timings.toList()
        
        if (timingsList.isEmpty()) return null
        
        val count = operationCounts[operationName]?.sum() ?: 0
        val errors = operationErrors[operationName]?.get() ?: 0
        
        val sortedTimings = timingsList.sorted()
        val average = timingsList.average()
        val min = sortedTimings.first()
        val max = sortedTimings.last()
        
        val p95Index = (sortedTimings.size * 0.95).toInt()
        val p99Index = (sortedTimings.size * 0.99).toInt()
        val p95 = if (p95Index < sortedTimings.size) sortedTimings[p95Index] else max
        val p99 = if (p99Index < sortedTimings.size) sortedTimings[p99Index] else max
        
        // Calculate throughput (operations per second)
        val oldestSnapshot = memorySnapshots.firstOrNull()
        val latestSnapshot = memorySnapshots.lastOrNull()
        val timeSpanSeconds = if (oldestSnapshot != null && latestSnapshot != null) {
            (latestSnapshot.timestamp.toEpochMilli() - oldestSnapshot.timestamp.toEpochMilli()) / 1000.0
        } else 1.0
        
        val throughput = count / maxOf(timeSpanSeconds, 1.0)
        val errorRate = if (count > 0) errors.toDouble() / count else 0.0
        
        return OperationStats(
            operationName = operationName,
            count = count,
            errors = errors,
            averageTimeNanos = average,
            minTimeNanos = min,
            maxTimeNanos = max,
            percentile95Nanos = p95,
            percentile99Nanos = p99,
            throughputPerSecond = throughput,
            errorRate = errorRate
        )
    }
    
    fun getAllOperationStats(): Map<String, OperationStats> {
        return operationTimings.keys.associateWith { operationName ->
            getOperationStats(operationName)!!
        }
    }
    
    fun getPerformanceReport(): Map<String, Any> {
        val allStats = getAllOperationStats()
        
        // Find problematic operations
        val slowOperations = allStats.filter { (_, stats) ->
            stats.averageTimeNanos > 100_000_000 // 100ms
        }
        
        val errorProneOperations = allStats.filter { (_, stats) ->
            stats.errorRate > 0.01 // 1% error rate
        }
        
        val lowThroughputOperations = allStats.filter { (_, stats) ->
            stats.throughputPerSecond < 10 // Less than 10 ops/sec
        }
        
        // Memory analysis
        val memoryAnalysis = analyzeMemoryUsage()
        
        return mapOf(
            "timestamp" to Instant.now().toString(),
            "operationStats" to allStats,
            "alerts" to mapOf(
                "slowOperations" to slowOperations.keys,
                "errorProneOperations" to errorProneOperations.keys,
                "lowThroughputOperations" to lowThroughputOperations.keys
            ),
            "memoryAnalysis" to memoryAnalysis,
            "recommendations" to generateRecommendations(allStats)
        )
    }
    
    fun runBenchmark(
        operationName: String,
        iterations: Int,
        operation: () -> Unit
    ): Map<String, Any> {
        println("🏃 Running benchmark: $operationName with $iterations iterations")
        
        // Warmup
        repeat(minOf(100, iterations / 10)) {
            operation()
        }
        
        // Clear previous stats for clean benchmark
        clearStats(operationName)
        
        // Run benchmark
        val startTime = System.currentTimeMillis()
        repeat(iterations) {
            profileOperation(operationName, operation)
        }
        val endTime = System.currentTimeMillis()
        
        val stats = getOperationStats(operationName)!!
        val totalTimeMs = endTime - startTime
        
        return mapOf(
            "operationName" to operationName,
            "iterations" to iterations,
            "totalTimeMs" to totalTimeMs,
            "stats" to stats,
            "verdict" to generateBenchmarkVerdict(stats)
        )
    }
    
    fun compareOperations(operationNames: List<String>): Map<String, Any> {
        val comparisons = operationNames.mapNotNull { name ->
            getOperationStats(name)?.let { name to it }
        }.toMap()
        
        if (comparisons.isEmpty()) {
            return mapOf("error" to "No valid operations to compare")
        }
        
        val fastest = comparisons.minByOrNull { it.value.averageTimeNanos }
        val mostReliable = comparisons.minByOrNull { it.value.errorRate }
        val highestThroughput = comparisons.maxByOrNull { it.value.throughputPerSecond }
        
        return mapOf(
            "operations" to comparisons,
            "fastest" to fastest?.key,
            "mostReliable" to mostReliable?.key,
            "highestThroughput" to highestThroughput?.key,
            "analysis" to generateComparisonAnalysis(comparisons)
        )
    }
    
    private fun analyzeMemoryUsage(): Map<String, Any> {
        val snapshots = memorySnapshots.toList()
        if (snapshots.isEmpty()) return emptyMap()
        
        val heapUsages = snapshots.map { it.heapUsed }
        val avgHeapUsage = heapUsages.average()
        val maxHeapUsage = heapUsages.maxOrNull() ?: 0
        val heapTrend = if (snapshots.size > 1) {
            val first = snapshots.first().heapUsed
            val last = snapshots.last().heapUsed
            when {
                last > first * 1.1 -> "INCREASING"
                last < first * 0.9 -> "DECREASING"
                else -> "STABLE"
            }
        } else "UNKNOWN"
        
        return mapOf(
            "averageHeapUsageMB" to avgHeapUsage / (1024 * 1024),
            "maxHeapUsageMB" to maxHeapUsage / (1024 * 1024),
            "heapTrend" to heapTrend,
            "memoryPressure" to if (maxHeapUsage > Runtime.getRuntime().maxMemory() * 0.8) "HIGH" else "NORMAL"
        )
    }
    
    private fun generateRecommendations(stats: Map<String, OperationStats>): List<String> {
        val recommendations = mutableListOf<String>()
        
        stats.forEach { (name, stat) ->
            when {
                stat.averageTimeNanos > 1_000_000_000 -> // 1 second
                    recommendations.add("Optimize $name - average time ${stat.averageTimeNanos / 1_000_000}ms is too high")
                
                stat.errorRate > 0.05 -> // 5%
                    recommendations.add("Investigate errors in $name - error rate ${(stat.errorRate * 100).toInt()}% is concerning")
                
                stat.throughputPerSecond < 1 ->
                    recommendations.add("Scale up $name - throughput of ${stat.throughputPerSecond} ops/sec is too low")
            }
        }
        
        if (recommendations.isEmpty()) {
            recommendations.add("All operations are performing within acceptable limits")
        }
        
        return recommendations
    }
    
    private fun generateBenchmarkVerdict(stats: OperationStats): String {
        return when {
            stats.averageTimeNanos < 1_000_000 -> "EXCELLENT" // < 1ms
            stats.averageTimeNanos < 10_000_000 -> "GOOD" // < 10ms
            stats.averageTimeNanos < 100_000_000 -> "ACCEPTABLE" // < 100ms
            else -> "NEEDS_OPTIMIZATION"
        }
    }
    
    private fun generateComparisonAnalysis(comparisons: Map<String, OperationStats>): List<String> {
        val analysis = mutableListOf<String>()
        
        val avgTimes = comparisons.mapValues { it.value.averageTimeNanos }
        val fastest = avgTimes.minByOrNull { it.value }
        val slowest = avgTimes.maxByOrNull { it.value }
        
        if (fastest != null && slowest != null) {
            val speedDifference = slowest.value / fastest.value
            analysis.add("${fastest.key} is ${speedDifference.toInt()}x faster than ${slowest.key}")
        }
        
        return analysis
    }
    
    private fun clearStats(operationName: String) {
        operationTimings[operationName]?.clear()
        operationCounts[operationName]?.reset()
        operationErrors[operationName]?.set(0)
    }
}