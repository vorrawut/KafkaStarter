package com.learning.KafkaStarter.scenarios

/**
 * Workshop Exercise: Identify Real-World Kafka Use Cases
 * 
 * Complete the use case analysis to understand where Kafka excels
 * in real-world applications.
 */

data class KafkaUseCase(
    val name: String,
    val description: String,
    val businessProblem: String,
    val kafkaSolution: String,
    val keyBenefits: List<String>,
    val exampleCompanies: List<String>
)

class KafkaUseCases {
    
    fun getUseCases(): List<KafkaUseCase> {
        return listOf(
            // TODO: Complete the e-commerce use case
            KafkaUseCase(
                name = "E-Commerce Order Processing",
                description = "TODO: Describe how e-commerce orders flow through systems",
                businessProblem = "TODO: What challenges do e-commerce platforms face?",
                kafkaSolution = "TODO: How does Kafka solve these challenges?",
                keyBenefits = listOf(
                    "TODO: Benefit 1",
                    "TODO: Benefit 2", 
                    "TODO: Benefit 3"
                ),
                exampleCompanies = listOf("Amazon", "eBay", "Shopify") // Real examples
            ),
            
            // TODO: Complete the financial services use case
            KafkaUseCase(
                name = "Financial Services - Real-Time Fraud Detection",
                description = "TODO: How do banks detect fraud in real-time?",
                businessProblem = "TODO: What's the challenge with fraud detection?",
                kafkaSolution = "TODO: How does Kafka enable real-time fraud detection?",
                keyBenefits = listOf(
                    "TODO: Speed benefit",
                    "TODO: Accuracy benefit",
                    "TODO: Cost benefit"
                ),
                exampleCompanies = listOf("JPMorgan Chase", "Capital One", "Square")
            ),
            
            // TODO: Add 3 more use cases:
            // 1. IoT Data Streaming (Tesla, GE, etc.)
            // 2. Social Media Activity Feeds (LinkedIn, Twitter, etc.) 
            // 3. Log Aggregation & Monitoring (Netflix, Uber, etc.)
            
        )
    }
    
    // TODO: Implement use case selection helper
    fun selectBestUseCase(
        dataVolume: String, // "low", "medium", "high", "massive"
        latencyRequirement: String, // "seconds", "sub-second", "real-time"
        consistencyNeeds: String, // "eventual", "strong"
        scalabilityNeeds: String // "single-region", "multi-region", "global"
    ): String {
        // TODO: Implement logic to recommend the best Kafka use case
        // based on requirements
        
        return "TODO: Implement recommendation logic"
    }
    
    // TODO: Calculate ROI for implementing Kafka
    fun calculateKafkaROI(
        currentIntegrationCost: Double,
        currentDowntimeHours: Int,
        currentDataLagMinutes: Int,
        implementationCost: Double
    ): Map<String, Double> {
        // TODO: Calculate potential ROI from implementing Kafka
        // Consider: reduced integration costs, less downtime, faster data
        
        return mapOf(
            "integrationSavings" to 0.0, // TODO: Calculate
            "downtimeSavings" to 0.0,    // TODO: Calculate  
            "speedImprovements" to 0.0,   // TODO: Calculate
            "totalROI" to 0.0             // TODO: Calculate
        )
    }
}