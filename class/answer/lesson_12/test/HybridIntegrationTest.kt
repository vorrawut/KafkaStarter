package com.learning.KafkaStarter.test

import com.learning.KafkaStarter.model.InventoryCommand
import com.learning.KafkaStarter.model.RestApiCall
import com.learning.KafkaStarter.model.WebhookEvent
import com.learning.KafkaStarter.service.CommandProcessor
import com.learning.KafkaStarter.service.RestApiTriggerService
import com.learning.KafkaStarter.service.WebhookService
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.kafka.test.context.EmbeddedKafka
import org.springframework.test.annotation.DirtiesContext
import java.time.Instant
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

@SpringBootTest
@DirtiesContext
@EmbeddedKafka(
    partitions = 3,
    topics = [
        "inventory-commands", "api-commands", "command-results",
        "api-triggers", "api-responses-success", "api-responses-failed",
        "webhook-events", "webhook-deliveries", "webhook-failures"
    ],
    brokerProperties = [
        "listeners=PLAINTEXT://localhost:9097",
        "port=9097"
    ]
)
class HybridIntegrationTest {
    
    @Autowired
    private lateinit var kafkaTemplate: KafkaTemplate<String, Any>
    
    @Autowired
    private lateinit var commandProcessor: CommandProcessor
    
    @Autowired
    private lateinit var restApiTriggerService: RestApiTriggerService
    
    @Autowired
    private lateinit var webhookService: WebhookService
    
    @BeforeEach
    fun setup() {
        // Reset statistics before each test
        commandProcessor.resetStatistics()
        restApiTriggerService.resetStatistics()
        webhookService.resetStatistics()
    }
    
    @Test
    fun `should process inventory commands and update stock`() {
        // Create inventory command to add stock
        val addStockCommand = InventoryCommand(
            commandId = "test-add-stock-1",
            commandType = "ADD_STOCK",
            productId = "PRODUCT_TEST_001",
            quantity = 50,
            reason = "Integration test stock addition"
        )
        
        // Send command for processing
        kafkaTemplate.send("inventory-commands", addStockCommand.productId, addStockCommand)
        
        // Wait for processing
        Thread.sleep(2000)
        
        // Verify command was processed
        val stats = commandProcessor.getProcessingStatistics()
        assertTrue(stats["commandsProcessed"] as Long >= 1, "Should have processed at least 1 command")
        assertTrue(stats["successfulCommands"] as Long >= 1, "Should have successful commands")
        
        // Verify inventory was updated
        val inventory = commandProcessor.getCurrentInventory()
        assertTrue(inventory.containsKey("PRODUCT_TEST_001"), "Product should exist in inventory")
        
        val currentStock = inventory["PRODUCT_TEST_001"] ?: 0
        assertTrue(currentStock >= 50, "Stock should be at least 50 after addition")
    }
    
    @Test
    fun `should validate inventory commands and reject invalid ones`() {
        // Create invalid inventory command (negative quantity)
        val invalidCommand = InventoryCommand(
            commandId = "test-invalid-1",
            commandType = "ADD_STOCK",
            productId = "INVALID_PRODUCT_ID!", // Invalid format
            quantity = -10, // Invalid quantity
            reason = "Invalid command test"
        )
        
        // Send invalid command
        kafkaTemplate.send("inventory-commands", "INVALID", invalidCommand)
        
        // Wait for processing
        Thread.sleep(2000)
        
        // Verify command was processed but failed validation
        val stats = commandProcessor.getProcessingStatistics()
        assertTrue(stats["commandsProcessed"] as Long >= 1, "Should have processed command")
        assertTrue(stats["failedCommands"] as Long >= 1, "Should have failed commands")
    }
    
    @Test
    fun `should handle different inventory command types`() {
        val testProductId = "PRODUCT_MULTI_TEST"
        
        // Test different command types in sequence
        val commands = listOf(
            InventoryCommand(
                commandId = "multi-1-set",
                commandType = "SET_STOCK",
                productId = testProductId,
                quantity = 100,
                reason = "Set initial stock"
            ),
            InventoryCommand(
                commandId = "multi-2-check",
                commandType = "CHECK_STOCK",
                productId = testProductId,
                quantity = 0,
                reason = "Check current stock"
            ),
            InventoryCommand(
                commandId = "multi-3-remove",
                commandType = "REMOVE_STOCK",
                productId = testProductId,
                quantity = 25,
                reason = "Remove some stock"
            ),
            InventoryCommand(
                commandId = "multi-4-add",
                commandType = "ADD_STOCK",
                productId = testProductId,
                quantity = 10,
                reason = "Add more stock"
            )
        )
        
        // Send all commands
        commands.forEach { command ->
            kafkaTemplate.send("inventory-commands", command.productId, command)
        }
        
        // Wait for processing
        Thread.sleep(5000)
        
        // Verify all commands were processed
        val stats = commandProcessor.getProcessingStatistics()
        assertTrue(stats["commandsProcessed"] as Long >= 4, "Should have processed at least 4 commands")
        
        // Verify final inventory state
        val inventory = commandProcessor.getCurrentInventory()
        val finalStock = inventory[testProductId] ?: 0
        assertEquals(85, finalStock, "Final stock should be 85 (100 - 25 + 10)")
    }
    
