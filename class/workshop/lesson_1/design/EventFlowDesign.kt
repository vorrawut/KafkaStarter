package com.learning.KafkaStarter.design

/**
 * Workshop Exercise: Design Event Flow for E-Commerce System
 * 
 * Design a complete event-driven architecture for an e-commerce platform
 * showing how events flow between services.
 */

data class Event(
    val name: String,
    val producer: String,
    val consumers: List<String>,
    val payload: String,
    val triggerCondition: String
)

data class Service(
    val name: String,
    val responsibility: String,
    val eventsProduced: List<String>,
    val eventsConsumed: List<String>
)

class EventFlowDesign {
    
    // TODO: Define all events in the e-commerce system
    fun defineEvents(): List<Event> {
        return listOf(
            // TODO: Complete the user registration event
            Event(
                name = "UserRegistered",
                producer = "User Service",
                consumers = listOf(
                    "TODO: Which services care about new users?",
                    "TODO: Add more consumers"
                ),
                payload = "TODO: What data goes in this event?",
                triggerCondition = "TODO: When is this event triggered?"
            ),
            
            // TODO: Add more events:
            // - OrderPlaced
            // - PaymentProcessed  
            // - InventoryReserved
            // - OrderShipped
            // - OrderDelivered
            // - ProductViewed
            // - CartUpdated
            
        )
    }
    
    // TODO: Define all services in the system
    fun defineServices(): List<Service> {
        return listOf(
            // TODO: Complete the Order Service definition
            Service(
                name = "Order Service",
                responsibility = "TODO: What does the Order Service do?",
                eventsProduced = listOf(
                    "TODO: What events does Order Service produce?"
                ),
                eventsConsumed = listOf(
                    "TODO: What events does Order Service consume?"
                )
            ),
            
            // TODO: Add more services:
            // - User Service
            // - Payment Service
            // - Inventory Service  
            // - Shipping Service
            // - Notification Service
            // - Analytics Service
            
        )
    }
    
    // TODO: Design the event flow for placing an order
    fun designOrderFlow(): List<String> {
        // TODO: Return the sequence of events when a user places an order
        // Example: ["UserClicksOrder", "OrderValidated", "InventoryChecked", ...]
        
        return listOf(
            "TODO: Step 1 - What happens first?",
            "TODO: Step 2 - What happens next?",
            // Continue the flow...
        )
    }
    
    // TODO: Identify potential issues with the event flow
    fun identifyPotentialIssues(): Map<String, List<String>> {
        return mapOf(
            "Consistency Issues" to listOf(
                "TODO: What consistency problems might occur?",
                "TODO: How could data get out of sync?"
            ),
            "Performance Issues" to listOf(
                "TODO: What performance bottlenecks could occur?",
                "TODO: How could the system slow down?"
            ),
            "Reliability Issues" to listOf(
                "TODO: What could go wrong with event delivery?",
                "TODO: How do we handle service failures?"
            )
        )
    }
    
    // TODO: Design solutions for the identified issues
    fun designSolutions(): Map<String, String> {
        return mapOf(
            "Event Ordering" to "TODO: How do we ensure events are processed in order?",
            "Duplicate Events" to "TODO: How do we handle duplicate events?",
            "Failed Events" to "TODO: What happens when event processing fails?",
            "Service Discovery" to "TODO: How do services find each other?",
            "Monitoring" to "TODO: How do we monitor event flows?"
        )
    }
}