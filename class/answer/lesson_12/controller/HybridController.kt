package com.learning.KafkaStarter.controller

import com.learning.KafkaStarter.model.InventoryCommand
import com.learning.KafkaStarter.model.RestApiCall
import com.learning.KafkaStarter.model.WebhookEvent
import com.learning.KafkaStarter.service.CommandProcessor
import com.learning.KafkaStarter.service.RestApiTriggerService
import com.learning.KafkaStarter.service.WebhookService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.ResponseEntity
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.web.bind.annotation.*
import java.time.Instant
import java.util.*
import kotlin.random.Random

@RestController
@RequestMapping("/api/hybrid")
class HybridController {
    
    @Autowired
    private lateinit var kafkaTemplate: KafkaTemplate<String, Any>
    
    @Autowired
    private lateinit var commandProcessor: CommandProcessor
    
    @Autowired
    private lateinit var restApiTriggerService: RestApiTriggerService
    
    @Autowired
    private lateinit var webhookService: WebhookService
    
    private val logger = org.slf4j.LoggerFactory.getLogger(HybridController::class.java)
    
    @PostMapping("/commands/inventory")
    fun sendInventoryCommand(@RequestBody command: InventoryCommand): ResponseEntity<Map<String, Any>> {
        return try {
            // Send inventory command to Kafka for processing
            kafkaTemplate.send("inventory-commands", command.productId, command)
            
            logger.info("Inventory command sent: commandId=${command.commandId}, " +
                "type=${command.commandType}, productId=${command.productId}")
            
            ResponseEntity.ok(
                mapOf(
                    "success" to true,
                    "message" to "Inventory command sent for processing",
                    "commandId" to command.commandId,
                    "commandType" to command.commandType,
                    "productId" to command.productId,
                    "quantity" to command.quantity,
                    "topic" to "inventory-commands",
                    "timestamp" to System.currentTimeMillis()
                )
            )
        } catch (e: Exception) {
            logger.error("Failed to send inventory command: commandId=${command.commandId}", e)
            
            ResponseEntity.internalServerError().body(
                mapOf(
                    "success" to false,
                    "error" to (e.message ?: "Unknown error"),
                    "commandId" to command.commandId,
                    "timestamp" to System.currentTimeMillis()
                )
            )
        }
    }
    
    @PostMapping("/triggers/api-call")
    fun triggerApiCall(@RequestBody apiCall: RestApiCall): ResponseEntity<Map<String, Any>> {
        return try {
            // Trigger REST API call via Kafka
            kafkaTemplate.send("api-triggers", apiCall.callId, apiCall)
            
            logger.info("API call triggered: callId=${apiCall.callId}, " +
                "method=${apiCall.method}, url=${apiCall.url}")
            
            ResponseEntity.ok(
                mapOf(
                    "success" to true,
                    "message" to "API call triggered via Kafka",
                    "callId" to apiCall.callId,
                    "method" to apiCall.method,
                    "url" to apiCall.url,
                    "triggeredBy" to apiCall.triggeredBy,
                    "topic" to "api-triggers",
                    "timestamp" to System.currentTimeMillis()
                )
            )
        } catch (e: Exception) {
            logger.error("Failed to trigger API call: callId=${apiCall.callId}", e)
            
            ResponseEntity.internalServerError().body(
                mapOf(
                    "success" to false,
                    "error" to (e.message ?: "Unknown error"),
                    "callId" to apiCall.callId,
                    "timestamp" to System.currentTimeMillis()
                )
            )
        }
    }
    