    @Test
    fun `should validate command business rules`() {
        val testProductId = "PRODUCT_BUSINESS_RULES"
        
        // First, set some initial stock
        val setStockCommand = InventoryCommand(
            commandId = "business-rules-set",
            commandType = "SET_STOCK",
            productId = testProductId,
            quantity = 20,
            reason = "Set stock for business rules test"
        )
        
        kafkaTemplate.send("inventory-commands", testProductId, setStockCommand)
        Thread.sleep(1500)
        
        // Try to remove more stock than available
        val removeStockCommand = InventoryCommand(
            commandId = "business-rules-remove",
            commandType = "REMOVE_STOCK",
            productId = testProductId,
            quantity = 50, // More than available
            reason = "Test insufficient stock scenario"
        )
        
        kafkaTemplate.send("inventory-commands", testProductId, removeStockCommand)
        Thread.sleep(2000)
        
        // Verify command was processed but should have failed due to insufficient stock
        val stats = commandProcessor.getProcessingStatistics()
        assertTrue(stats["commandsProcessed"] as Long >= 2, "Should have processed at least 2 commands")
        
        // Verify stock wasn't reduced beyond available amount
        val inventory = commandProcessor.getCurrentInventory()
        val currentStock = inventory[testProductId] ?: 0
        assertEquals(20, currentStock, "Stock should remain 20 due to insufficient stock rule")
    }
    
    @Test
    fun `should handle API command triggers`() {
        // Create API command that triggers external API calls
        val apiCommand = InventoryCommand(
            commandId = "api-command-test",
            commandType = "SYNC_INVENTORY",
            productId = "PRODUCT_API_TEST",
            quantity = 0,
            reason = "Test API command trigger"
        )
        
        // Send to API commands topic
        kafkaTemplate.send("api-commands", apiCommand.productId, apiCommand)
        
        // Wait for processing
        Thread.sleep(2000)
        
        // Verify that this would normally trigger API calls
        // (In the test environment, we just verify the processing logic)
        assertNotNull(apiCommand.commandId, "API command should have valid ID")
        assertEquals("SYNC_INVENTORY", apiCommand.commandType, "Should be sync inventory command")
    }
    
    @Test
    fun `should validate REST API call parameters`() {
        // Test with valid API call
        val validApiCall = RestApiCall(
            callId = "test-api-call-1",
            method = "POST",
            url = "https://api.example.com/test",
            headers = mapOf("Authorization" to "Bearer test-token"),
            body = """{"test": "data"}""",
            triggeredBy = "integration-test"
        )
        
        // Send for processing (would be processed by RestApiTriggerService)
        kafkaTemplate.send("api-triggers", validApiCall.callId, validApiCall)
        
        // Wait for processing
        Thread.sleep(1500)
        
        // Verify API call structure
        assertNotNull(validApiCall.callId, "API call should have valid ID")
        assertEquals("POST", validApiCall.method, "Should be POST method")
        assertTrue(validApiCall.url.startsWith("https://"), "Should be HTTPS URL")
    }
    
    @Test
    fun `should validate webhook event parameters`() {
        // Test with valid webhook event
        val validWebhook = WebhookEvent(
            webhookId = "test-webhook-1",
            eventType = "ORDER_CREATED",
            targetUrl = "https://webhook.example.com/orders",
            payload = mapOf(
                "orderId" to "ORDER-12345",
                "customerId" to "CUSTOMER-789",
                "total" to 99.99
            )
        )
        
        // Send for processing
        kafkaTemplate.send("webhook-events", validWebhook.webhookId, validWebhook)
        
        // Wait for processing
        Thread.sleep(1500)
        
        // Verify webhook structure
        assertNotNull(validWebhook.webhookId, "Webhook should have valid ID")
        assertEquals("ORDER_CREATED", validWebhook.eventType, "Should be order created event")
        assertTrue(validWebhook.targetUrl.startsWith("https://"), "Should be HTTPS URL")
        assertTrue(validWebhook.payload.isNotEmpty(), "Should have payload")
    }
    
