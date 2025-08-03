package com.learning.KafkaStarter.monitoring

import org.springframework.stereotype.Component
import java.time.Instant
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.atomic.AtomicLong

@Component
class KafkaErrorTracker {
    
    private val errorCounts = ConcurrentHashMap<String, AtomicLong>()
    private val recentErrors = ConcurrentLinkedQueue<ErrorInfo>()
    private val errorPatterns = ConcurrentHashMap<String, ErrorPattern>()
    
    data class ErrorInfo(
        val timestamp: Instant,
        val topic: String,
        val partition: Int,
        val offset: Long,
        val errorType: String,
        val message: String,
        val stackTrace: String? = null,
        val context: Map<String, Any> = emptyMap(),
        val severity: ErrorSeverity = ErrorSeverity.MEDIUM
    )
    
    data class ErrorPattern(
        val pattern: String,
        val count: AtomicLong,
        val firstOccurrence: Instant,
        val lastOccurrence: Instant,
        val description: String
    )
    
    enum class ErrorSeverity { LOW, MEDIUM, HIGH, CRITICAL }
    
    fun trackError(
        topic: String,
        partition: Int,
        offset: Long,
        exception: Exception,
        context: Map<String, Any> = emptyMap()
    ): String {
        val errorType = classifyError(exception)
        val errorId = generateErrorId()
        val severity = determineSeverity(exception, errorType)
        
        val errorInfo = ErrorInfo(
            timestamp = Instant.now(),
            topic = topic,
            partition = partition,
            offset = offset,
            errorType = errorType,
            message = exception.message ?: "Unknown error",
            stackTrace = exception.stackTraceToString(),
            context = context,
            severity = severity
        )
        
        // Update error counts
        errorCounts.computeIfAbsent(errorType) { AtomicLong(0) }.incrementAndGet()
        
        // Store recent error
        recentErrors.offer(errorInfo)
        while (recentErrors.size > 1000) { // Keep last 1000 errors
            recentErrors.poll()
        }
        
        // Detect patterns
        detectErrorPattern(errorInfo)
        
        // Log based on severity
        when (severity) {
            ErrorSeverity.CRITICAL -> {
                println("🚨 CRITICAL ERROR: $errorType in $topic-$partition at offset $offset")
                triggerCriticalAlert(errorInfo)
            }
            ErrorSeverity.HIGH -> {
                println("⚠️ HIGH SEVERITY: $errorType in $topic-$partition")
            }
            ErrorSeverity.MEDIUM -> {
                println("⚡ ERROR: $errorType in $topic-$partition")
            }
            ErrorSeverity.LOW -> {
                println("ℹ️ Minor error: $errorType")
            }
        }
        
        return errorId
    }
    
    fun getErrorSummary(): Map<String, Any> {
        val totalErrors = errorCounts.values.sumOf { it.get() }
        val errorsByType = errorCounts.mapValues { it.value.get() }
        val recentErrorsList = recentErrors.toList().takeLast(50)
        
        val severityDistribution = recentErrorsList.groupBy { it.severity }
            .mapValues { it.value.size }
        
        val topErrorTypes = errorsByType.toList()
            .sortedByDescending { it.second }
            .take(10)
        
        val errorTrends = analyzeErrorTrends()
        val patterns = errorPatterns.mapValues { (_, pattern) ->
            mapOf(
                "count" to pattern.count.get(),
                "firstOccurrence" to pattern.firstOccurrence.toString(),
                "lastOccurrence" to pattern.lastOccurrence.toString(),
                "description" to pattern.description
            )
        }
        
        return mapOf(
            "totalErrors" to totalErrors,
            "errorsByType" to errorsByType,
            "topErrorTypes" to topErrorTypes,
            "recentErrors" to recentErrorsList.map { formatErrorForDisplay(it) },
            "severityDistribution" to severityDistribution,
            "errorTrends" to errorTrends,
            "detectedPatterns" to patterns,
            "recommendations" to generateErrorRecommendations()
        )
    }
    
    fun getErrorsForTopic(topic: String): List<ErrorInfo> {
        return recentErrors.filter { it.topic == topic }
    }
    
    fun getErrorsByType(errorType: String): List<ErrorInfo> {
        return recentErrors.filter { it.errorType == errorType }
    }
    
    fun getErrorsByTimeRange(startTime: Instant, endTime: Instant): List<ErrorInfo> {
        return recentErrors.filter { 
            it.timestamp.isAfter(startTime) && it.timestamp.isBefore(endTime) 
        }
    }
    
