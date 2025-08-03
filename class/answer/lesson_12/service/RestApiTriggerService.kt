package com.learning.KafkaStarter.service

import com.learning.KafkaStarter.model.RestApiCall
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
class RestApiTriggerService {
    
    private val restTemplate = RestTemplate()
    private val kafkaTemplate: KafkaTemplate<String, Any> by lazy { 
        // In a real application, inject this properly
        throw NotImplementedError("KafkaTemplate injection needed")
    }
    
    @Value("\${hybrid.api.timeout:30000}")
    private var apiTimeout: Long = 30000
    
    @Value("\${hybrid.api.retries:3}")
    private var maxRetries: Int = 3
    
    @Value("\${hybrid.api.retry-delay:1000}")
    private var retryDelay: Long = 1000
    
    private val logger = org.slf4j.LoggerFactory.getLogger(RestApiTriggerService::class.java)
    
    // Processing statistics
    private val apiCallsTriggered = AtomicLong(0)
    private val successfulCalls = AtomicLong(0)
    private val failedCalls = AtomicLong(0)
    private val retriedCalls = AtomicLong(0)
    
    @KafkaListener(topics = ["api-triggers"])
    fun processApiTrigger(
        @Payload apiCall: RestApiCall
    ) {
        apiCallsTriggered.incrementAndGet()
        
        try {
            logger.info("Processing API trigger: callId=${apiCall.callId}, " +
                "method=${apiCall.method}, url=${apiCall.url}")
            
            // Validate API call before making request
            if (!isValidApiCall(apiCall)) {
                logger.warn("Invalid API call: ${apiCall.callId}")
                failedCalls.incrementAndGet()
                return
            }
            
            // Make REST API call
            val response = makeRestCall(apiCall)
            
            // Handle successful response
            handleApiResponse(response, apiCall)
            successfulCalls.incrementAndGet()
            
            logger.info("API call completed successfully: callId=${apiCall.callId}, " +
                "status=${response["status"]}")
            
        } catch (e: Exception) {
            failedCalls.incrementAndGet()
            logger.error("API trigger processing failed: callId=${apiCall.callId}", e)
            
            // Attempt retry for retryable errors
            if (isRetryableError(e)) {
                retryFailedCall(apiCall)
            } else {
                // Send to dead letter or error handling
                handleNonRetryableError(apiCall, e)
            }
        }
    }
    
    fun makeRestCall(apiCall: RestApiCall): Map<String, Any> {
        val startTime = System.currentTimeMillis()
        
        try {
            // Prepare HTTP headers
            val headers = HttpHeaders().apply {
                contentType = MediaType.APPLICATION_JSON
                set("User-Agent", "Kafka-Hybrid-Service/1.0")
                set("X-Request-ID", apiCall.callId)
                
                // Add custom headers from the API call
                apiCall.headers.forEach { (key, value) ->
                    set(key, value)
                }
            }
            
            // Create HTTP entity
            val entity = if (apiCall.body != null) {
                HttpEntity(apiCall.body, headers)
            } else {
                HttpEntity<String>(headers)
            }
            
            // Determine HTTP method
            val method = when (apiCall.method.uppercase()) {
                "GET" -> HttpMethod.GET
                "POST" -> HttpMethod.POST
                "PUT" -> HttpMethod.PUT
                "DELETE" -> HttpMethod.DELETE
                "PATCH" -> HttpMethod.PATCH
                else -> throw IllegalArgumentException("Unsupported HTTP method: ${apiCall.method}")
            }
            
            logger.debug("Making ${method} request to ${apiCall.url}")
            
            // Make the HTTP request
            val response = restTemplate.exchange(
                apiCall.url,
                method,
                entity,
                String::class.java
            )
            
            val responseTime = System.currentTimeMillis() - startTime
            
            return mapOf(
                "status" to response.statusCode.value(),
                "statusText" to response.statusCode.reasonPhrase,
                "headers" to response.headers.toSingleValueMap(),
                "body" to (response.body ?: ""),
                "responseTime" to responseTime,
                "success" to true
            )
            
        } catch (e: HttpClientErrorException) {
            // 4xx errors
            val responseTime = System.currentTimeMillis() - startTime
            logger.warn("Client error response: ${e.statusCode} - ${e.responseBodyAsString}")
            
            return mapOf(
                "status" to e.statusCode.value(),
                "statusText" to e.statusCode.reasonPhrase,
                "body" to e.responseBodyAsString,
                "responseTime" to responseTime,
                "success" to false,
                "error" to "CLIENT_ERROR"
            )
            
        } catch (e: HttpServerErrorException) {
            // 5xx errors
            val responseTime = System.currentTimeMillis() - startTime
            logger.error("Server error response: ${e.statusCode} - ${e.responseBodyAsString}")
            
            return mapOf(
                "status" to e.statusCode.value(),
                "statusText" to e.statusCode.reasonPhrase,
                "body" to e.responseBodyAsString,
                "responseTime" to responseTime,
                "success" to false,
                "error" to "SERVER_ERROR"
            )
            
        } catch (e: ResourceAccessException) {
            // Network/timeout errors
            val responseTime = System.currentTimeMillis() - startTime
            logger.error("Network error: ${e.message}")
            
            return mapOf(
                "status" to 0,
                "statusText" to "Network Error",
                "body" to "",
                "responseTime" to responseTime,
                "success" to false,
                "error" to "NETWORK_ERROR",
                "message" to (e.message ?: "Unknown network error")
            )
            
        } catch (e: Exception) {
            // Other errors
            val responseTime = System.currentTimeMillis() - startTime
            logger.error("Unexpected error during API call", e)
            
            return mapOf(
                "status" to 0,
                "statusText" to "Error",
                "body" to "",
                "responseTime" to responseTime,
                "success" to false,
                "error" to "UNKNOWN_ERROR",
                "message" to (e.message ?: "Unknown error")
            )
        }
    }
    
