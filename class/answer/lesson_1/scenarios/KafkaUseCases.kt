package com.learning.KafkaStarter.scenarios

/**
 * Complete Answer: Real-World Kafka Use Cases
 * 
 * This demonstrates comprehensive analysis of real-world scenarios
 * where Kafka provides significant business value.
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
            KafkaUseCase(
                name = "E-Commerce Order Processing",
                description = "End-to-end order lifecycle management from cart to delivery, coordinating multiple services for payment, inventory, shipping, and customer communication.",
                businessProblem = "Traditional e-commerce systems struggle with service coupling, order consistency across systems, real-time inventory updates, and scaling during peak traffic periods like Black Friday.",
                kafkaSolution = "Event-driven order processing where OrderPlaced events trigger parallel processing by payment, inventory, shipping, and notification services. Event sourcing maintains complete order history.",
                keyBenefits = listOf(
                    "Independent service scaling during traffic spikes",
                    "Fault isolation - payment failures don't break inventory updates", 
                    "Real-time order tracking and customer notifications",
                    "Complete audit trail for regulatory compliance",
                    "Easy addition of new services (recommendations, analytics)"
                ),
                exampleCompanies = listOf("Amazon", "eBay", "Shopify", "Zalando", "Otto")
            ),
            
            KafkaUseCase(
                name = "Financial Services - Real-Time Fraud Detection",
                description = "Real-time analysis of financial transactions to detect fraudulent patterns and suspicious activities before transaction completion.",
                businessProblem = "Banks lose billions annually to fraud. Traditional batch processing detects fraud too late. Need sub-second analysis of transactions against historical patterns and ML models.",
                kafkaSolution = "Transaction events stream to real-time ML models via Kafka. Models analyze patterns, risk scores, and user behavior in real-time. Results trigger automated blocking or manual review workflows.",
                keyBenefits = listOf(
                    "Sub-second fraud detection reduces losses by 60-80%",
                    "Real-time risk scoring improves customer experience",
                    "Scalable ML model deployment for millions of transactions",
                    "Complete transaction audit trail for regulatory compliance",
                    "A/B testing of fraud models in production"
                ),
                exampleCompanies = listOf("JPMorgan Chase", "Capital One", "Square", "PayPal", "Stripe")
            ),
            
            KafkaUseCase(
                name = "IoT Data Streaming - Smart Manufacturing",
                description = "Real-time processing of sensor data from manufacturing equipment to optimize operations, predict maintenance, and ensure quality control.",
                businessProblem = "Manufacturing equipment generates massive sensor data (temperature, pressure, vibration). Traditional systems can't process data in real-time, leading to unexpected downtime and quality issues.",
                kafkaSolution = "Sensor data streams through Kafka to real-time analytics engines. Stream processing detects anomalies, predicts equipment failures, and triggers automated responses or maintenance alerts.",
                keyBenefits = listOf(
                    "Predictive maintenance reduces downtime by 30-50%",
                    "Real-time quality control prevents defective products",
                    "Equipment optimization improves efficiency by 15-25%",
                    "Automated responses reduce human error",
                    "Historical data analytics for continuous improvement"
                ),
                exampleCompanies = listOf("Tesla", "GE", "Siemens", "Boeing", "Ford")
            ),
            
            KafkaUseCase(
                name = "Social Media Activity Feeds",
                description = "Real-time generation of personalized content feeds based on user activities, connections, and preferences across social media platforms.",
                businessProblem = "Social platforms must deliver personalized, real-time content to millions of users. Traditional polling-based systems can't scale and provide stale content, reducing user engagement.",
                kafkaSolution = "User activity events (likes, shares, posts, follows) stream through Kafka. Real-time stream processing generates personalized feeds, recommendations, and notifications for each user.",
                keyBenefits = listOf(
                    "Real-time feed updates increase user engagement by 40%",
                    "Personalized content improves time-on-platform",
                    "Scalable to billions of daily interactions",
                    "A/B testing of recommendation algorithms",
                    "Real-time trending topic detection"
                ),
                exampleCompanies = listOf("LinkedIn", "Twitter", "Facebook", "Instagram", "TikTok")
            ),
            
            KafkaUseCase(
                name = "Log Aggregation & System Monitoring",
                description = "Centralized collection and real-time analysis of application logs, metrics, and system events for monitoring, alerting, and troubleshooting.",
                businessProblem = "Distributed systems generate massive log volumes across hundreds of services. Traditional log aggregation is slow, expensive, and makes debugging difficult during outages.",
                kafkaSolution = "All services publish logs and metrics to Kafka topics. Stream processing performs real-time analysis, aggregation, and alerting. Historical data enables trend analysis and capacity planning.",
                keyBenefits = listOf(
                    "Real-time alerting reduces incident response time by 70%",
                    "Centralized logs improve debugging and root cause analysis",
                    "Cost-effective log storage and retention",
                    "Automated anomaly detection and alerting",
                    "Performance trend analysis for capacity planning"
                ),
                exampleCompanies = listOf("Netflix", "Uber", "Airbnb", "Spotify", "Slack")
            )
        )
    }
    
    fun selectBestUseCase(
        dataVolume: String, // "low", "medium", "high", "massive"
        latencyRequirement: String, // "seconds", "sub-second", "real-time"
        consistencyNeeds: String, // "eventual", "strong"
        scalabilityNeeds: String // "single-region", "multi-region", "global"
    ): String {
        val score = calculateUseCaseScore(dataVolume, latencyRequirement, consistencyNeeds, scalabilityNeeds)
        
        return when {
            score >= 12 -> "IoT Data Streaming - Perfect for massive data volumes with real-time processing needs"
            score >= 10 -> "Financial Fraud Detection - Ideal for real-time analysis with strong consistency requirements"
            score >= 8 -> "Social Media Feeds - Great for high-volume, real-time personalization"
            score >= 6 -> "E-Commerce Order Processing - Excellent for business process coordination"
            else -> "Log Aggregation - Good starting point for centralized monitoring"
        }
    }
    
    private fun calculateUseCaseScore(
        dataVolume: String,
        latencyRequirement: String, 
        consistencyNeeds: String,
        scalabilityNeeds: String
    ): Int {
        var score = 0
        
        // Data volume scoring
        score += when (dataVolume) {
            "massive" -> 4
            "high" -> 3
            "medium" -> 2
            "low" -> 1
            else -> 1
        }
        
        // Latency requirement scoring  
        score += when (latencyRequirement) {
            "real-time" -> 4
            "sub-second" -> 3
            "seconds" -> 2
            else -> 1
        }
        
        // Consistency needs scoring
        score += when (consistencyNeeds) {
            "strong" -> 2
            "eventual" -> 3
            else -> 2
        }
        
        // Scalability needs scoring
        score += when (scalabilityNeeds) {
            "global" -> 4
            "multi-region" -> 3
            "single-region" -> 2
            else -> 1
        }
        
        return score
    }
    
    fun calculateKafkaROI(
        currentIntegrationCost: Double,
        currentDowntimeHours: Int,
        currentDataLagMinutes: Int,
        implementationCost: Double
    ): Map<String, Double> {
        
        // Calculate annual savings from reduced integration complexity
        // Assume Kafka reduces integration costs by 40-60%
        val integrationSavings = currentIntegrationCost * 0.5
        
        // Calculate downtime cost savings
        // Assume $10,000 per hour of downtime cost
        // Kafka typically reduces downtime by 50-70%
        val downtimeCostPerHour = 10000.0
        val downtimeSavings = currentDowntimeHours * downtimeCostPerHour * 0.6
        
        // Calculate speed improvement value
        // Faster data processing improves decision making
        // Estimate 20% productivity improvement from real-time data
        val productivityImprovementValue = currentIntegrationCost * 0.2
        
        // Calculate developer productivity gains
        // Kafka simplifies integration, saving development time
        val developerProductivitySavings = implementationCost * 0.3
        
        val totalSavings = integrationSavings + downtimeSavings + 
                          productivityImprovementValue + developerProductivitySavings
        
        val netROI = ((totalSavings - implementationCost) / implementationCost) * 100
        
        return mapOf(
            "integrationSavings" to integrationSavings,
            "downtimeSavings" to downtimeSavings,
            "speedImprovements" to productivityImprovementValue,
            "developerProductivity" to developerProductivitySavings,
            "totalSavings" to totalSavings,
            "implementationCost" to implementationCost,
            "netROI" to netROI,
            "paybackMonths" to if (totalSavings > 0) (implementationCost / (totalSavings / 12)) else 999.0
        )
    }
    
    // Additional utility methods for use case analysis
    fun getUseCaseByIndustry(industry: String): List<KafkaUseCase> {
        val industryMapping = mapOf(
            "retail" to listOf("E-Commerce Order Processing"),
            "finance" to listOf("Financial Services - Real-Time Fraud Detection"),
            "manufacturing" to listOf("IoT Data Streaming - Smart Manufacturing"),
            "technology" to listOf("Social Media Activity Feeds", "Log Aggregation & System Monitoring"),
            "automotive" to listOf("IoT Data Streaming - Smart Manufacturing")
        )
        
        val relevantUseCases = industryMapping[industry.lowercase()] ?: emptyList()
        return getUseCases().filter { useCase -> 
            relevantUseCases.any { it in useCase.name }
        }
    }
    
    fun validateUseCaseRequirements(
        useCase: KafkaUseCase,
        requirements: Map<String, Any>
    ): Map<String, Boolean> {
        return mapOf(
            "throughputMatch" to (requirements["eventsPerSecond"] as? Int ?: 0 > 1000),
            "latencyMatch" to (requirements["maxLatencyMs"] as? Int ?: 1000 < 1000),
            "scalabilityMatch" to (requirements["expectedGrowth"] as? String == "high"),
            "consistencyMatch" to (requirements["consistencyLevel"] as? String in listOf("eventual", "strong")),
            "overallFit" to true // This would be calculated based on above factors
        )
    }
}