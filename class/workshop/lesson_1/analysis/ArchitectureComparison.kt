package com.learning.KafkaStarter.analysis

/**
 * Workshop Exercise: Compare Traditional vs Event-Driven Architecture
 * 
 * Complete the analysis below to understand the key differences between
 * traditional synchronous architectures and event-driven systems.
 */

data class ArchitectureCharacteristic(
    val name: String,
    val traditionalApproach: String,
    val eventDrivenApproach: String,
    val eventDrivenAdvantage: String
)

class ArchitectureComparison {
    
    fun getArchitectureComparison(): List<ArchitectureCharacteristic> {
        return listOf(
            // TODO: Complete the coupling comparison
            ArchitectureCharacteristic(
                name = "Coupling",
                traditionalApproach = "TODO: Describe how services are coupled in traditional architecture",
                eventDrivenApproach = "TODO: Describe how events decouple services",
                eventDrivenAdvantage = "TODO: Explain the benefit of loose coupling"
            ),
            
            // TODO: Complete the scalability comparison  
            ArchitectureCharacteristic(
                name = "Scalability",
                traditionalApproach = "TODO: How does traditional architecture handle scale?",
                eventDrivenApproach = "TODO: How do events enable better scaling?",
                eventDrivenAdvantage = "TODO: What's the scaling advantage?"
            ),
            
            // TODO: Complete the failure handling comparison
            ArchitectureCharacteristic(
                name = "Failure Handling", 
                traditionalApproach = "TODO: How do failures propagate in sync systems?",
                eventDrivenApproach = "TODO: How do events isolate failures?",
                eventDrivenAdvantage = "TODO: What's the resilience benefit?"
            ),
            
            // TODO: Add 2 more characteristics to compare
            // HINT: Consider data consistency, real-time processing, integration complexity
        )
    }
    
    // TODO: Implement a method to calculate the "event-readiness" score of a system
    fun calculateEventReadinessScore(
        serviceCount: Int,
        integrationPoints: Int,
        realTimeRequirements: Int,
        dataVolumeGBPerDay: Int
    ): Int {
        // TODO: Create a scoring algorithm that considers:
        // - Number of services (more services = higher event readiness score)
        // - Integration complexity (more integrations = higher score)
        // - Real-time requirements (more real-time needs = higher score)
        // - Data volume (higher volume = higher score)
        // Return a score from 0-100
        
        return 0 // TODO: Replace with actual calculation
    }
    
    // TODO: Create a decision framework for when to use events vs direct calls
    fun shouldUseEvents(
        isAsynchronousOk: Boolean,
        needsDecoupling: Boolean,
        hasMultipleConsumers: Boolean,
        needsAuditTrail: Boolean,
        isHighVolume: Boolean
    ): String {
        // TODO: Implement decision logic
        // Return one of: "Definitely Use Events", "Consider Events", "Direct Calls OK"
        
        return "TODO: Implement decision logic"
    }
}