package com.learning.KafkaStarter.controller

import com.learning.KafkaStarter.model.DashboardMetric
import com.learning.KafkaStarter.model.Alert
import com.learning.KafkaStarter.model.WebEvent
import com.learning.KafkaStarter.model.TransactionEvent
import com.learning.KafkaStarter.service.DashboardQueryService
import com.learning.KafkaStarter.websocket.DashboardWebSocketHandler
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.ResponseEntity
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.web.bind.annotation.*
import java.time.Instant
import java.util.*

@RestController
@RequestMapping("/api/dashboard")
class DashboardController {
    
    @Autowired
    private lateinit var dashboardQueryService: DashboardQueryService
    
    @Autowired
    private lateinit var kafkaTemplate: KafkaTemplate<String, Any>
    
    @Autowired
    private lateinit var webSocketHandler: DashboardWebSocketHandler
    
    @GetMapping("/metrics/active-users")
    fun getCurrentActiveUsers(): ResponseEntity<DashboardMetric> {
        return try {
            // Get current active users metric
            val metric = dashboardQueryService.getCurrentActiveUsers()
            
            if (metric != null) {
                ResponseEntity.ok(metric)
            } else {
                // Return default metric if no data available
                ResponseEntity.ok(
                    DashboardMetric(
                        metricType = "ACTIVE_USERS",
                        value = 0.0,
                        timestamp = System.currentTimeMillis(),
                        breakdown = mapOf(
                            "message" to "No recent activity data available",
                            "dataSource" to "state_store"
                        )
                    )
                )
            }
        } catch (e: Exception) {
            ResponseEntity.internalServerError().body(
                DashboardMetric(
                    metricType = "ACTIVE_USERS_ERROR",
                    value = -1.0,
                    timestamp = System.currentTimeMillis(),
                    breakdown = mapOf(
                        "error" to (e.message ?: "Unknown error"),
                        "errorType" to e::class.simpleName
                    )
                )
            )
        }
    }
    
    @GetMapping("/metrics/revenue")
    fun getCurrentRevenue(): ResponseEntity<DashboardMetric> {
        return try {
            // Get current revenue metrics
            val metric = dashboardQueryService.getCurrentRevenueMetrics()
            
            if (metric != null) {
                ResponseEntity.ok(metric)
            } else {
                // Return default metric if no data available
                ResponseEntity.ok(
                    DashboardMetric(
                        metricType = "REVENUE",
                        value = 0.0,
                        timestamp = System.currentTimeMillis(),
                        breakdown = mapOf(
                            "message" to "No recent revenue data available",
                            "dataSource" to "state_store"
                        )
                    )
                )
            }
        } catch (e: Exception) {
            ResponseEntity.internalServerError().body(
                DashboardMetric(
                    metricType = "REVENUE_ERROR",
                    value = -1.0,
                    timestamp = System.currentTimeMillis(),
                    breakdown = mapOf(
                        "error" to (e.message ?: "Unknown error"),
                        "errorType" to e::class.simpleName
                    )
                )
            )
        }
    }
    
    @GetMapping("/alerts")
    fun getRecentAlerts(@RequestParam(defaultValue = "10") limit: Int): ResponseEntity<List<Alert>> {
        return try {
            // Get recent alerts
            val alerts = dashboardQueryService.getRecentAlerts(limit)
            ResponseEntity.ok(alerts)
        } catch (e: Exception) {
            ResponseEntity.internalServerError().body(
                listOf(
                    Alert(
                        alertId = "error-alert",
                        alertType = "SYSTEM_ERROR",
                        severity = "HIGH",
                        message = "Failed to retrieve alerts: ${e.message}",
                        timestamp = System.currentTimeMillis(),
                        data = mapOf("errorType" to e::class.simpleName)
                    )
                )
            )
        }
    }
    
    @GetMapping("/metrics/history")
    fun getMetricsHistory(
        @RequestParam startTime: Long,
        @RequestParam endTime: Long
    ): ResponseEntity<List<DashboardMetric>> {
        return try {
            // Validate time range
            if (endTime <= startTime) {
                return ResponseEntity.badRequest().body(emptyList())
            }
            
            // Get metrics history in time range
            val metrics = dashboardQueryService.getMetricsInTimeRange(startTime, endTime)
            ResponseEntity.ok(metrics)
        } catch (e: Exception) {
            ResponseEntity.internalServerError().body(
                listOf(
                    DashboardMetric(
                        metricType = "HISTORY_ERROR",
                        value = -1.0,
                        timestamp = System.currentTimeMillis(),
                        breakdown = mapOf(
                            "error" to (e.message ?: "Unknown error"),
                            "requestedRange" to mapOf(
                                "startTime" to startTime,
                                "endTime" to endTime
                            )
                        )
                    )
                )
            )
        }
    }
    
