package com.learning.KafkaStarter.streams

import com.learning.KafkaStarter.model.WebEvent
import com.learning.KafkaStarter.model.TransactionEvent
import com.learning.KafkaStarter.model.DashboardMetric
import com.learning.KafkaStarter.model.Alert
import org.apache.kafka.common.serialization.Serdes
import org.apache.kafka.streams.KeyValue
import org.apache.kafka.streams.StreamsBuilder
import org.apache.kafka.streams.Topology
import org.apache.kafka.streams.kstream.*
import org.springframework.kafka.support.serializer.JsonSerde
import org.springframework.stereotype.Component
import java.time.Duration
import java.time.Instant
import java.util.*

@Component
class DashboardStreamProcessor {
    
    fun buildDashboardTopology(): Topology {
        val builder = StreamsBuilder()
        
        // Create source streams for web events and transactions
        val webEvents: KStream<String, WebEvent> = builder.stream(
            "web-events",
            Consumed.with(Serdes.String(), JsonSerde(WebEvent::class.java))
        )
        
        val transactionEvents: KStream<String, TransactionEvent> = builder.stream(
            "transaction-events",
            Consumed.with(Serdes.String(), JsonSerde(TransactionEvent::class.java))
        )
        
        // Build real-time user activity analytics
        val userActivityMetrics = buildUserActivityAnalytics(webEvents)
        
        // Build real-time revenue analytics
        val revenueMetrics = buildRevenueAnalytics(transactionEvents)
        
        // Build alerting system
        val alerts = buildAlertingSystem(webEvents, transactionEvents)
        
        // Output metrics and alerts to dashboard topics
        userActivityMetrics.to("dashboard-user-metrics")
        revenueMetrics.to("dashboard-revenue-metrics")
        alerts.to("dashboard-alerts")
        
        return builder.build()
    }
    
    fun buildUserActivityAnalytics(webEvents: KStream<String, WebEvent>): KStream<String, DashboardMetric> {
        // Real-time user activity analytics with 1-minute windows
        return webEvents
            .selectKey { _, event -> "user-activity" }
            .groupByKey()
            .windowedBy(TimeWindows.of(Duration.ofMinutes(1)))
            .aggregate(
                { UserActivityAggregate() },
                { key, event, aggregate ->
                    aggregate.copy(
                        uniqueUsers = aggregate.uniqueUsers + event.userId,
                        totalEvents = aggregate.totalEvents + 1,
                        pageViews = aggregate.pageViews + if (event.action == "PAGE_VIEW") 1 else 0,
                        clicks = aggregate.clicks + if (event.action == "CLICK") 1 else 0,
                        sessions = aggregate.sessions + event.sessionId,
                        lastUpdate = event.timestamp
                    )
                },
                Named.`as`("user-activity-aggregates"),
                Materialized.`as`<String, UserActivityAggregate, WindowStore<org.apache.kafka.common.utils.Bytes, ByteArray>>("user-activity-store")
                    .withKeySerde(Serdes.String())
                    .withValueSerde(JsonSerde(UserActivityAggregate::class.java))
                    .withRetention(Duration.ofHours(24))
            )
            .toStream()
            .map { windowedKey, aggregate ->
                KeyValue(
                    "user-activity-${windowedKey.window().start()}",
                    DashboardMetric(
                        metricType = "USER_ACTIVITY",
                        value = aggregate.uniqueUsers.size.toDouble(),
                        timestamp = aggregate.lastUpdate,
                        breakdown = mapOf(
                            "uniqueUsers" to aggregate.uniqueUsers.size,
                            "totalEvents" to aggregate.totalEvents,
                            "pageViews" to aggregate.pageViews,
                            "clicks" to aggregate.clicks,
                            "sessions" to aggregate.sessions.size,
                            "windowStart" to windowedKey.window().start(),
                            "windowEnd" to windowedKey.window().end()
                        )
                    )
                )
            }
    }
    