    @PostMapping("/webhooks/send")
    fun sendWebhook(@RequestBody webhookEvent: WebhookEvent): ResponseEntity<Map<String, Any>> {
        return try {
            // Send webhook event to Kafka for processing
            kafkaTemplate.send("webhook-events", webhookEvent.webhookId, webhookEvent)
            
            logger.info("Webhook event sent: webhookId=${webhookEvent.webhookId}, " +
                "eventType=${webhookEvent.eventType}, targetUrl=${webhookEvent.targetUrl}")
            
            ResponseEntity.ok(
                mapOf(
                    "success" to true,
                    "message" to "Webhook event sent for processing",
                    "webhookId" to webhookEvent.webhookId,
                    "eventType" to webhookEvent.eventType,
                    "targetUrl" to webhookEvent.targetUrl,
                    "topic" to "webhook-events",
                    "timestamp" to System.currentTimeMillis()
                )
            )
        } catch (e: Exception) {
            logger.error("Failed to send webhook event: webhookId=${webhookEvent.webhookId}", e)
            
            ResponseEntity.internalServerError().body(
                mapOf(
                    "success" to false,
                    "error" to (e.message ?: "Unknown error"),
                    "webhookId" to webhookEvent.webhookId,
                    "timestamp" to System.currentTimeMillis()
                )
            )
        }
    }
    
    @GetMapping("/stats")
    fun getHybridStats(): ResponseEntity<Map<String, Any>> {
        return try {
            // Get hybrid processing statistics
            val commandStats = commandProcessor.getProcessingStatistics()
            val apiStats = restApiTriggerService.getProcessingStatistics()
            val webhookStats = webhookService.getProcessingStatistics()
            
            ResponseEntity.ok(
                mapOf(
                    "success" to true,
                    "commandProcessing" to commandStats,
                    "apiTriggers" to apiStats,
                    "webhooks" to webhookStats,
                    "topics" to mapOf(
                        "inventoryCommands" to "inventory-commands",
                        "apiCommands" to "api-commands",
                        "commandResults" to "command-results",
                        "apiTriggers" to "api-triggers",
                        "apiResponses" to listOf("api-responses-success", "api-responses-failed"),
                        "webhookEvents" to "webhook-events",
                        "webhookDeliveries" to "webhook-deliveries",
                        "webhookFailures" to "webhook-failures"
                    ),
                    "currentInventory" to commandProcessor.getCurrentInventory(),
                    "systemHealth" to getSystemHealth(),
                    "timestamp" to System.currentTimeMillis()
                )
            )
        } catch (e: Exception) {
            logger.error("Failed to get hybrid statistics", e)
            
            ResponseEntity.internalServerError().body(
                mapOf(
                    "success" to false,
                    "error" to (e.message ?: "Unknown error"),
                    "timestamp" to System.currentTimeMillis()
                )
            )
        }
    }
    
    @PostMapping("/reset-stats")
    fun resetStatistics(): ResponseEntity<Map<String, Any>> {
        return try {
            commandProcessor.resetStatistics()
            restApiTriggerService.resetStatistics()
            webhookService.resetStatistics()
            
            ResponseEntity.ok(
                mapOf(
                    "success" to true,
                    "message" to "All hybrid processing statistics reset",
                    "timestamp" to System.currentTimeMillis()
                )
            )
        } catch (e: Exception) {
            ResponseEntity.internalServerError().body(
                mapOf(
                    "success" to false,
                    "error" to (e.message ?: "Unknown error"),
                    "timestamp" to System.currentTimeMillis()
                )
            )
        }
    }
    
