package com.learning.KafkaStarter.service

import com.learning.KafkaStarter.model.WebhookEvent
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.*
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.messaging.handler.annotation.Payload
import org.springframework.stereotype.Service
import org.springframework.web.client.RestTemplate
import org.springframework.web.client.HttpClientErrorException
import org.springframework.web.client.HttpServerErrorException
import org.springframework.web.client.ResourceAccessException
import java.time.Duration
import java.util.concurrent.atomic.AtomicLong

@Service
class WebhookService {
    
    private val restTemplate = RestTemplate()
    private val kafkaTemplate: KafkaTemplate<String, Any> by lazy { 
        // In a real application, inject this properly
        throw NotImplementedError("KafkaTemplate injection needed")
    }
    
    @Value("\${hybrid.webhook.timeout:15000}")
    private var webhookTimeout: Long = 15000
    
    @Value("\${hybrid.webhook.retries:2}")
    private var maxRetries: Int = 2
    
    @Value("\${hybrid.webhook.retry-delay:2000}")
    private var retryDelay: Long = 2000
    
    private val logger = org.slf4j.LoggerFactory.getLogger(WebhookService::class.java)
    
    // Processing statistics
    private val webhooksProcessed = AtomicLong(0)
    private val successfulWebhooks = AtomicLong(0)
    private val failedWebhooks = AtomicLong(0)
    private val retriedWebhooks = AtomicLong(0)
    
    @KafkaListener(topics = ["webhook-events"])
    fun processWebhookEvent(
        @Payload webhookEvent: WebhookEvent
    ) {
        webhooksProcessed.incrementAndGet()
        
        try {
            logger.info("Processing webhook event: webhookId=${webhookEvent.webhookId}, " +
                "eventType=${webhookEvent.eventType}, targetUrl=${webhookEvent.targetUrl}")
            
            // Validate webhook before sending
            if (!validateWebhookEvent(webhookEvent)) {
                logger.warn("Invalid webhook event: ${webhookEvent.webhookId}")
                failedWebhooks.incrementAndGet()
                return
            }
            
            // Send webhook to external service
            val success = sendWebhook(webhookEvent)
            
            if (success) {
                successfulWebhooks.incrementAndGet()
                logger.info("Webhook sent successfully: webhookId=${webhookEvent.webhookId}")
                
                // Send success notification
                notifyWebhookSuccess(webhookEvent)
                
            } else {
                failedWebhooks.incrementAndGet()
                logger.warn("Webhook failed: webhookId=${webhookEvent.webhookId}")
                
                // Handle failure
                handleWebhookFailure(webhookEvent, Exception("Webhook delivery failed"))
            }
            
        } catch (e: Exception) {
            failedWebhooks.incrementAndGet()
            logger.error("Webhook processing failed: webhookId=${webhookEvent.webhookId}", e)
            
            handleWebhookFailure(webhookEvent, e)
        }
    }
    
