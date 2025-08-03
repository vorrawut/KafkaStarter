package com.learning.KafkaStarter.websocket

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.learning.KafkaStarter.model.DashboardMetric
import com.learning.KafkaStarter.model.Alert
import org.springframework.stereotype.Component
import org.springframework.web.socket.*
import java.util.concurrent.ConcurrentHashMap

@Component
class DashboardWebSocketHandler : TextWebSocketHandler() {
    
    private val sessions = ConcurrentHashMap<String, WebSocketSession>()
    private val objectMapper = jacksonObjectMapper()
    
    override fun afterConnectionEstablished(session: WebSocketSession) {
        // Store session and send initial dashboard data
        sessions[session.id] = session
        logger.info("Dashboard WebSocket connected: ${session.id}")
        
        try {
            // Send welcome message with initial data
            val welcomeMessage = mapOf(
                "type" to "WELCOME",
                "message" to "Connected to dashboard real-time updates",
                "sessionId" to session.id,
                "timestamp" to System.currentTimeMillis()
            )
            
            sendToSession(session, welcomeMessage)
            
            // Send initial dashboard state
            sendInitialDashboardData(session)
            
        } catch (e: Exception) {
            logger.error("Failed to send initial data to WebSocket session ${session.id}", e)
        }
    }
    
    override fun handleTextMessage(session: WebSocketSession, message: TextMessage) {
        try {
            // Parse dashboard requests and respond with data
            val request = objectMapper.readValue(message.payload, Map::class.java)
            val requestType = request["type"] as? String
            
            when (requestType) {
                "GET_CURRENT_METRICS" -> {
                    sendCurrentMetrics(session)
                }
                "GET_RECENT_ALERTS" -> {
                    sendRecentAlerts(session)
                }
                "SUBSCRIBE_UPDATES" -> {
                    val updateType = request["updateType"] as? String
                    subscribeToUpdates(session, updateType)
                }
                "PING" -> {
                    val pongMessage = mapOf(
                        "type" to "PONG",
                        "timestamp" to System.currentTimeMillis()
                    )
                    sendToSession(session, pongMessage)
                }
                else -> {
                    val errorMessage = mapOf(
                        "type" to "ERROR",
                        "message" to "Unknown request type: $requestType",
                        "timestamp" to System.currentTimeMillis()
                    )
                    sendToSession(session, errorMessage)
                }
            }
        } catch (e: Exception) {
            logger.error("Failed to handle WebSocket message from session ${session.id}", e)
            
            val errorMessage = mapOf(
                "type" to "ERROR",
                "message" to "Failed to process message: ${e.message}",
                "timestamp" to System.currentTimeMillis()
            )
            
            try {
                sendToSession(session, errorMessage)
            } catch (sendError: Exception) {
                logger.error("Failed to send error message to session ${session.id}", sendError)
            }
        }
    }
    
    override fun afterConnectionClosed(session: WebSocketSession, status: CloseStatus) {
        // Remove session from active sessions
        sessions.remove(session.id)
        logger.info("Dashboard WebSocket disconnected: ${session.id}, status: $status")
    }
    
    fun broadcastMetricUpdate(metric: DashboardMetric) {
        // Broadcast metric updates to all connected clients
        val updateMessage = mapOf(
            "type" to "METRIC_UPDATE",
            "metric" to metric,
            "timestamp" to System.currentTimeMillis()
        )
        
        broadcastToAllSessions(updateMessage)
    }
    
    fun broadcastAlert(alert: Alert) {
        // Broadcast alerts to all connected clients
        val alertMessage = mapOf(
            "type" to "ALERT",
            "alert" to alert,
            "timestamp" to System.currentTimeMillis()
        )
        
        broadcastToAllSessions(alertMessage)
    }
    
    private fun sendInitialDashboardData(session: WebSocketSession) {
        // Send current dashboard state
        val initialData = mapOf(
            "type" to "INITIAL_DATA",
            "data" to mapOf(
                "activeUsers" to 42,
                "currentRevenue" to 12450.75,
                "alertCount" to 3,
                "systemStatus" to "HEALTHY"
            ),
            "timestamp" to System.currentTimeMillis()
        )
        
        sendToSession(session, initialData)
    }
    
    private fun sendCurrentMetrics(session: WebSocketSession) {
        // Send current metrics to specific session
        val metricsData = mapOf(
            "type" to "CURRENT_METRICS",
            "metrics" to listOf(
                mapOf(
                    "metricType" to "ACTIVE_USERS",
                    "value" to 42,
                    "timestamp" to System.currentTimeMillis()
                ),
                mapOf(
                    "metricType" to "REVENUE",
                    "value" to 12450.75,
                    "timestamp" to System.currentTimeMillis()
                )
            ),
            "timestamp" to System.currentTimeMillis()
        )
        
        sendToSession(session, metricsData)
    }
    
    private fun sendRecentAlerts(session: WebSocketSession) {
        // Send recent alerts to specific session
        val alertsData = mapOf(
            "type" to "RECENT_ALERTS",
            "alerts" to listOf(
                mapOf(
                    "alertId" to "alert-1",
                    "alertType" to "LOW_ACTIVITY",
                    "severity" to "MEDIUM",
                    "message" to "Low user activity detected",
                    "timestamp" to System.currentTimeMillis() - 300000
                ),
                mapOf(
                    "alertId" to "alert-2",
                    "alertType" to "HIGH_VALUE_TRANSACTION",
                    "severity" to "HIGH",
                    "message" to "High value transaction detected",
                    "timestamp" to System.currentTimeMillis() - 120000
                )
            ),
            "timestamp" to System.currentTimeMillis()
        )
        
        sendToSession(session, alertsData)
    }
    
    private fun subscribeToUpdates(session: WebSocketSession, updateType: String?) {
        // Handle subscription to specific update types
        val subscriptionMessage = mapOf(
            "type" to "SUBSCRIPTION_CONFIRMED",
            "updateType" to (updateType ?: "ALL"),
            "message" to "Subscribed to ${updateType ?: "all"} updates",
            "timestamp" to System.currentTimeMillis()
        )
        
        sendToSession(session, subscriptionMessage)
    }
    
    private fun sendToSession(session: WebSocketSession, data: Any) {
        // Send data to specific WebSocket session
        if (session.isOpen) {
            try {
                val jsonMessage = objectMapper.writeValueAsString(data)
                session.sendMessage(TextMessage(jsonMessage))
            } catch (e: Exception) {
                logger.error("Failed to send message to WebSocket session ${session.id}", e)
            }
        }
    }
    
    private fun broadcastToAllSessions(data: Any) {
        // Broadcast data to all connected sessions
        val deadSessions = mutableListOf<String>()
        
        sessions.forEach { (sessionId, session) ->
            if (session.isOpen) {
                try {
                    sendToSession(session, data)
                } catch (e: Exception) {
                    logger.error("Failed to broadcast to session $sessionId", e)
                    deadSessions.add(sessionId)
                }
            } else {
                deadSessions.add(sessionId)
            }
        }
        
        // Clean up dead sessions
        deadSessions.forEach { sessionId ->
            sessions.remove(sessionId)
        }
    }
    
    // Utility method to get active session count
    fun getActiveSessionCount(): Int = sessions.size
    
    // Utility method to get session information
    fun getSessionInfo(): Map<String, Any> {
        return mapOf(
            "totalSessions" to sessions.size,
            "activeSessions" to sessions.values.count { it.isOpen },
            "sessionIds" to sessions.keys.toList()
        )
    }
    
    companion object {
        private val logger = org.slf4j.LoggerFactory.getLogger(DashboardWebSocketHandler::class.java)
    }
}