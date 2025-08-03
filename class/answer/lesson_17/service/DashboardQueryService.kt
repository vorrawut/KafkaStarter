package com.learning.KafkaStarter.service

import com.learning.KafkaStarter.model.DashboardMetric
import com.learning.KafkaStarter.model.Alert
import org.apache.kafka.streams.KafkaStreams
import org.apache.kafka.streams.StoreQueryParameters
import org.apache.kafka.streams.errors.InvalidStateStoreException
import org.apache.kafka.streams.state.QueryableStoreTypes
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.time.Instant

@Service
class DashboardQueryService {
    
    @Autowired
    private lateinit var kafkaStreams: KafkaStreams
    
    fun getCurrentActiveUsers(): DashboardMetric? {
        return try {
            // Query state store for current active users
            val store = kafkaStreams.store(
                StoreQueryParameters.fromNameAndType(
                    "user-activity-store",
                    QueryableStoreTypes.windowStore<String, UserActivityAggregate>()
                )
            )
            
            val now = Instant.now()
            val windowStart = now.minusSeconds(60).toEpochMilli() // Last minute
            val windowEnd = now.toEpochMilli()
            
            store.fetch("user-activity", windowStart, windowEnd).use { iterator ->
                if (iterator.hasNext()) {
                    val entry = iterator.next()
                    val aggregate = entry.value
                    
                    DashboardMetric(
                        metricType = "ACTIVE_USERS",
                        value = aggregate.uniqueUsers.size.toDouble(),
                        timestamp = aggregate.lastUpdate,
                        breakdown = mapOf(
                            "uniqueUsers" to aggregate.uniqueUsers.size,
                            "totalEvents" to aggregate.totalEvents,
                            "pageViews" to aggregate.pageViews,
                            "clicks" to aggregate.clicks,
                            "sessions" to aggregate.sessions.size
                        )
                    )
                } else {
                    null
                }
            }
        } catch (e: InvalidStateStoreException) {
            // State store not available (rebalancing, etc.)
            null
        } catch (e: Exception) {
            throw RuntimeException("Failed to query active users", e)
        }
    }
    
    fun getCurrentRevenueMetrics(): DashboardMetric? {
        return try {
            // Query state store for current revenue metrics
            val store = kafkaStreams.store(
                StoreQueryParameters.fromNameAndType(
                    "revenue-store",
                    QueryableStoreTypes.windowStore<String, RevenueAggregate>()
                )
            )
            
            val now = Instant.now()
            val windowStart = now.minusSeconds(300).toEpochMilli() // Last 5 minutes
            val windowEnd = now.toEpochMilli()
            
            store.fetch("revenue", windowStart, windowEnd).use { iterator ->
                if (iterator.hasNext()) {
                    val entry = iterator.next()
                    val aggregate = entry.value
                    
                    DashboardMetric(
                        metricType = "REVENUE",
                        value = aggregate.totalRevenue,
                        timestamp = aggregate.lastUpdate,
                        breakdown = mapOf(
                            "totalRevenue" to aggregate.totalRevenue,
                            "transactionCount" to aggregate.transactionCount,
                            "averageValue" to aggregate.averageTransactionValue,
                            "maxValue" to aggregate.maxTransactionValue,
                            "currencies" to aggregate.currencyBreakdown
                        )
                    )
                } else {
                    null
                }
            }
        } catch (e: InvalidStateStoreException) {
            null
        } catch (e: Exception) {
            throw RuntimeException("Failed to query revenue metrics", e)
        }
    }
    
    fun getRecentAlerts(limit: Int = 10): List<Alert> {
        return try {
            // In a real implementation, alerts would be stored in a state store
            // For this example, we'll return mock recent alerts
            val mockAlerts = listOf(
                Alert(
                    alertId = "alert-1",
                    alertType = "LOW_ACTIVITY",
                    severity = "MEDIUM",
                    message = "Low user activity detected in the last 5 minutes",
                    timestamp = System.currentTimeMillis() - 300000, // 5 minutes ago
                    data = mapOf("threshold" to 10, "actual" to 5)
                ),
                Alert(
                    alertId = "alert-2",
                    alertType = "HIGH_VALUE_TRANSACTION",
                    severity = "HIGH",
                    message = "High value transaction detected: $15,000",
                    timestamp = System.currentTimeMillis() - 120000, // 2 minutes ago
                    data = mapOf("amount" to 15000.0, "currency" to "USD")
                )
            )
            
            mockAlerts.take(limit)
        } catch (e: Exception) {
            emptyList()
        }
    }
    
