package com.learning.KafkaStarter.service

import com.learning.KafkaStarter.model.RawCustomerEvent
import com.learning.KafkaStarter.model.EnrichedCustomerEvent
import com.learning.KafkaStarter.model.TransformationMetadata
import org.springframework.stereotype.Service
import java.time.Instant

@Service
class MessageTransformationService {
    
    private val logger = org.slf4j.LoggerFactory.getLogger(MessageTransformationService::class.java)
    
    fun transformCustomerEvent(rawEvent: RawCustomerEvent): EnrichedCustomerEvent? {
        val startTime = System.currentTimeMillis()
        
        try {
            logger.debug("Transforming customer event: ${rawEvent.eventId}")
            
            // Validate event data before transformation
            if (!validateEventData(rawEvent)) {
                logger.warn("Event validation failed: ${rawEvent.eventId}")
                return null
            }
            
            // Enrich the raw event with customer data
            val enrichedData = enrichWithCustomerData(rawEvent)
            
            // Normalize event data
            val normalizedData = normalizeEventData(rawEvent.rawData)
            
            // Calculate customer segment
            val customerSegment = calculateCustomerSegment(rawEvent.customerId)
            
            // Create transformation metadata
            val processingTime = System.currentTimeMillis() - startTime
            val transformationMetadata = TransformationMetadata(
                transformationType = "CUSTOMER_ENRICHMENT",
                enrichmentSources = listOf("CUSTOMER_DB", "PREFERENCE_SERVICE", "HISTORY_SERVICE"),
                transformationRules = listOf(
                    "NORMALIZE_FIELDS",
                    "ENRICH_CUSTOMER_DATA", 
                    "CALCULATE_SEGMENT",
                    "ADD_METADATA"
                ),
                processingTimeMs = processingTime
            )
            
            // Create enriched event
            val enrichedEvent = EnrichedCustomerEvent(
                eventId = rawEvent.eventId,
                customerId = rawEvent.customerId,
                customerSegment = customerSegment,
                eventType = rawEvent.eventType,
                enrichedData = enrichedData + normalizedData,
                transformationMetadata = transformationMetadata,
                timestamp = rawEvent.timestamp
            )
            
            logger.debug("Event transformed successfully: ${rawEvent.eventId} -> ${enrichedEvent.eventId}")
            return enrichedEvent
            
        } catch (e: Exception) {
            logger.error("Error transforming customer event: ${rawEvent.eventId}", e)
            return null
        }
    }
    
    fun enrichWithCustomerData(rawEvent: RawCustomerEvent): Map<String, Any> {
        // Simulate enrichment with customer data from various sources
        val customerData = mutableMapOf<String, Any>()
        
        // Mock customer profile data
        customerData["customerTier"] = when {
            rawEvent.customerId.endsWith("1") || rawEvent.customerId.endsWith("2") -> "PREMIUM"
            rawEvent.customerId.endsWith("3") -> "VIP"
            else -> "STANDARD"
        }
        
        // Mock customer preferences
        customerData["preferences"] = mapOf(
            "emailMarketing" to (rawEvent.customerId.hashCode() % 2 == 0),
            "smsNotifications" to (rawEvent.customerId.hashCode() % 3 == 0),
            "language" to if (rawEvent.customerId.contains("intl")) "EN" else "EN-US"
        )
        
        // Mock customer history
        customerData["totalOrders"] = (rawEvent.customerId.hashCode() % 50) + 1
        customerData["averageOrderValue"] = ((rawEvent.customerId.hashCode() % 500) + 50).toDouble()
        customerData["lastOrderDate"] = System.currentTimeMillis() - (rawEvent.customerId.hashCode() % 86400000)
        
        // Mock geographic data
        customerData["region"] = when (rawEvent.source) {
            "web-app" -> "US-WEST"
            "mobile-app" -> "US-EAST"
            "api" -> "EU-CENTRAL"
            else -> "UNKNOWN"
        }
        
        // Mock engagement score
        customerData["engagementScore"] = (rawEvent.customerId.hashCode() % 100).let { 
            if (it < 0) -it else it 
        }
        
        logger.debug("Enriched customer data for ${rawEvent.customerId}: $customerData")
        return customerData
    }
    