    fun handleApiResponse(response: Map<String, Any>, originalCall: RestApiCall) {
        try {
            // Log response details
            val status = response["status"] as Int
            val success = response["success"] as Boolean
            val responseTime = response["responseTime"] as Long
            
            logger.debug("API response received: callId=${originalCall.callId}, " +
                "status=$status, success=$success, responseTime=${responseTime}ms")
            
            // Create response event
            val responseEvent = mapOf(
                "callId" to originalCall.callId,
                "originalUrl" to originalCall.url,
                "originalMethod" to originalCall.method,
                "triggeredBy" to originalCall.triggeredBy,
                "response" to response,
                "timestamp" to System.currentTimeMillis()
            )
            
            // Send response to appropriate topic based on success/failure
            if (success && status in 200..299) {
                // Send successful response
                sendToKafka("api-responses-success", responseEvent)
                
                // Trigger follow-up actions if needed
                triggerFollowUpActions(originalCall, response)
                
            } else {
                // Send failed response
                sendToKafka("api-responses-failed", responseEvent)
                
                // Handle failure scenarios
                handleApiFailure(originalCall, response)
            }
            
        } catch (e: Exception) {
            logger.error("Error handling API response for call: ${originalCall.callId}", e)
        }
    }
    
    fun retryFailedCall(apiCall: RestApiCall) {
        retriedCalls.incrementAndGet()
        
        try {
            logger.info("Retrying failed API call: callId=${apiCall.callId}")
            
            // Add retry metadata
            val retryCall = apiCall.copy(
                callId = "${apiCall.callId}-retry-${System.currentTimeMillis()}",
                headers = apiCall.headers + mapOf(
                    "X-Retry-Original" to apiCall.callId,
                    "X-Retry-Attempt" to "1"
                )
            )
            
            // Schedule retry with delay
            Thread.sleep(retryDelay)
            
            // Send to retry topic or back to main topic
            sendToKafka("api-triggers-retry", retryCall)
            
        } catch (e: Exception) {
            logger.error("Error retrying API call: ${apiCall.callId}", e)
        }
    }
    
    private fun isValidApiCall(apiCall: RestApiCall): Boolean {
        // Validate URL
        if (!isValidUrl(apiCall.url)) {
            logger.warn("Invalid URL: ${apiCall.url}")
            return false
        }
        
        // Validate HTTP method
        val validMethods = setOf("GET", "POST", "PUT", "DELETE", "PATCH")
        if (apiCall.method.uppercase() !in validMethods) {
            logger.warn("Invalid HTTP method: ${apiCall.method}")
            return false
        }
        
        // Validate required fields
        if (apiCall.callId.isBlank() || apiCall.triggeredBy.isBlank()) {
            logger.warn("Missing required fields: callId or triggeredBy")
            return false
        }
        
        return true
    }
    
