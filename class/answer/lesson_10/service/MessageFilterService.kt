package com.learning.KafkaStarter.service

import com.learning.KafkaStarter.model.RawCustomerEvent
import org.springframework.stereotype.Service
import java.util.concurrent.atomic.AtomicLong

@Service
class MessageFilterService {
    
    private val logger = org.slf4j.LoggerFactory.getLogger(MessageFilterService::class.java)
    
    // Filter statistics
    private val totalEventsProcessed = AtomicLong(0)
    private val eventsFiltered = AtomicLong(0)
    private val eventsAccepted = AtomicLong(0)
    
    fun shouldProcessEvent(rawEvent: RawCustomerEvent): Boolean {
        totalEventsProcessed.incrementAndGet()
        
        try {
            logger.debug("Filtering event: ${rawEvent.eventId}, type: ${rawEvent.eventType}")
            
            // Apply all filter criteria
            val passesEventTypeFilter = filterByEventType(rawEvent.eventType)
            val passesCustomerFilter = filterByCustomerSegment(rawEvent.customerId)
            val passesAgeFilter = filterByEventAge(rawEvent.timestamp)
            val passesQualityFilter = filterByDataQuality(rawEvent.rawData)
            val passesSourceFilter = filterBySource(rawEvent.source)
            val passesBusinessRules = filterByBusinessRules(rawEvent)
            
            val shouldProcess = passesEventTypeFilter && 
                              passesCustomerFilter && 
                              passesAgeFilter && 
                              passesQualityFilter && 
                              passesSourceFilter && 
                              passesBusinessRules
            
            if (shouldProcess) {
                eventsAccepted.incrementAndGet()
                logger.debug("Event accepted: ${rawEvent.eventId}")
            } else {
                eventsFiltered.incrementAndGet()
                val reason = getFilterReason(rawEvent)
                logger.info("Event filtered: ${rawEvent.eventId}, reason: $reason")
            }
            
            return shouldProcess
            
        } catch (e: Exception) {
            logger.error("Error filtering event: ${rawEvent.eventId}", e)
            eventsFiltered.incrementAndGet()
            return false
        }
    }
    
    fun filterByEventType(eventType: String): Boolean {
        // Filter events by type - only process specific event types
        val allowedEventTypes = setOf(
            "USER_REGISTRATION",
            "USER_LOGIN", 
            "PURCHASE_COMPLETED",
            "PURCHASE_CANCELLED",
            "PRODUCT_VIEWED",
            "CART_ABANDONED",
            "SUPPORT_TICKET_CREATED",
            "REVIEW_SUBMITTED"
        )
        
        val isAllowed = eventType in allowedEventTypes
        logger.debug("Event type filter: $eventType -> $isAllowed")
        return isAllowed
    }
    
    fun filterByCustomerSegment(customerId: String): Boolean {
        // Filter events by customer segment - exclude test and internal customers
        val excludePatterns = listOf(
            "TEST_", "INTERNAL_", "DEMO_", "SYSTEM_", "BOT_"
        )
        
        val isExcluded = excludePatterns.any { pattern -> 
            customerId.startsWith(pattern, ignoreCase = true) 
        }
        
        // Also exclude obviously invalid customer IDs
        val isValid = customerId.isNotBlank() && 
                     customerId.length >= 3 && 
                     customerId.length <= 50 &&
                     !customerId.contains("null", ignoreCase = true)
        
        val passes = !isExcluded && isValid
        logger.debug("Customer segment filter: $customerId -> $passes")
        return passes
    }
    
    fun filterByEventAge(timestamp: Long): Boolean {
        // Filter events by age - only process recent events within time window
        val now = System.currentTimeMillis()
        val maxAge = 24 * 60 * 60 * 1000L // 24 hours
        val maxFuture = 5 * 60 * 1000L // 5 minutes in future
        
        val age = now - timestamp
        val isRecent = age <= maxAge && age >= -maxFuture
        
        logger.debug("Event age filter: timestamp=$timestamp, age=${age}ms -> $isRecent")
        return isRecent
    }
    
    fun filterByDataQuality(rawData: Map<String, Any>): Boolean {
        // Filter events by data quality - ensure sufficient data quality
        if (rawData.isEmpty()) {
            logger.debug("Data quality filter: empty data -> false")
            return false
        }
        
        // Check for minimum required fields
        val hasRequiredFields = rawData.size >= 2
        
        // Check for obvious test or invalid data
        val hasValidData = !rawData.values.any { value ->
            when (value) {
                is String -> value.equals("test", ignoreCase = true) || 
                           value.equals("null", ignoreCase = true) ||
                           value.isBlank()
                is Number -> value.toDouble() < 0 && value.toString().contains("test")
                else -> false
            }
        }
        
        val passes = hasRequiredFields && hasValidData
        logger.debug("Data quality filter: fields=${rawData.size}, valid=$hasValidData -> $passes")
        return passes
    }
    
