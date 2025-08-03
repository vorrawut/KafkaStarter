package com.learning.KafkaStarter.validation

import org.apache.kafka.clients.admin.AdminClient
import org.apache.kafka.clients.admin.AdminClientConfig
import org.apache.kafka.clients.admin.NewTopic
import org.springframework.kafka.core.KafkaAdmin
import org.springframework.stereotype.Component
import java.net.HttpURLConnection
import java.net.URL
import java.util.*
import java.util.concurrent.TimeUnit

/**
 * Complete Answer: Environment Validation
 * 
 * This demonstrates comprehensive validation of the Kafka environment
 * to ensure all components are properly configured and accessible.
 */

@Component
class EnvironmentValidator {
    
    private val logger = org.slf4j.LoggerFactory.getLogger(EnvironmentValidator::class.java)
    
    fun validateKafkaBroker(bootstrapServers: String): ValidationResult {
        return try {
            logger.info("Validating Kafka broker at: $bootstrapServers")
            
            // Create AdminClient to test broker connectivity
            val props = Properties().apply {
                put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers)
                put(AdminClientConfig.REQUEST_TIMEOUT_MS_CONFIG, 5000)
                put(AdminClientConfig.DEFAULT_API_TIMEOUT_MS_CONFIG, 5000)
            }
            
            AdminClient.create(props).use { adminClient ->
                // Test broker connectivity by getting cluster metadata
                val clusterMetadata = adminClient.describeCluster()
                val brokers = clusterMetadata.nodes().get(5, TimeUnit.SECONDS)
                val clusterId = clusterMetadata.clusterId().get(5, TimeUnit.SECONDS)
                
                ValidationResult(
                    component = "Kafka Broker",
                    isHealthy = brokers.isNotEmpty(),
                    message = "Successfully connected to Kafka cluster",
                    details = mapOf(
                        "bootstrapServers" to bootstrapServers,
                        "clusterId" to clusterId,
                        "brokerCount" to brokers.size,
                        "brokers" to brokers.map { "${it.host()}:${it.port()}" }
                    )
                )
            }
        } catch (e: Exception) {
            logger.error("Failed to validate Kafka broker", e)
            ValidationResult(
                component = "Kafka Broker",
                isHealthy = false,
                message = "Connection failed: ${e.message}",
                details = mapOf(
                    "error" to e.toString(),
                    "bootstrapServers" to bootstrapServers
                )
            )
        }
    }
    
    fun validateSchemaRegistry(schemaRegistryUrl: String): ValidationResult {
        return try {
            logger.info("Validating Schema Registry at: $schemaRegistryUrl")
            
            val url = URL("$schemaRegistryUrl/subjects")
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.connectTimeout = 5000
            connection.readTimeout = 5000
            
            val responseCode = connection.responseCode
            val isHealthy = responseCode in 200..299
            
            val responseBody = if (isHealthy) {
                connection.inputStream.bufferedReader().use { it.readText() }
            } else {
                connection.errorStream?.bufferedReader()?.use { it.readText() } ?: "No response body"
            }
            
            ValidationResult(
                component = "Schema Registry",
                isHealthy = isHealthy,
                message = if (isHealthy) "Schema Registry is accessible" else "Schema Registry returned error $responseCode",
                details = mapOf(
                    "url" to schemaRegistryUrl,
                    "endpoint" to "/subjects",
                    "responseCode" to responseCode,
                    "response" to responseBody.take(200) // Limit response size
                )
            )
        } catch (e: Exception) {
            logger.error("Failed to validate Schema Registry", e)
            ValidationResult(
                component = "Schema Registry", 
                isHealthy = false,
                message = "Connection failed: ${e.message}",
                details = mapOf(
                    "error" to e.toString(),
                    "url" to schemaRegistryUrl
                )
            )
        }
    }
    
    fun validateKafkaUI(kafkaUIUrl: String): ValidationResult {
        return try {
            logger.info("Validating Kafka UI at: $kafkaUIUrl")
            
            val isAccessible = testHttpConnection(kafkaUIUrl, 5000)
            
            ValidationResult(
                component = "Kafka UI",
                isHealthy = isAccessible,
                message = if (isAccessible) "Kafka UI is accessible" else "Kafka UI is not accessible",
                details = mapOf(
                    "url" to kafkaUIUrl,
                    "accessible" to isAccessible
                )
            )
        } catch (e: Exception) {
            logger.error("Failed to validate Kafka UI", e)
            ValidationResult(
                component = "Kafka UI",
                isHealthy = false,
                message = "UI not accessible: ${e.message}",
                details = mapOf(
                    "error" to e.toString(),
                    "url" to kafkaUIUrl
                )
            )
        }
    }
    
    fun validateTopicOperations(bootstrapServers: String): ValidationResult {
        return try {
            logger.info("Validating topic operations")
            
            val props = Properties().apply {
                put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers)
                put(AdminClientConfig.REQUEST_TIMEOUT_MS_CONFIG, 10000)
            }
            
            AdminClient.create(props).use { adminClient ->
                val testTopicName = "environment-test-topic-${System.currentTimeMillis()}"
                
                try {
                    // Create test topic
                    val newTopic = NewTopic(testTopicName, 1, 1.toShort())
                    adminClient.createTopics(listOf(newTopic)).all().get(10, TimeUnit.SECONDS)
                    
                    // List topics to verify creation
                    val topics = adminClient.listTopics().names().get(5, TimeUnit.SECONDS)
                    val topicExists = topics.contains(testTopicName)
                    
                    // Delete test topic
                    adminClient.deleteTopics(listOf(testTopicName)).all().get(10, TimeUnit.SECONDS)
                    
                    ValidationResult(
                        component = "Topic Operations",
                        isHealthy = topicExists,
                        message = "Successfully performed topic operations",
                        details = mapOf(
                            "testTopic" to testTopicName,
                            "operations" to listOf("create", "list", "delete"),
                            "topicCreated" to topicExists,
                            "totalTopics" to topics.size
                        )
                    )
                } catch (e: Exception) {
                    // Clean up test topic if it exists
                    try {
                        adminClient.deleteTopics(listOf(testTopicName))
                    } catch (cleanupException: Exception) {
                        logger.warn("Failed to cleanup test topic", cleanupException)
                    }
                    throw e
                }
            }
        } catch (e: Exception) {
            logger.error("Failed to validate topic operations", e)
            ValidationResult(
                component = "Topic Operations",
                isHealthy = false,
                message = "Topic operations failed: ${e.message}",
                details = mapOf("error" to e.toString())
            )
        }
    }
    
    fun validateCompleteEnvironment(): EnvironmentReport {
        logger.info("Starting complete environment validation")
        
        val results = mutableListOf<ValidationResult>()
        
        // Run all validation checks
        results.add(validateKafkaBroker("localhost:9092"))
        results.add(validateSchemaRegistry("http://localhost:8081"))
        results.add(validateKafkaUI("http://localhost:8080"))
        results.add(validateTopicOperations("localhost:9092"))
        
        // Additional optional checks
        results.add(validateOptionalService("AKHQ", "http://localhost:8082"))
        results.add(validateOptionalService("Prometheus", "http://localhost:9090"))
        results.add(validateOptionalService("Grafana", "http://localhost:3001"))
        
        val allHealthy = results.all { it.isHealthy }
        val healthyCount = results.count { it.isHealthy }
        
        logger.info("Environment validation complete: $healthyCount/$${results.size} components healthy")
        
        return EnvironmentReport(
            isFullyHealthy = allHealthy,
            healthyComponents = healthyCount,
            totalComponents = results.size,
            results = results,
            recommendations = generateRecommendations(results)
        )
    }
    
    private fun validateOptionalService(serviceName: String, url: String): ValidationResult {
        return try {
            val isAccessible = testHttpConnection(url, 3000)
            ValidationResult(
                component = serviceName,
                isHealthy = isAccessible,
                message = if (isAccessible) "$serviceName is accessible" else "$serviceName is not accessible (optional)",
                details = mapOf("url" to url, "optional" to true)
            )
        } catch (e: Exception) {
            ValidationResult(
                component = serviceName,
                isHealthy = false,
                message = "$serviceName not accessible (optional): ${e.message}",
                details = mapOf("url" to url, "optional" to true, "error" to e.toString())
            )
        }
    }
    
    private fun generateRecommendations(results: List<ValidationResult>): List<String> {
        val recommendations = mutableListOf<String>()
        
        results.filter { !it.isHealthy }.forEach { result ->
            when (result.component) {
                "Kafka Broker" -> {
                    recommendations.add("🐳 Check Docker containers: docker-compose ps")
                    recommendations.add("🔍 Check Kafka logs: docker-compose logs broker")
                    recommendations.add("⚡ Verify port 9092 is not in use: lsof -i :9092")
                    recommendations.add("💾 Ensure Docker has at least 4GB memory allocated")
                    recommendations.add("⏱️ Wait 1-2 minutes for Kafka to fully start")
                }
                "Schema Registry" -> {
                    recommendations.add("🔗 Verify Schema Registry depends on Kafka broker")
                    recommendations.add("🔍 Check Schema Registry logs: docker-compose logs schema-registry")
                    recommendations.add("⚡ Verify port 8081 is not in use: lsof -i :8081")
                    recommendations.add("🌐 Test direct access: curl http://localhost:8081/subjects")
                }
                "Kafka UI" -> {
                    recommendations.add("🌐 Open browser to http://localhost:8080")
                    recommendations.add("🔍 Check UI logs: docker-compose logs kafka-ui")
                    recommendations.add("⚡ Verify port 8080 is not in use: lsof -i :8080")
                    recommendations.add("🔗 Ensure UI can connect to Kafka broker")
                }
                "Topic Operations" -> {
                    recommendations.add("⚡ Verify Kafka broker is fully started")
                    recommendations.add("🔑 Check Kafka permissions and ACLs")
                    recommendations.add("🕒 Increase timeout if operations are slow")
                    recommendations.add("🔍 Check for Kafka connectivity issues")
                }
                "AKHQ", "Prometheus", "Grafana" -> {
                    // These are optional services
                    recommendations.add("ℹ️ ${result.component} is optional - you can proceed without it")
                }
            }
        }
        
        if (recommendations.isEmpty()) {
            recommendations.add("🎉 All components are healthy! Your environment is ready for development.")
        }
        
        return recommendations
    }
    
    private fun testHttpConnection(urlString: String, timeoutMs: Int = 5000): Boolean {
        return try {
            val url = URL(urlString)
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.connectTimeout = timeoutMs
            connection.readTimeout = timeoutMs
            
            val responseCode = connection.responseCode
            responseCode in 200..399 // Accept redirects as well
        } catch (e: Exception) {
            logger.debug("HTTP connection test failed for $urlString", e)
            false
        }
    }
}

// Data classes for validation results
data class ValidationResult(
    val component: String,
    val isHealthy: Boolean,
    val message: String,
    val details: Map<String, Any> = emptyMap(),
    val timestamp: Long = System.currentTimeMillis()
)

data class EnvironmentReport(
    val isFullyHealthy: Boolean,
    val healthyComponents: Int,
    val totalComponents: Int,
    val results: List<ValidationResult>,
    val recommendations: List<String>,
    val timestamp: Long = System.currentTimeMillis()
) {
    fun printReport() {
        println("\n🔍 Environment Validation Report")
        println("================================")
        println("Status: ${if (isFullyHealthy) "✅ All Healthy" else "⚠️ Issues Found"}")
        println("Components: $healthyComponents/$totalComponents healthy")
        println()
        
        results.forEach { result ->
            val status = if (result.isHealthy) "✅" else "❌"
            println("$status ${result.component}: ${result.message}")
        }
        
        if (recommendations.isNotEmpty()) {
            println("\n💡 Recommendations:")
            recommendations.forEach { rec ->
                println("  $rec")
            }
        }
        
        println("\n" + "=".repeat(50))
    }
}