    @PostMapping("/simulate/order-fulfillment")
    fun simulateOrderFulfillment(
        @RequestParam orderId: String,
        @RequestParam quantity: Int
    ): ResponseEntity<Map<String, Any>> {
        return try {
            // Simulate complete order fulfillment workflow
            val productId = "PRODUCT_${(1..5).random().toString().padStart(3, '0')}"
            val workflowId = "workflow-${UUID.randomUUID()}"
            
            logger.info("Simulating order fulfillment: orderId=$orderId, productId=$productId, quantity=$quantity")
            
            // Step 1: Check inventory
            val checkInventoryCommand = InventoryCommand(
                commandId = "$workflowId-check-inventory",
                commandType = "CHECK_STOCK",
                productId = productId,
                quantity = 0,
                reason = "Order fulfillment check for order $orderId"
            )
            
            kafkaTemplate.send("inventory-commands", productId, checkInventoryCommand)
            
            // Step 2: Reserve inventory
            val reserveInventoryCommand = InventoryCommand(
                commandId = "$workflowId-reserve-inventory",
                commandType = "REMOVE_STOCK",
                productId = productId,
                quantity = quantity,
                reason = "Reserve inventory for order $orderId"
            )
            
            kafkaTemplate.send("inventory-commands", productId, reserveInventoryCommand)
            
            // Step 3: Trigger external API calls
            val fulfillmentApiCall = RestApiCall(
                callId = "$workflowId-fulfillment-api",
                method = "POST",
                url = "https://api.fulfillment-center.com/orders",
                headers = mapOf(
                    "Authorization" to "Bearer mock-token",
                    "Content-Type" to "application/json"
                ),
                body = mapOf(
                    "orderId" to orderId,
                    "productId" to productId,
                    "quantity" to quantity,
                    "priority" to "STANDARD"
                ).toString(),
                triggeredBy = workflowId
            )
            
            kafkaTemplate.send("api-triggers", workflowId, fulfillmentApiCall)
            
            // Step 4: Send webhook notifications
            val orderWebhook = WebhookEvent(
                webhookId = "$workflowId-order-webhook",
                eventType = "ORDER_FULFILLMENT_STARTED",
                targetUrl = "https://webhook.customer-system.com/orders",
                payload = mapOf(
                    "orderId" to orderId,
                    "productId" to productId,
                    "quantity" to quantity,
                    "status" to "FULFILLMENT_STARTED",
                    "workflowId" to workflowId,
                    "estimatedShipping" to "2-3 business days"
                )
            )
            
            kafkaTemplate.send("webhook-events", workflowId, orderWebhook)
            
            // Step 5: Schedule follow-up API calls
            val shippingApiCall = RestApiCall(
                callId = "$workflowId-shipping-api",
                method = "POST",
                url = "https://api.shipping-partner.com/shipments",
                headers = mapOf(
                    "Authorization" to "Bearer shipping-token",
                    "Content-Type" to "application/json"
                ),
                body = mapOf(
                    "orderId" to orderId,
                    "productId" to productId,
                    "quantity" to quantity,
                    "shippingAddress" to mapOf(
                        "street" to "123 Customer St",
                        "city" to "Sample City",
                        "zipCode" to "12345"
                    )
                ).toString(),
                triggeredBy = workflowId
            )
            
            kafkaTemplate.send("api-triggers", workflowId, shippingApiCall)
            
            ResponseEntity.ok(
                mapOf(
                    "success" to true,
                    "message" to "Order fulfillment workflow initiated",
                    "orderId" to orderId,
                    "productId" to productId,
                    "quantity" to quantity,
                    "workflowId" to workflowId,
                    "steps" to listOf(
                        mapOf("step" to 1, "action" to "Check inventory", "commandId" to checkInventoryCommand.commandId),
                        mapOf("step" to 2, "action" to "Reserve inventory", "commandId" to reserveInventoryCommand.commandId),
                        mapOf("step" to 3, "action" to "Trigger fulfillment API", "callId" to fulfillmentApiCall.callId),
                        mapOf("step" to 4, "action" to "Send order webhook", "webhookId" to orderWebhook.webhookId),
                        mapOf("step" to 5, "action" to "Trigger shipping API", "callId" to shippingApiCall.callId)
                    ),
                    "expectedEvents" to mapOf(
                        "inventoryCommands" to 2,
                        "apiCalls" to 2,
                        "webhooks" to 1
                    ),
                    "timestamp" to System.currentTimeMillis()
                )
            )
        } catch (e: Exception) {
            logger.error("Failed to simulate order fulfillment: orderId=$orderId", e)
            
            ResponseEntity.internalServerError().body(
                mapOf(
                    "success" to false,
                    "error" to (e.message ?: "Unknown error"),
                    "orderId" to orderId,
                    "timestamp" to System.currentTimeMillis()
                )
            )
        }
    }
    