    @GetMapping("/health")
    fun getSystemHealth(): ResponseEntity<Map<String, Any>> {
        return try {
            // Get dashboard system health
            val health = dashboardQueryService.getSystemHealth()
            ResponseEntity.ok(health)
        } catch (e: Exception) {
            ResponseEntity.internalServerError().body(
                mapOf(
                    "overallHealth" to "ERROR",
                    "error" to (e.message ?: "Unknown error"),
                    "errorType" to e::class.simpleName,
                    "lastChecked" to System.currentTimeMillis()
                )
            )
        }
    }
    
    @PostMapping("/events/generate-test-data")
    fun generateTestData(@RequestParam(defaultValue = "100") count: Int): ResponseEntity<Map<String, Any>> {
        return try {
            // Generate test events for dashboard testing
            val generatedEventIds = mutableListOf<String>()
            val eventTypes = listOf("PAGE_VIEW", "CLICK", "LOGIN", "LOGOUT")
            val userIds = (1..10).map { "user-$it" }
            
            // Generate web events
            repeat(count / 2) {
                val webEvent = WebEvent(
                    eventId = UUID.randomUUID().toString(),
                    userId = userIds.random(),
                    sessionId = UUID.randomUUID().toString(),
                    action = eventTypes.random(),
                    timestamp = System.currentTimeMillis(),
                    page = "/page-${(1..5).random()}",
                    userAgent = "TestAgent/1.0",
                    customData = mapOf(
                        "testData" to true,
                        "generatedAt" to System.currentTimeMillis()
                    )
                )
                
                kafkaTemplate.send("web-events", webEvent.eventId, webEvent)
                generatedEventIds.add(webEvent.eventId)
            }
            
            // Generate transaction events
            repeat(count / 2) {
                val transactionEvent = TransactionEvent(
                    transactionId = UUID.randomUUID().toString(),
                    userId = userIds.random(),
                    amount = (10.0..1000.0).random(),
                    currency = listOf("USD", "EUR", "GBP").random(),
                    type = listOf("COMPLETED", "FAILED", "PENDING").random(),
                    timestamp = System.currentTimeMillis(),
                    metadata = mapOf(
                        "testData" to true,
                        "generatedAt" to System.currentTimeMillis()
                    )
                )
                
                kafkaTemplate.send("transaction-events", transactionEvent.transactionId, transactionEvent)
                generatedEventIds.add(transactionEvent.transactionId)
            }
            
            ResponseEntity.ok(
                mapOf(
                    "success" to true,
                    "generatedEvents" to count,
                    "webEvents" to (count / 2),
                    "transactionEvents" to (count / 2),
                    "eventIds" to generatedEventIds,
                    "message" to "Test data generated successfully",
                    "timestamp" to System.currentTimeMillis()
                )
            )
        } catch (e: Exception) {
            ResponseEntity.internalServerError().body(
                mapOf(
                    "success" to false,
                    "error" to (e.message ?: "Unknown error"),
                    "errorType" to e::class.simpleName,
                    "requestedCount" to count,
                    "timestamp" to System.currentTimeMillis()
                )
            )
        }
    }
    
    @GetMapping("/websocket/info")
    fun getWebSocketInfo(): ResponseEntity<Map<String, Any>> {
        return try {
            val sessionInfo = webSocketHandler.getSessionInfo()
            ResponseEntity.ok(
                mapOf(
                    "websocketSessions" to sessionInfo,
                    "serverTime" to System.currentTimeMillis(),
                    "uptimeMinutes" to (System.currentTimeMillis() - startTime) / 60000
                )
            )
        } catch (e: Exception) {
            ResponseEntity.internalServerError().body(
                mapOf(
                    "error" to (e.message ?: "Unknown error"),
                    "timestamp" to System.currentTimeMillis()
                )
            )
        }
    }
    
    @PostMapping("/websocket/broadcast-test")
    fun broadcastTestMessage(@RequestBody message: Map<String, Any>): ResponseEntity<Map<String, Any>> {
        return try {
            // Broadcast test message to all WebSocket clients
            val testMetric = DashboardMetric(
                metricType = "TEST_METRIC",
                value = 42.0,
                timestamp = System.currentTimeMillis(),
                breakdown = message
            )
            
            webSocketHandler.broadcastMetricUpdate(testMetric)
            
            ResponseEntity.ok(
                mapOf(
                    "success" to true,
                    "message" to "Test message broadcasted",
                    "activeConnections" to webSocketHandler.getActiveSessionCount(),
                    "broadcastData" to testMetric
                )
            )
        } catch (e: Exception) {
            ResponseEntity.internalServerError().body(
                mapOf(
                    "success" to false,
                    "error" to (e.message ?: "Unknown error")
                )
            )
        }
    }
    
    companion object {
        private val startTime = System.currentTimeMillis()
    }
}