    fun getMetricsInTimeRange(startTime: Long, endTime: Long): List<DashboardMetric> {
        return try {
            val metrics = mutableListOf<DashboardMetric>()
            
            // Query user activity metrics
            try {
                val userActivityStore = kafkaStreams.store(
                    StoreQueryParameters.fromNameAndType(
                        "user-activity-store",
                        QueryableStoreTypes.windowStore<String, UserActivityAggregate>()
                    )
                )
                
                userActivityStore.fetch("user-activity", startTime, endTime).use { iterator ->
                    while (iterator.hasNext()) {
                        val entry = iterator.next()
                        val aggregate = entry.value
                        
                        metrics.add(
                            DashboardMetric(
                                metricType = "USER_ACTIVITY",
                                value = aggregate.uniqueUsers.size.toDouble(),
                                timestamp = entry.key,
                                breakdown = mapOf(
                                    "uniqueUsers" to aggregate.uniqueUsers.size,
                                    "totalEvents" to aggregate.totalEvents,
                                    "pageViews" to aggregate.pageViews,
                                    "clicks" to aggregate.clicks
                                )
                            )
                        )
                    }
                }
            } catch (e: Exception) {
                // Handle user activity store not available
            }
            
            // Query revenue metrics
            try {
                val revenueStore = kafkaStreams.store(
                    StoreQueryParameters.fromNameAndType(
                        "revenue-store",
                        QueryableStoreTypes.windowStore<String, RevenueAggregate>()
                    )
                )
                
                revenueStore.fetch("revenue", startTime, endTime).use { iterator ->
                    while (iterator.hasNext()) {
                        val entry = iterator.next()
                        val aggregate = entry.value
                        
                        metrics.add(
                            DashboardMetric(
                                metricType = "REVENUE",
                                value = aggregate.totalRevenue,
                                timestamp = entry.key,
                                breakdown = mapOf(
                                    "totalRevenue" to aggregate.totalRevenue,
                                    "transactionCount" to aggregate.transactionCount,
                                    "averageValue" to aggregate.averageTransactionValue
                                )
                            )
                        )
                    }
                }
            } catch (e: Exception) {
                // Handle revenue store not available
            }
            
            metrics.sortedBy { it.timestamp }
        } catch (e: Exception) {
            emptyList()
        }
    }
    
    fun getSystemHealth(): Map<String, Any> {
        return try {
            val streamState = kafkaStreams.state()
            val isRunning = streamState == KafkaStreams.State.RUNNING
            
            // Check state store availability
            val userActivityStoreAvailable = try {
                kafkaStreams.store(
                    StoreQueryParameters.fromNameAndType(
                        "user-activity-store",
                        QueryableStoreTypes.windowStore<String, UserActivityAggregate>()
                    )
                )
                true
            } catch (e: Exception) {
                false
            }
            
            val revenueStoreAvailable = try {
                kafkaStreams.store(
                    StoreQueryParameters.fromNameAndType(
                        "revenue-store",
                        QueryableStoreTypes.windowStore<String, RevenueAggregate>()
                    )
                )
                true
            } catch (e: Exception) {
                false
            }
            
            // Get stream threads info
            val threadsMetadata = try {
                kafkaStreams.localThreadsMetadata()
            } catch (e: Exception) {
                emptyList()
            }
            
            val currentTime = System.currentTimeMillis()
            val lastDataTime = getCurrentActiveUsers()?.timestamp ?: 0
            val dataFreshnessMinutes = (currentTime - lastDataTime) / 60000
            
            mapOf(
                "streamState" to streamState.toString(),
                "isRunning" to isRunning,
                "storeAvailability" to mapOf(
                    "userActivityStore" to userActivityStoreAvailable,
                    "revenueStore" to revenueStoreAvailable
                ),
                "threadsCount" to threadsMetadata.size,
                "activeTasksCount" to threadsMetadata.sumOf { it.activeTasks().size },
                "standbyTasksCount" to threadsMetadata.sumOf { it.standbyTasks().size },
                "dataFreshnessMinutes" to dataFreshnessMinutes,
                "overallHealth" to if (isRunning && userActivityStoreAvailable && revenueStoreAvailable) "HEALTHY" else "DEGRADED",
                "lastChecked" to currentTime
            )
        } catch (e: Exception) {
            mapOf(
                "overallHealth" to "ERROR",
                "error" to (e.message ?: "Unknown error"),
                "lastChecked" to System.currentTimeMillis()
            )
        }
    }
}

// Data classes for aggregates (should match the ones in the stream processor)
data class UserActivityAggregate(
    val uniqueUsers: Set<String> = emptySet(),
    val totalEvents: Long = 0,
    val pageViews: Long = 0,
    val clicks: Long = 0,
    val sessions: Set<String> = emptySet(),
    val lastUpdate: Long = 0
)

data class RevenueAggregate(
    val totalRevenue: Double = 0.0,
    val transactionCount: Long = 0,
    val averageTransactionValue: Double = 0.0,
    val maxTransactionValue: Double = 0.0,
    val currencyBreakdown: Map<String, Double> = emptyMap(),
    val lastUpdate: Long = 0
)