    private fun isValidUrl(url: String): Boolean {
        return try {
            val uri = java.net.URI(url)
            uri.scheme in setOf("http", "https") && uri.host != null
        } catch (e: Exception) {
            false
        }
    }
    
    private fun isRetryableError(error: Exception): Boolean {
        return when (error) {
            is ResourceAccessException -> true // Network timeouts
            is HttpServerErrorException -> error.statusCode.is5xxServerError // 5xx errors
            else -> false
        }
    }
    
    private fun handleNonRetryableError(apiCall: RestApiCall, error: Exception) {
        logger.error("Non-retryable error for API call: ${apiCall.callId}", error)
        
        val errorEvent = mapOf(
            "callId" to apiCall.callId,
            "originalUrl" to apiCall.url,
            "originalMethod" to apiCall.method,
            "triggeredBy" to apiCall.triggeredBy,
            "error" to mapOf(
                "type" to error::class.simpleName,
                "message" to (error.message ?: "Unknown error")
            ),
            "timestamp" to System.currentTimeMillis()
        )
        
        sendToKafka("api-call-errors", errorEvent)
    }
    
    private fun triggerFollowUpActions(apiCall: RestApiCall, response: Map<String, Any>) {
        // Example follow-up actions based on API response
        when {
            apiCall.url.contains("/inventory/") -> {
                // Inventory API follow-up
                triggerInventoryFollowUp(apiCall, response)
            }
            apiCall.url.contains("/catalog/") -> {
                // Catalog API follow-up
                triggerCatalogFollowUp(apiCall, response)
            }
            // Add more follow-up patterns as needed
        }
    }
    
    private fun triggerInventoryFollowUp(apiCall: RestApiCall, response: Map<String, Any>) {
        val followUpEvent = mapOf(
            "eventType" to "INVENTORY_API_COMPLETED",
            "originalCallId" to apiCall.callId,
            "triggeredBy" to apiCall.triggeredBy,
            "apiResponse" to response,
            "timestamp" to System.currentTimeMillis()
        )
        
        sendToKafka("inventory-api-events", followUpEvent)
    }
    
    private fun triggerCatalogFollowUp(apiCall: RestApiCall, response: Map<String, Any>) {
        val followUpEvent = mapOf(
            "eventType" to "CATALOG_API_COMPLETED",
            "originalCallId" to apiCall.callId,
            "triggeredBy" to apiCall.triggeredBy,
            "apiResponse" to response,
            "timestamp" to System.currentTimeMillis()
        )
        
        sendToKafka("catalog-api-events", followUpEvent)
    }
    
    private fun handleApiFailure(apiCall: RestApiCall, response: Map<String, Any>) {
        val failureEvent = mapOf(
            "eventType" to "API_CALL_FAILED",
            "callId" to apiCall.callId,
            "url" to apiCall.url,
            "method" to apiCall.method,
            "triggeredBy" to apiCall.triggeredBy,
            "response" to response,
            "timestamp" to System.currentTimeMillis()
        )
        
        sendToKafka("api-call-failures", failureEvent)
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
            "apiCallsTriggered" to apiCallsTriggered.get(),
            "successfulCalls" to successfulCalls.get(),
            "failedCalls" to failedCalls.get(),
            "retriedCalls" to retriedCalls.get(),
            "successRate" to if (apiCallsTriggered.get() > 0) {
                (successfulCalls.get().toDouble() / apiCallsTriggered.get()) * 100
            } else 0.0,
            "configuration" to mapOf(
                "timeout" to apiTimeout,
                "maxRetries" to maxRetries,
                "retryDelay" to retryDelay
            )
        )
    }
    
    fun resetStatistics() {
        apiCallsTriggered.set(0)
        successfulCalls.set(0)
        failedCalls.set(0)
        retriedCalls.set(0)
        logger.info("API trigger service statistics reset")
    }
}