    fun filterBySource(source: String): Boolean {
        // Filter by event source - only allow trusted sources
        val trustedSources = setOf(
            "web-app", 
            "mobile-app", 
            "api", 
            "batch-import"
        )
        
        val isTrusted = source in trustedSources
        logger.debug("Source filter: $source -> $isTrusted")
        return isTrusted
    }
    
    fun filterByBusinessRules(rawEvent: RawCustomerEvent): Boolean {
        // Apply custom business rules
        try {
            // Rule 1: Don't process events from blocked sources during maintenance hours
            if (rawEvent.source == "batch-import") {
                val hour = java.time.LocalDateTime.now().hour
                if (hour in 2..4) { // Maintenance window 2-4 AM
                    logger.debug("Business rule filter: batch import during maintenance -> false")
                    return false
                }
            }
            
            // Rule 2: Limit high-frequency events from same customer
            if (rawEvent.eventType == "PRODUCT_VIEWED") {
                // Simulate rate limiting (in real system, use Redis or similar)
                val hash = rawEvent.customerId.hashCode()
                if (hash % 10 == 0) { // Simulate 10% rate limiting for viewed events
                    logger.debug("Business rule filter: rate limited product view -> false")
                    return false
                }
            }
            
            // Rule 3: Filter out events with suspicious patterns
            val suspiciousPatterns = listOf("script", "injection", "attack", "spam")
            val hasSuspiciousData = rawEvent.rawData.values.any { value ->
                if (value is String) {
                    suspiciousPatterns.any { pattern -> 
                        value.contains(pattern, ignoreCase = true) 
                    }
                } else false
            }
            
            if (hasSuspiciousData) {
                logger.debug("Business rule filter: suspicious data detected -> false")
                return false
            }
            
            // Rule 4: Process only recent registration events
            if (rawEvent.eventType == "USER_REGISTRATION") {
                val registrationAge = System.currentTimeMillis() - rawEvent.timestamp
                val maxRegistrationAge = 2 * 60 * 60 * 1000L // 2 hours
                
                if (registrationAge > maxRegistrationAge) {
                    logger.debug("Business rule filter: old registration event -> false")
                    return false
                }
            }
            
            logger.debug("Business rules filter: passed all rules -> true")
            return true
            
        } catch (e: Exception) {
            logger.error("Error applying business rules filter", e)
            return false
        }
    }
    
    fun getFilterReason(rawEvent: RawCustomerEvent): String? {
        // Determine the specific reason why an event was filtered
        if (!filterByEventType(rawEvent.eventType)) {
            return "UNSUPPORTED_EVENT_TYPE: ${rawEvent.eventType}"
        }
        
        if (!filterByCustomerSegment(rawEvent.customerId)) {
            return "INVALID_CUSTOMER: ${rawEvent.customerId}"
        }
        
        if (!filterByEventAge(rawEvent.timestamp)) {
            val age = System.currentTimeMillis() - rawEvent.timestamp
            return "EVENT_TOO_OLD: age=${age}ms"
        }
        
        if (!filterByDataQuality(rawEvent.rawData)) {
            return "POOR_DATA_QUALITY: fields=${rawEvent.rawData.size}"
        }
        
        if (!filterBySource(rawEvent.source)) {
            return "UNTRUSTED_SOURCE: ${rawEvent.source}"
        }
        
        if (!filterByBusinessRules(rawEvent)) {
            return "BUSINESS_RULE_VIOLATION"
        }
        
        return null // Should not reach here if event was actually filtered
    }
    
    // Statistics and utility methods
    fun getFilterStatistics(): Map<String, Any> {
        val total = totalEventsProcessed.get()
        val filtered = eventsFiltered.get()
        val accepted = eventsAccepted.get()
        
        return mapOf(
            "totalEventsProcessed" to total,
            "eventsFiltered" to filtered,
            "eventsAccepted" to accepted,
            "filterRate" to if (total > 0) (filtered.toDouble() / total * 100) else 0.0,
            "acceptanceRate" to if (total > 0) (accepted.toDouble() / total * 100) else 0.0
        )
    }
    
    fun resetStatistics() {
        totalEventsProcessed.set(0)
        eventsFiltered.set(0)
        eventsAccepted.set(0)
        logger.info("Filter statistics reset")
    }
}