    fun normalizeEventData(rawData: Map<String, Any>): Map<String, Any> {
        val normalizedData = mutableMapOf<String, Any>()
        
        // Normalize field names (convert to snake_case)
        rawData.forEach { (key, value) ->
            val normalizedKey = key.replace(Regex("([a-z])([A-Z])"), "$1_$2").lowercase()
            normalizedData[normalizedKey] = value
        }
        
        // Standardize common fields
        normalizedData["normalized_timestamp"] = System.currentTimeMillis()
        
        // Clean and validate data types
        normalizedData.forEach { (key, value) ->
            when {
                key.contains("amount") || key.contains("price") || key.contains("value") -> {
                    // Ensure numeric values
                    normalizedData[key] = when (value) {
                        is String -> value.toDoubleOrNull() ?: 0.0
                        is Number -> value.toDouble()
                        else -> 0.0
                    }
                }
                key.contains("date") || key.contains("time") -> {
                    // Ensure timestamp format
                    normalizedData[key] = when (value) {
                        is String -> value.toLongOrNull() ?: System.currentTimeMillis()
                        is Number -> value.toLong()
                        else -> System.currentTimeMillis()
                    }
                }
                key.contains("email") -> {
                    // Normalize email format
                    if (value is String) {
                        normalizedData[key] = value.lowercase().trim()
                    }
                }
            }
        }
        
        logger.debug("Normalized event data: original=${rawData.size} fields, normalized=${normalizedData.size} fields")
        return normalizedData
    }
    
    fun calculateCustomerSegment(customerId: String): String {
        // Business rules for customer segmentation
        return when {
            customerId.startsWith("VIP_") -> "VIP"
            customerId.startsWith("PREMIUM_") -> "PREMIUM"
            customerId.endsWith("_CORP") -> "CORPORATE"
            customerId.contains("_GOLD_") -> "GOLD"
            customerId.contains("_SILVER_") -> "SILVER"
            customerId.length > 15 -> "ENTERPRISE"
            customerId.matches(Regex(".*[0-9]{4,}.*")) -> "HIGH_VALUE"
            else -> "STANDARD"
        }
    }
    
    fun validateEventData(rawEvent: RawCustomerEvent): Boolean {
        // Validate required fields
        if (rawEvent.eventId.isBlank() || rawEvent.customerId.isBlank()) {
            logger.warn("Missing required fields: eventId or customerId")
            return false
        }
        
        // Validate event type
        val validEventTypes = setOf(
            "USER_REGISTRATION", "USER_LOGIN", "USER_LOGOUT", 
            "PURCHASE_COMPLETED", "PURCHASE_CANCELLED", "CART_ABANDONED",
            "PRODUCT_VIEWED", "PRODUCT_LIKED", "REVIEW_SUBMITTED",
            "SUPPORT_TICKET_CREATED", "NEWSLETTER_SUBSCRIBED"
        )
        
        if (rawEvent.eventType !in validEventTypes) {
            logger.warn("Invalid event type: ${rawEvent.eventType}")
            return false
        }
        
        // Validate timestamp (not too old, not in future)
        val now = System.currentTimeMillis()
        val maxAge = 24 * 60 * 60 * 1000L // 24 hours
        val maxFuture = 5 * 60 * 1000L // 5 minutes
        
        if (rawEvent.timestamp < (now - maxAge) || rawEvent.timestamp > (now + maxFuture)) {
            logger.warn("Invalid timestamp: ${rawEvent.timestamp}, current: $now")
            return false
        }
        
        // Validate source
        val validSources = setOf("web-app", "mobile-app", "api", "batch-import", "third-party")
        if (rawEvent.source !in validSources) {
            logger.warn("Invalid source: ${rawEvent.source}")
            return false
        }
        
        // Validate data quality
        if (rawEvent.rawData.isEmpty()) {
            logger.warn("Empty raw data")
            return false
        }
        
        logger.debug("Event validation passed: ${rawEvent.eventId}")
        return true
    }
    
    fun getTransformationRules(eventType: String): List<String> {
        return when (eventType) {
            "USER_REGISTRATION" -> listOf(
                "VALIDATE_EMAIL", "NORMALIZE_NAME", "SET_DEFAULT_PREFERENCES", 
                "CALCULATE_INITIAL_SEGMENT", "ADD_ONBOARDING_FLAGS"
            )
            "PURCHASE_COMPLETED" -> listOf(
                "CALCULATE_ORDER_VALUE", "UPDATE_CUSTOMER_TIER", "ADD_LOYALTY_POINTS",
                "ENRICH_PRODUCT_DATA", "CALCULATE_SHIPPING_INFO"
            )
            "PRODUCT_VIEWED" -> listOf(
                "TRACK_BROWSE_HISTORY", "UPDATE_PREFERENCES", "CALCULATE_INTEREST_SCORE"
            )
            else -> listOf(
                "BASIC_NORMALIZATION", "ADD_TIMESTAMP", "ENRICH_CUSTOMER_DATA"
            )
        }
    }
}