    fun buildRevenueAnalytics(transactionEvents: KStream<String, TransactionEvent>): KStream<String, DashboardMetric> {
        // Real-time revenue analytics with 5-minute windows
        return transactionEvents
            .filter { _, transaction -> transaction.type == "COMPLETED" }
            .selectKey { _, transaction -> "revenue" }
            .groupByKey()
            .windowedBy(TimeWindows.of(Duration.ofMinutes(5)))
            .aggregate(
                { RevenueAggregate() },
                { key, transaction, aggregate ->
                    aggregate.copy(
                        totalRevenue = aggregate.totalRevenue + transaction.amount,
                        transactionCount = aggregate.transactionCount + 1,
                        averageTransactionValue = (aggregate.totalRevenue + transaction.amount) / (aggregate.transactionCount + 1),
                        maxTransactionValue = maxOf(aggregate.maxTransactionValue, transaction.amount),
                        currencyBreakdown = updateCurrencyBreakdown(aggregate.currencyBreakdown, transaction.currency, transaction.amount),
                        lastUpdate = transaction.timestamp
                    )
                },
                Named.`as`("revenue-aggregates"),
                Materialized.`as`<String, RevenueAggregate, WindowStore<org.apache.kafka.common.utils.Bytes, ByteArray>>("revenue-store")
                    .withRetention(Duration.ofHours(24))
            )
            .toStream()
            .map { windowedKey, aggregate ->
                KeyValue(
                    "revenue-${windowedKey.window().start()}",
                    DashboardMetric(
                        metricType = "REVENUE",
                        value = aggregate.totalRevenue,
                        timestamp = aggregate.lastUpdate,
                        breakdown = mapOf(
                            "totalRevenue" to aggregate.totalRevenue,
                            "transactionCount" to aggregate.transactionCount,
                            "averageValue" to aggregate.averageTransactionValue,
                            "maxValue" to aggregate.maxTransactionValue,
                            "currencies" to aggregate.currencyBreakdown,
                            "windowStart" to windowedKey.window().start(),
                            "windowEnd" to windowedKey.window().end()
                        )
                    )
                )
            }
    }
    
    fun buildAlertingSystem(
        webEvents: KStream<String, WebEvent>,
        transactionEvents: KStream<String, TransactionEvent>
    ): KStream<String, Alert> {
        
        // Low activity alert
        val lowActivityAlerts = webEvents
            .selectKey { _, event -> "activity-monitor" }
            .groupByKey()
            .windowedBy(TimeWindows.of(Duration.ofMinutes(5)))
            .count(Named.`as`("activity-count"))
            .toStream()
            .filter { windowedKey, count -> count < 10 } // Less than 10 events in 5 minutes
            .map { windowedKey, count ->
                KeyValue(
                    "low-activity-${windowedKey.window().start()}",
                    Alert(
                        alertId = UUID.randomUUID().toString(),
                        alertType = "LOW_ACTIVITY",
                        severity = "MEDIUM",
                        message = "Low user activity detected: only $count events in 5 minutes",
                        timestamp = System.currentTimeMillis(),
                        data = mapOf(
                            "eventCount" to count,
                            "threshold" to 10,
                            "windowStart" to windowedKey.window().start(),
                            "windowEnd" to windowedKey.window().end()
                        )
                    )
                )
            }
        
        // High value transaction alert
        val highValueAlerts = transactionEvents
            .filter { _, transaction -> transaction.amount > 10000.0 } // Transactions over $10,000
            .map { key, transaction ->
                KeyValue(
                    "high-value-${transaction.transactionId}",
                    Alert(
                        alertId = UUID.randomUUID().toString(),
                        alertType = "HIGH_VALUE_TRANSACTION",
                        severity = "HIGH",
                        message = "High value transaction detected: $${transaction.amount}",
                        timestamp = transaction.timestamp,
                        data = mapOf(
                            "transactionId" to transaction.transactionId,
                            "userId" to transaction.userId,
                            "amount" to transaction.amount,
                            "currency" to transaction.currency
                        )
                    )
                )
            }
        
        // Failed transaction alert
        val failedTransactionAlerts = transactionEvents
            .filter { _, transaction -> transaction.type == "FAILED" }
            .selectKey { _, transaction -> transaction.userId }
            .groupByKey()
            .windowedBy(TimeWindows.of(Duration.ofMinutes(5)))
            .count()
            .toStream()
            .filter { windowedKey, count -> count >= 3 } // 3 or more failures in 5 minutes
            .map { windowedKey, count ->
                KeyValue(
                    "failed-transactions-${windowedKey.key()}-${windowedKey.window().start()}",
                    Alert(
                        alertId = UUID.randomUUID().toString(),
                        alertType = "MULTIPLE_FAILED_TRANSACTIONS",
                        severity = "HIGH",
                        message = "Multiple failed transactions for user ${windowedKey.key()}: $count failures",
                        timestamp = System.currentTimeMillis(),
                        data = mapOf(
                            "userId" to windowedKey.key(),
                            "failureCount" to count,
                            "timeWindow" to "5 minutes"
                        )
                    )
                )
            }
        
        // Merge all alert streams
        return lowActivityAlerts
            .merge(highValueAlerts)
            .merge(failedTransactionAlerts)
    }
    
    private fun updateCurrencyBreakdown(current: Map<String, Double>, currency: String, amount: Double): Map<String, Double> {
        return current.toMutableMap().apply {
            this[currency] = (this[currency] ?: 0.0) + amount
        }
    }
}

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