    @PostMapping("/bulk/commands")
    fun sendBulkCommands(
        @RequestParam(defaultValue = "5") count: Int,
        @RequestParam(defaultValue = "MIXED") commandType: String
    ): ResponseEntity<Map<String, Any>> {
        return try {
            val commands = generateTestCommands(count, commandType)
            
            commands.forEach { command ->
                kafkaTemplate.send("inventory-commands", command.productId, command)
            }
            
            logger.info("Bulk commands sent: count=$count, type=$commandType")
            
            ResponseEntity.ok(
                mapOf(
                    "success" to true,
                    "message" to "Bulk commands sent",
                    "count" to count,
                    "commandType" to commandType,
                    "commands" to commands.map {
                        mapOf(
                            "commandId" to it.commandId,
                            "commandType" to it.commandType,
                            "productId" to it.productId,
                            "quantity" to it.quantity
                        )
                    },
                    "timestamp" to System.currentTimeMillis()
                )
            )
        } catch (e: Exception) {
            logger.error("Failed to send bulk commands: count=$count", e)
            
            ResponseEntity.internalServerError().body(
                mapOf(
                    "success" to false,
                    "error" to (e.message ?: "Unknown error"),
                    "requestedCount" to count,
                    "timestamp" to System.currentTimeMillis()
                )
            )
        }
    }
    
    @PostMapping("/demo/e-commerce-flow")
    fun demonstrateECommerceFlow(): ResponseEntity<Map<String, Any>> {
        return try {
            val demoId = "demo-${UUID.randomUUID()}"
            val orderId = "ORDER-${Random.nextInt(10000, 99999)}"
            val customerId = "CUSTOMER-${Random.nextInt(1000, 9999)}"
            
            logger.info("Demonstrating e-commerce flow: demoId=$demoId, orderId=$orderId")
            
            // Simulate complete e-commerce order flow
            val events = mutableListOf<Map<String, Any>>()
            
            // 1. Inventory check
            val inventoryCheck = InventoryCommand(
                commandId = "$demoId-inventory-check",
                commandType = "CHECK_STOCK",
                productId = "PRODUCT_001",
                quantity = 0,
                reason = "E-commerce order inventory check"
            )
            kafkaTemplate.send("inventory-commands", "PRODUCT_001", inventoryCheck)
            events.add(mapOf("type" to "INVENTORY_COMMAND", "id" to inventoryCheck.commandId))
            
            // 2. Payment API call
            val paymentApi = RestApiCall(
                callId = "$demoId-payment",
                method = "POST",
                url = "https://api.payment-processor.com/charges",
                body = mapOf(
                    "amount" to 9999, // $99.99
                    "currency" to "USD",
                    "customerId" to customerId,
                    "orderId" to orderId
                ).toString(),
                triggeredBy = demoId
            )
            kafkaTemplate.send("api-triggers", demoId, paymentApi)
            events.add(mapOf("type" to "API_CALL", "id" to paymentApi.callId))
            
            // 3. Customer notification webhook
            val customerWebhook = WebhookEvent(
                webhookId = "$demoId-customer-notification",
                eventType = "ORDER_CREATED",
                targetUrl = "https://webhook.customer-portal.com/orders",
                payload = mapOf(
                    "orderId" to orderId,
                    "customerId" to customerId,
                    "status" to "PROCESSING",
                    "total" to 99.99
                )
            )
            kafkaTemplate.send("webhook-events", demoId, customerWebhook)
            events.add(mapOf("type" to "WEBHOOK", "id" to customerWebhook.webhookId))
            
            // 4. Inventory reservation
            val inventoryReserve = InventoryCommand(
                commandId = "$demoId-inventory-reserve",
                commandType = "REMOVE_STOCK",
                productId = "PRODUCT_001",
                quantity = 2,
                reason = "Reserve inventory for order $orderId"
            )
            kafkaTemplate.send("inventory-commands", "PRODUCT_001", inventoryReserve)
            events.add(mapOf("type" to "INVENTORY_COMMAND", "id" to inventoryReserve.commandId))
            
            // 5. Shipping API call
            val shippingApi = RestApiCall(
                callId = "$demoId-shipping",
                method = "POST",
                url = "https://api.shipping-service.com/labels",
                body = mapOf(
                    "orderId" to orderId,
                    "weight" to 1.5,
                    "dimensions" to mapOf("length" to 10, "width" to 8, "height" to 6)
                ).toString(),
                triggeredBy = demoId
            )
            kafkaTemplate.send("api-triggers", demoId, shippingApi)
            events.add(mapOf("type" to "API_CALL", "id" to shippingApi.callId))
            
            // 6. Analytics webhook
            val analyticsWebhook = WebhookEvent(
                webhookId = "$demoId-analytics",
                eventType = "ORDER_ANALYTICS",
                targetUrl = "https://webhook.analytics-platform.com/events",
                payload = mapOf(
                    "orderId" to orderId,
                    "customerId" to customerId,
                    "productId" to "PRODUCT_001",
                    "revenue" to 99.99,
                    "timestamp" to System.currentTimeMillis()
                )
            )
            kafkaTemplate.send("webhook-events", demoId, analyticsWebhook)
            events.add(mapOf("type" to "WEBHOOK", "id" to analyticsWebhook.webhookId))
            
            ResponseEntity.ok(
                mapOf(
                    "success" to true,
                    "message" to "E-commerce flow demonstration initiated",
                    "demoId" to demoId,
                    "orderId" to orderId,
                    "customerId" to customerId,
                    "eventsTriggered" to events.size,
                    "events" to events,
                    "workflow" to mapOf(
                        "inventoryCommands" to 2,
                        "apiCalls" to 2,
                        "webhooks" to 2
                    ),
                    "timestamp" to System.currentTimeMillis()
                )
            )
        } catch (e: Exception) {
            logger.error("Failed to demonstrate e-commerce flow", e)
            
            ResponseEntity.internalServerError().body(
                mapOf(
                    "success" to false,
                    "error" to (e.message ?: "Unknown error"),
                    "timestamp" to System.currentTimeMillis()
                )
            )
        }
    }
    