    fun sendWebhook(webhookEvent: WebhookEvent): Boolean {
        val startTime = System.currentTimeMillis()
        
        try {
            // Validate URL before sending
            if (!validateWebhookUrl(webhookEvent.targetUrl)) {
                logger.error("Invalid webhook URL: ${webhookEvent.targetUrl}")
                return false
            }
            
            // Prepare HTTP headers
            val headers = HttpHeaders().apply {
                contentType = MediaType.APPLICATION_JSON
                set("User-Agent", "Kafka-Webhook-Service/1.0")
                set("X-Webhook-ID", webhookEvent.webhookId)
                set("X-Event-Type", webhookEvent.eventType)
                set("X-Timestamp", webhookEvent.timestamp.toString())
                set("X-Retry-Count", webhookEvent.retryCount.toString())
                
                // Add signature for security (in production, use HMAC)
                set("X-Webhook-Signature", generateWebhookSignature(webhookEvent))
            }
            
            // Create payload
            val payload = mapOf(
                "webhookId" to webhookEvent.webhookId,
                "eventType" to webhookEvent.eventType,
                "timestamp" to webhookEvent.timestamp,
                "data" to webhookEvent.payload,
                "retryCount" to webhookEvent.retryCount
            )
            
            // Create HTTP entity
            val entity = HttpEntity(payload, headers)
            
            logger.debug("Sending webhook to ${webhookEvent.targetUrl}")
            
            // Make the HTTP POST request
            val response = restTemplate.exchange(
                webhookEvent.targetUrl,
                HttpMethod.POST,
                entity,
                String::class.java
            )
            
            val responseTime = System.currentTimeMillis() - startTime
            
            // Check if response indicates success
            val isSuccess = response.statusCode.is2xxSuccessful
            
            if (isSuccess) {
                logger.info("Webhook delivered successfully: webhookId=${webhookEvent.webhookId}, " +
                    "status=${response.statusCode.value()}, responseTime=${responseTime}ms")
                
                // Log webhook delivery
                logWebhookDelivery(webhookEvent, response.statusCode.value(), responseTime, true)
                
            } else {
                logger.warn("Webhook returned non-success status: webhookId=${webhookEvent.webhookId}, " +
                    "status=${response.statusCode.value()}")
                
                logWebhookDelivery(webhookEvent, response.statusCode.value(), responseTime, false)
            }
            
            return isSuccess
            
        } catch (e: HttpClientErrorException) {
            // 4xx errors - usually non-retryable
            val responseTime = System.currentTimeMillis() - startTime
            logger.warn("Webhook client error: webhookId=${webhookEvent.webhookId}, " +
                "status=${e.statusCode.value()}, response=${e.responseBodyAsString}")
            
            logWebhookDelivery(webhookEvent, e.statusCode.value(), responseTime, false)
            return false
            
        } catch (e: HttpServerErrorException) {
            // 5xx errors - potentially retryable
            val responseTime = System.currentTimeMillis() - startTime
            logger.error("Webhook server error: webhookId=${webhookEvent.webhookId}, " +
                "status=${e.statusCode.value()}, response=${e.responseBodyAsString}")
            
            logWebhookDelivery(webhookEvent, e.statusCode.value(), responseTime, false)
            
            // Retry for server errors if retry count allows
            if (webhookEvent.retryCount < maxRetries) {
                scheduleWebhookRetry(webhookEvent)
            }
            
            return false
            
        } catch (e: ResourceAccessException) {
            // Network/timeout errors - retryable
            val responseTime = System.currentTimeMillis() - startTime
            logger.error("Webhook network error: webhookId=${webhookEvent.webhookId}, error=${e.message}")
            
            logWebhookDelivery(webhookEvent, 0, responseTime, false)
            
            // Retry for network errors if retry count allows
            if (webhookEvent.retryCount < maxRetries) {
                scheduleWebhookRetry(webhookEvent)
            }
            
            return false
            
        } catch (e: Exception) {
            // Other errors
            val responseTime = System.currentTimeMillis() - startTime
            logger.error("Webhook unexpected error: webhookId=${webhookEvent.webhookId}", e)
            
            logWebhookDelivery(webhookEvent, 0, responseTime, false)
            return false
        }
    }
    
    fun handleWebhookFailure(webhookEvent: WebhookEvent, error: Exception) {
        logger.error("Handling webhook failure: webhookId=${webhookEvent.webhookId}", error)
        
        // Create failure event
        val failureEvent = mapOf(
            "webhookId" to webhookEvent.webhookId,
            "eventType" to webhookEvent.eventType,
            "targetUrl" to webhookEvent.targetUrl,
            "retryCount" to webhookEvent.retryCount,
            "error" to mapOf(
                "type" to error::class.simpleName,
                "message" to (error.message ?: "Unknown error")
            ),
            "timestamp" to System.currentTimeMillis()
        )
        
        // Send to failure topic for monitoring
        sendToKafka("webhook-failures", failureEvent)
        
        // If max retries exceeded, send to dead letter
        if (webhookEvent.retryCount >= maxRetries) {
            sendToKafka("webhook-dead-letter", failureEvent)
            logger.error("Webhook max retries exceeded: webhookId=${webhookEvent.webhookId}")
        }
    }
    
    fun validateWebhookUrl(url: String): Boolean {
        if (url.isBlank()) {
            logger.warn("Empty webhook URL")
            return false
        }
        
        return try {
            val uri = java.net.URI(url)
            
            // Validate scheme
            if (uri.scheme !in setOf("http", "https")) {
                logger.warn("Invalid webhook URL scheme: ${uri.scheme}")
                return false
            }
            
            // Validate host
            if (uri.host.isNullOrBlank()) {
                logger.warn("Invalid webhook URL host")
                return false
            }
            
            // Security check - block internal/private IPs in production
            if (isInternalUrl(uri.host)) {
                logger.warn("Webhook URL points to internal network: ${uri.host}")
                return false
            }
            
            true
            
        } catch (e: Exception) {
            logger.warn("Invalid webhook URL format: $url", e)
            false
        }
    }
    
    private fun validateWebhookEvent(webhookEvent: WebhookEvent): Boolean {
        // Validate required fields
        if (webhookEvent.webhookId.isBlank() || webhookEvent.eventType.isBlank()) {
            logger.warn("Missing required webhook fields: webhookId or eventType")
            return false
        }
        
        // Validate URL
        if (!validateWebhookUrl(webhookEvent.targetUrl)) {
            return false
        }
        
        // Validate event type
        val validEventTypes = setOf(
            "ORDER_CREATED", "ORDER_UPDATED", "ORDER_COMPLETED", "ORDER_CANCELLED",
            "PAYMENT_PROCESSED", "PAYMENT_FAILED", "PAYMENT_REFUNDED",
            "INVENTORY_UPDATED", "PRODUCT_CREATED", "PRODUCT_UPDATED",
            "USER_REGISTERED", "USER_UPDATED", "SYSTEM_ALERT"
        )
        
        if (webhookEvent.eventType !in validEventTypes) {
            logger.warn("Invalid webhook event type: ${webhookEvent.eventType}")
            return false
        }
        
        // Validate payload size (prevent abuse)
        val payloadSize = webhookEvent.payload.toString().length
        if (payloadSize > 10000) { // 10KB limit
            logger.warn("Webhook payload too large: ${payloadSize} bytes")
            return false
        }
        
        return true
    }
    