    fun analyzeErrorSpike(windowMinutes: Int = 5): Map<String, Any> {
        val cutoffTime = Instant.now().minusSeconds(windowMinutes * 60L)
        val recentErrorsInWindow = recentErrors.filter { it.timestamp.isAfter(cutoffTime) }
        
        val errorRate = recentErrorsInWindow.size.toDouble() / windowMinutes
        val baseline = calculateBaselineErrorRate()
        
        val isSpike = errorRate > baseline * 3 // 3x increase is considered a spike
        
        return mapOf(
            "isSpike" to isSpike,
            "currentRate" to errorRate,
            "baselineRate" to baseline,
            "spikeMultiplier" to if (baseline > 0) errorRate / baseline else 0.0,
            "windowMinutes" to windowMinutes,
            "errorsInWindow" to recentErrorsInWindow.size,
            "topErrorTypesInSpike" to recentErrorsInWindow.groupBy { it.errorType }
                .mapValues { it.value.size }
                .toList()
                .sortedByDescending { it.second }
                .take(5)
        )
    }
    
    fun generateErrorReport(timeRangeHours: Int = 24): Map<String, Any> {
        val startTime = Instant.now().minusSeconds(timeRangeHours * 3600L)
        val errorsInRange = getErrorsByTimeRange(startTime, Instant.now())
        
        val hourlyBreakdown = errorsInRange.groupBy { 
            it.timestamp.epochSecond / 3600 // Group by hour
        }.mapValues { it.value.size }
        
        val topicBreakdown = errorsInRange.groupBy { it.topic }
            .mapValues { it.value.size }
        
        val mostProblematicPartitions = errorsInRange.groupBy { "${it.topic}-${it.partition}" }
            .mapValues { it.value.size }
            .toList()
            .sortedByDescending { it.second }
            .take(10)
        
        return mapOf(
            "timeRange" to "${timeRangeHours}h",
            "totalErrors" to errorsInRange.size,
            "hourlyBreakdown" to hourlyBreakdown,
            "topicBreakdown" to topicBreakdown,
            "mostProblematicPartitions" to mostProblematicPartitions,
            "recommendations" to generateSpecificRecommendations(errorsInRange)
        )
    }
    
    private fun classifyError(exception: Exception): String {
        return when (exception) {
            is org.apache.kafka.common.errors.SerializationException -> "SERIALIZATION_ERROR"
            is org.apache.kafka.common.errors.TimeoutException -> "TIMEOUT_ERROR"
            is org.apache.kafka.common.errors.AuthenticationException -> "AUTHENTICATION_ERROR"
            is org.apache.kafka.common.errors.AuthorizationException -> "AUTHORIZATION_ERROR"
            is org.apache.kafka.common.errors.RetriableException -> "RETRIABLE_ERROR"
            is org.apache.kafka.common.errors.NetworkException -> "NETWORK_ERROR"
            is IllegalArgumentException -> "VALIDATION_ERROR"
            is NullPointerException -> "NULL_POINTER_ERROR"
            is RuntimeException -> "RUNTIME_ERROR"
            else -> "UNKNOWN_ERROR"
        }
    }
    
    private fun determineSeverity(exception: Exception, errorType: String): ErrorSeverity {
        return when {
            exception is org.apache.kafka.common.errors.AuthenticationException ||
            exception is org.apache.kafka.common.errors.AuthorizationException -> ErrorSeverity.CRITICAL
            
            errorType == "SERIALIZATION_ERROR" ||
            errorType == "NULL_POINTER_ERROR" -> ErrorSeverity.HIGH
            
            errorType == "TIMEOUT_ERROR" ||
            errorType == "NETWORK_ERROR" -> ErrorSeverity.MEDIUM
            
            else -> ErrorSeverity.LOW
        }
    }
    
    private fun detectErrorPattern(errorInfo: ErrorInfo) {
        val pattern = identifyPattern(errorInfo)
        
        errorPatterns.compute(pattern) { _, existing ->
            if (existing == null) {
                ErrorPattern(
                    pattern = pattern,
                    count = AtomicLong(1),
                    firstOccurrence = errorInfo.timestamp,
                    lastOccurrence = errorInfo.timestamp,
                    description = generatePatternDescription(pattern)
                )
            } else {
                existing.count.incrementAndGet()
                existing.copy(lastOccurrence = errorInfo.timestamp)
            }
        }
    }
    