    private fun generateTestCommands(count: Int, commandType: String): List<InventoryCommand> {
        val commandTypes = when (commandType) {
            "STOCK" -> listOf("ADD_STOCK", "REMOVE_STOCK", "SET_STOCK", "CHECK_STOCK")
            "API" -> listOf("SYNC_INVENTORY", "UPDATE_CATALOG", "NOTIFY_SUPPLIERS")
            "MIXED" -> listOf("ADD_STOCK", "REMOVE_STOCK", "CHECK_STOCK", "SYNC_INVENTORY")
            else -> listOf(commandType)
        }
        
        val products = (1..5).map { "PRODUCT_${it.toString().padStart(3, '0')}" }
        
        return (1..count).map { index ->
            val selectedCommandType = commandTypes.random()
            val productId = products.random()
            val quantity = if (selectedCommandType in setOf("ADD_STOCK", "REMOVE_STOCK", "SET_STOCK")) {
                Random.nextInt(1, 50)
            } else 0
            
            InventoryCommand(
                commandId = "test-command-$index-${UUID.randomUUID()}",
                commandType = selectedCommandType,
                productId = productId,
                quantity = quantity,
                reason = "Test command for bulk processing",
                metadata = mapOf(
                    "testGenerated" to true,
                    "batchIndex" to index,
                    "generatedAt" to System.currentTimeMillis()
                )
            )
        }
    }
    
    private fun getSystemHealth(): Map<String, Any> {
        val commandStats = commandProcessor.getProcessingStatistics()
        val apiStats = restApiTriggerService.getProcessingStatistics()
        val webhookStats = webhookService.getProcessingStatistics()
        
        val commandSuccessRate = commandStats["successRate"] as Double
        val apiSuccessRate = apiStats["successRate"] as Double
        val webhookSuccessRate = webhookStats["successRate"] as Double
        
        val overallHealth = when {
            commandSuccessRate >= 95.0 && apiSuccessRate >= 90.0 && webhookSuccessRate >= 85.0 -> "HEALTHY"
            commandSuccessRate >= 85.0 && apiSuccessRate >= 80.0 && webhookSuccessRate >= 75.0 -> "WARNING"
            else -> "CRITICAL"
        }
        
        return mapOf(
            "overallHealth" to overallHealth,
            "commandProcessingHealth" to if (commandSuccessRate >= 95.0) "HEALTHY" else "WARNING",
            "apiTriggerHealth" to if (apiSuccessRate >= 90.0) "HEALTHY" else "WARNING",
            "webhookHealth" to if (webhookSuccessRate >= 85.0) "HEALTHY" else "WARNING",
            "lastHealthCheck" to System.currentTimeMillis()
        )
    }
}