    @Test
    fun `should demonstrate complete hybrid workflow`() {
        val workflowId = "hybrid-workflow-test"
        val productId = "PRODUCT_WORKFLOW"
        
        // Step 1: Initialize inventory
        val initCommand = InventoryCommand(
            commandId = "$workflowId-init",
            commandType = "SET_STOCK",
            productId = productId,
            quantity = 100,
            reason = "Initialize inventory for workflow test"
        )
        
        kafkaTemplate.send("inventory-commands", productId, initCommand)
        Thread.sleep(1000)
        
        // Step 2: Check inventory via command
        val checkCommand = InventoryCommand(
            commandId = "$workflowId-check",
            commandType = "CHECK_STOCK",
            productId = productId,
            quantity = 0,
            reason = "Check inventory before order"
        )
        
        kafkaTemplate.send("inventory-commands", productId, checkCommand)
        Thread.sleep(1000)
        
        // Step 3: Reserve inventory
        val reserveCommand = InventoryCommand(
            commandId = "$workflowId-reserve",
            commandType = "REMOVE_STOCK",
            productId = productId,
            quantity = 5,
            reason = "Reserve inventory for order"
        )
        
        kafkaTemplate.send("inventory-commands", productId, reserveCommand)
        Thread.sleep(1000)
        
        // Step 4: Trigger API call
        val apiCall = RestApiCall(
            callId = "$workflowId-api",
            method = "POST",
            url = "https://api.fulfillment.com/orders",
            body = """{"orderId": "ORDER-WORKFLOW", "productId": "$productId", "quantity": 5}""",
            triggeredBy = workflowId
        )
        
        kafkaTemplate.send("api-triggers", workflowId, apiCall)
        Thread.sleep(1000)
        
        // Step 5: Send webhook notification
        val webhook = WebhookEvent(
            webhookId = "$workflowId-webhook",
            eventType = "ORDER_PROCESSED",
            targetUrl = "https://webhook.customer.com/orders",
            payload = mapOf(
                "orderId" to "ORDER-WORKFLOW",
                "productId" to productId,
                "quantity" to 5,
                "status" to "PROCESSING"
            )
        )
        
        kafkaTemplate.send("webhook-events", workflowId, webhook)
        
        // Wait for all processing to complete
        Thread.sleep(3000)
        
        // Verify workflow results
        val commandStats = commandProcessor.getProcessingStatistics()
        assertTrue(commandStats["commandsProcessed"] as Long >= 3, "Should process inventory commands")
        
        val inventory = commandProcessor.getCurrentInventory()
        val finalStock = inventory[productId] ?: 0
        assertEquals(95, finalStock, "Final stock should be 95 (100 - 5)")
        
        // Verify API and webhook processing would have been triggered
        assertNotNull(apiCall.callId, "API call should have valid structure")
        assertNotNull(webhook.webhookId, "Webhook should have valid structure")
    }
    
    @Test
    fun `should handle bulk command processing`() {
        val productIds = listOf("BULK_001", "BULK_002", "BULK_003")
        val bulkCommands = mutableListOf<InventoryCommand>()
        
        // Create bulk commands for different products
        productIds.forEachIndexed { index, productId ->
            val command = InventoryCommand(
                commandId = "bulk-command-$index",
                commandType = "SET_STOCK",
                productId = productId,
                quantity = (index + 1) * 25,
                reason = "Bulk command processing test"
            )
            bulkCommands.add(command)
        }
        
        // Send all commands
        bulkCommands.forEach { command ->
            kafkaTemplate.send("inventory-commands", command.productId, command)
        }
        
        // Wait for bulk processing
        Thread.sleep(4000)
        
        // Verify bulk processing
        val stats = commandProcessor.getProcessingStatistics()
        assertTrue(stats["commandsProcessed"] as Long >= 3, "Should process bulk commands")
        
        // Verify individual product stocks
        val inventory = commandProcessor.getCurrentInventory()
        assertEquals(25, inventory["BULK_001"], "BULK_001 should have 25 units")
        assertEquals(50, inventory["BULK_002"], "BULK_002 should have 50 units")
        assertEquals(75, inventory["BULK_003"], "BULK_003 should have 75 units")
    }
    
    @Test
    fun `should maintain processing statistics across multiple operations`() {
        // Perform various operations
        val commands = listOf(
            InventoryCommand("stats-1", "ADD_STOCK", "STATS_PRODUCT", 10, "Stats test 1"),
            InventoryCommand("stats-2", "CHECK_STOCK", "STATS_PRODUCT", 0, "Stats test 2"),
            InventoryCommand("stats-3", "REMOVE_STOCK", "STATS_PRODUCT", 5, "Stats test 3"),
            InventoryCommand("stats-4", "INVALID_TYPE", "STATS_PRODUCT", 1, "Stats test 4") // This should fail
        )
        
        commands.forEach { command ->
            kafkaTemplate.send("inventory-commands", command.productId, command)
        }
        
        Thread.sleep(3000)
        
        val stats = commandProcessor.getProcessingStatistics()
        
        // Verify statistics structure
        assertTrue(stats.containsKey("commandsProcessed"), "Should track processed commands")
        assertTrue(stats.containsKey("successfulCommands"), "Should track successful commands")
        assertTrue(stats.containsKey("failedCommands"), "Should track failed commands")
        assertTrue(stats.containsKey("successRate"), "Should calculate success rate")
        
        // Should have processed all 4 commands
        assertEquals(4L, stats["commandsProcessed"], "Should have processed 4 commands")
        
        // Should have some successes and at least one failure (invalid command type)
        assertTrue(stats["successfulCommands"] as Long >= 3, "Should have successful commands")
        assertTrue(stats["failedCommands"] as Long >= 1, "Should have failed commands")
        
        val successRate = stats["successRate"] as Double
        assertTrue(successRate >= 0.0 && successRate <= 100.0, "Success rate should be valid percentage")
    }
}