    private fun identifyPattern(errorInfo: ErrorInfo): String {
        return when {
            errorInfo.errorType == "SERIALIZATION_ERROR" -> "serialization_failures"
            errorInfo.errorType == "TIMEOUT_ERROR" -> "timeout_pattern"
            errorInfo.message.contains("connection", ignoreCase = true) -> "connection_issues"
            errorInfo.message.contains("authentication", ignoreCase = true) -> "auth_failures"
            else -> "general_errors"
        }
    }
    
    private fun generatePatternDescription(pattern: String): String {
        return when (pattern) {
            "serialization_failures" -> "Repeated serialization errors indicating schema or data format issues"
            "timeout_pattern" -> "Frequent timeouts suggesting network or performance problems"
            "connection_issues" -> "Connection-related errors indicating network instability"
            "auth_failures" -> "Authentication/authorization failures indicating security configuration issues"
            else -> "General error pattern requiring investigation"
        }
    }
    
    private fun analyzeErrorTrends(): Map<String, Any> {
        val last24Hours = recentErrors.filter { 
            it.timestamp.isAfter(Instant.now().minusSeconds(24 * 3600))
        }
        
        val last1Hour = recentErrors.filter { 
            it.timestamp.isAfter(Instant.now().minusSeconds(3600))
        }
        
        val trend = when {
            last1Hour.size > last24Hours.size / 24 * 2 -> "INCREASING"
            last1Hour.size < last24Hours.size / 24 / 2 -> "DECREASING"
            else -> "STABLE"
        }
        
        return mapOf(
            "trend" to trend,
            "last24Hours" to last24Hours.size,
            "lastHour" to last1Hour.size,
            "projectedDaily" to last1Hour.size * 24
        )
    }
    
    private fun calculateBaselineErrorRate(): Double {
        val last7Days = recentErrors.filter { 
            it.timestamp.isAfter(Instant.now().minusSeconds(7 * 24 * 3600))
        }
        
        return last7Days.size.toDouble() / (7 * 24 * 60) // errors per minute
    }
    
    private fun generateErrorRecommendations(): List<String> {
        val recommendations = mutableListOf<String>()
        
        val totalErrors = errorCounts.values.sumOf { it.get() }
        if (totalErrors > 1000) {
            recommendations.add("High error count detected - consider implementing circuit breakers")
        }
        
        val serializationErrors = errorCounts["SERIALIZATION_ERROR"]?.get() ?: 0
        if (serializationErrors > totalErrors * 0.1) {
            recommendations.add("High serialization error rate - check schema compatibility")
        }
        
        val timeoutErrors = errorCounts["TIMEOUT_ERROR"]?.get() ?: 0
        if (timeoutErrors > totalErrors * 0.2) {
            recommendations.add("Frequent timeouts - consider increasing timeout values or optimizing network")
        }
        
        if (recommendations.isEmpty()) {
            recommendations.add("Error levels are within acceptable limits")
        }
        
        return recommendations
    }
    
    private fun generateSpecificRecommendations(errors: List<ErrorInfo>): List<String> {
        val recommendations = mutableListOf<String>()
        
        val topicWithMostErrors = errors.groupBy { it.topic }.maxByOrNull { it.value.size }?.key
        if (topicWithMostErrors != null) {
            recommendations.add("Focus investigation on topic: $topicWithMostErrors")
        }
        
        val criticalErrors = errors.filter { it.severity == ErrorSeverity.CRITICAL }
        if (criticalErrors.isNotEmpty()) {
            recommendations.add("Address ${criticalErrors.size} critical errors immediately")
        }
        
        return recommendations
    }
    
    private fun formatErrorForDisplay(error: ErrorInfo): Map<String, Any> {
        return mapOf(
            "timestamp" to error.timestamp.toString(),
            "topic" to error.topic,
            "partition" to error.partition,
            "offset" to error.offset,
            "type" to error.errorType,
            "message" to error.message.take(100), // Truncate long messages
            "severity" to error.severity.name
        )
    }
    
    private fun triggerCriticalAlert(errorInfo: ErrorInfo) {
        // In a real implementation, this would send alerts to monitoring systems
        println("🚨 CRITICAL ALERT: ${errorInfo.errorType} - ${errorInfo.message}")
    }
    
    private fun generateErrorId(): String {
        return "err-${System.currentTimeMillis()}-${(1000..9999).random()}"
    }
}