    private fun isInternalUrl(host: String): Boolean {
        // Block common internal/private IP ranges
        val internalPatterns = listOf(
            Regex("^127\\..*"),          // Loopback
            Regex("^10\\..*"),           // Private Class A
            Regex("^172\\.(1[6-9]|2[0-9]|3[0-1])\\..*"), // Private Class B
            Regex("^192\\.168\\..*"),    // Private Class C
            Regex("^169\\.254\\..*"),    // Link-local
            Regex("^::1$"),              // IPv6 loopback
            Regex("^fc[0-9a-f][0-9a-f]:.*"), // IPv6 private
        )
        
        return internalPatterns.any { it.matches(host) } || 
               host.equals("localhost", ignoreCase = true)
    }
    
    private fun generateWebhookSignature(webhookEvent: WebhookEvent): String {
        // In production, use HMAC-SHA256 with a secret key
        // For this example, we'll use a simple hash
        val payload = "${webhookEvent.webhookId}:${webhookEvent.eventType}:${webhookEvent.timestamp}"
        return payload.hashCode().toString()
    }
    
    private fun scheduleWebhookRetry(webhookEvent: WebhookEvent) {
        retriedWebhooks.incrementAndGet()
        
        val retryEvent = webhookEvent.copy(
            retryCount = webhookEvent.retryCount + 1,
            webhookId = "${webhookEvent.webhookId}-retry-${webhookEvent.retryCount + 1}"
        )
        
        logger.info("Scheduling webhook retry: originalId=${webhookEvent.webhookId}, " +
            "retryId=${retryEvent.webhookId}, retryCount=${retryEvent.retryCount}")
        
        // In a real implementation, use a scheduler or delayed topic
        Thread {
            Thread.sleep(retryDelay * (retryEvent.retryCount)) // Exponential backoff
            sendToKafka("webhook-events", retryEvent)
        }.start()
    }
    
    private fun notifyWebhookSuccess(webhookEvent: WebhookEvent) {
        val successEvent = mapOf(
            "webhookId" to webhookEvent.webhookId,
            "eventType" to webhookEvent.eventType,
            "targetUrl" to webhookEvent.targetUrl,
            "deliveredAt" to System.currentTimeMillis(),
            "retryCount" to webhookEvent.retryCount
        )
        
        sendToKafka("webhook-deliveries", successEvent)
    }
    
    private fun logWebhookDelivery(webhookEvent: WebhookEvent, statusCode: Int, responseTime: Long, success: Boolean) {
        val logEvent = mapOf(
            "webhookId" to webhookEvent.webhookId,
            "eventType" to webhookEvent.eventType,
            "targetUrl" to webhookEvent.targetUrl,
            "statusCode" to statusCode,
            "responseTime" to responseTime,
            "success" to success,
            "retryCount" to webhookEvent.retryCount,
            "timestamp" to System.currentTimeMillis()
        )
        
        sendToKafka("webhook-logs", logEvent)
    }
    
    private fun sendToKafka(topic: String, data: Any) {
        try {
            // In a real implementation, properly inject KafkaTemplate
            // kafkaTemplate.send(topic, data)
            logger.debug("Would send to Kafka topic '$topic': $data")
        } catch (e: Exception) {
            logger.error("Failed to send to Kafka topic '$topic'", e)
        }
    }
    
    // Statistics and utility methods
    fun getProcessingStatistics(): Map<String, Any> {
        return mapOf(
            "webhooksProcessed" to webhooksProcessed.get(),
            "successfulWebhooks" to successfulWebhooks.get(),
            "failedWebhooks" to failedWebhooks.get(),
            "retriedWebhooks" to retriedWebhooks.get(),
            "successRate" to if (webhooksProcessed.get() > 0) {
                (successfulWebhooks.get().toDouble() / webhooksProcessed.get()) * 100
            } else 0.0,
            "configuration" to mapOf(
                "timeout" to webhookTimeout,
                "maxRetries" to maxRetries,
                "retryDelay" to retryDelay
            )
        )
    }
    
    fun resetStatistics() {
        webhooksProcessed.set(0)
        successfulWebhooks.set(0)
        failedWebhooks.set(0)
        retriedWebhooks.set(0)
        logger.info("Webhook service statistics reset")
    }
}