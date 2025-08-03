package com.learning.KafkaStarter.service

import com.learning.KafkaStarter.model.InventoryCommand
import com.learning.KafkaStarter.model.CommandResult
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.kafka.support.KafkaHeaders
import org.springframework.messaging.handler.annotation.Header
import org.springframework.messaging.handler.annotation.Payload
import org.springframework.stereotype.Service
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong

@Service
class CommandProcessor {
    
    @Autowired
    private lateinit var kafkaTemplate: KafkaTemplate<String, CommandResult>
    
    private val logger = org.slf4j.LoggerFactory.getLogger(CommandProcessor::class.java)
    
    // In-memory inventory simulation (in production, use a database)
    private val inventory = ConcurrentHashMap<String, Int>()
    
    // Processing statistics
    private val commandsProcessed = AtomicLong(0)
    private val successfulCommands = AtomicLong(0)
    private val failedCommands = AtomicLong(0)
    
    init {
        // Initialize some sample inventory
        inventory["PRODUCT_001"] = 100
        inventory["PRODUCT_002"] = 250
        inventory["PRODUCT_003"] = 75
        inventory["PRODUCT_004"] = 150
        inventory["PRODUCT_005"] = 300
    }
    
    @KafkaListener(topics = ["inventory-commands"])
    fun processInventoryCommand(
        @Payload command: InventoryCommand,
        @Header(KafkaHeaders.RECEIVED_PARTITION_ID) partition: Int,
        @Header(KafkaHeaders.OFFSET) offset: Long
    ) {
        val startTime = System.currentTimeMillis()
        commandsProcessed.incrementAndGet()
        
        try {
            logger.info("Processing inventory command: commandId=${command.commandId}, " +
                "type=${command.commandType}, productId=${command.productId}, " +
                "quantity=${command.quantity}, partition=$partition, offset=$offset")
            
            // Validate command before processing
            if (!validateCommand(command)) {
                val errorResult = CommandResult(
                    commandId = command.commandId,
                    status = "FAILED",
                    result = mapOf("error" to "Command validation failed"),
                    message = "Invalid command: ${command.commandId}",
                    processingTimeMs = System.currentTimeMillis() - startTime
                )
                
                sendCommandResult(errorResult)
                failedCommands.incrementAndGet()
                return
            }
            
            // Execute inventory update
            val result = executeInventoryUpdate(command)
            
            // Send result back
            sendCommandResult(result)
            
            if (result.status == "SUCCESS") {
                successfulCommands.incrementAndGet()
            } else {
                failedCommands.incrementAndGet()
            }
            
            logger.info("Inventory command processed successfully: commandId=${command.commandId}, " +
                "status=${result.status}, processingTime=${result.processingTimeMs}ms")
            
        } catch (e: Exception) {
            failedCommands.incrementAndGet()
            logger.error("Failed to process inventory command: commandId=${command.commandId}, " +
                "partition=$partition, offset=$offset", e)
            
            // Send error result
            val errorResult = CommandResult(
                commandId = command.commandId,
                status = "ERROR",
                result = mapOf("error" to (e.message ?: "Unknown error")),
                message = "Command processing failed: ${e.message}",
                processingTimeMs = System.currentTimeMillis() - startTime
            )
            
            sendCommandResult(errorResult)
        }
    }
    
    @KafkaListener(topics = ["api-commands"])
    fun processApiCommand(
        @Payload command: InventoryCommand,
        @Header(KafkaHeaders.RECEIVED_TOPIC) topic: String
    ) {
        try {
            logger.info("Processing API command: commandId=${command.commandId}, " +
                "type=${command.commandType}, topic=$topic")
            
            // API commands trigger different behavior
            when (command.commandType) {
                "SYNC_INVENTORY" -> {
                    // Trigger external API synchronization
                    triggerInventorySyncApi(command)
                }
                "UPDATE_CATALOG" -> {
                    // Trigger catalog update API
                    triggerCatalogUpdateApi(command)
                }
                "NOTIFY_SUPPLIERS" -> {
                    // Trigger supplier notification API
                    triggerSupplierNotificationApi(command)
                }
                else -> {
                    logger.warn("Unknown API command type: ${command.commandType}")
                }
            }
            
        } catch (e: Exception) {
            logger.error("Failed to process API command: commandId=${command.commandId}, " +
                "topic=$topic", e)
        }
    }
    
    fun executeInventoryUpdate(command: InventoryCommand): CommandResult {
        val startTime = System.currentTimeMillis()
        
        try {
            val currentStock = inventory.getOrDefault(command.productId, 0)
            val resultData = mutableMapOf<String, Any>()
            
            when (command.commandType) {
                "ADD_STOCK" -> {
                    val newStock = currentStock + command.quantity
                    inventory[command.productId] = newStock
                    
                    resultData["previousStock"] = currentStock
                    resultData["addedQuantity"] = command.quantity
                    resultData["newStock"] = newStock
                    resultData["operation"] = "ADD_STOCK"
                    
                    logger.info("Added stock: productId=${command.productId}, " +
                        "added=${command.quantity}, newStock=$newStock")
                }
                
                "REMOVE_STOCK" -> {
                    if (currentStock >= command.quantity) {
                        val newStock = currentStock - command.quantity
                        inventory[command.productId] = newStock
                        
                        resultData["previousStock"] = currentStock
                        resultData["removedQuantity"] = command.quantity
                        resultData["newStock"] = newStock
                        resultData["operation"] = "REMOVE_STOCK"
                        
                        logger.info("Removed stock: productId=${command.productId}, " +
                            "removed=${command.quantity}, newStock=$newStock")
                    } else {
                        return CommandResult(
                            commandId = command.commandId,
                            status = "FAILED",
                            result = mapOf(
                                "error" to "Insufficient stock",
                                "currentStock" to currentStock,
                                "requestedQuantity" to command.quantity
                            ),
                            message = "Cannot remove ${command.quantity} items, only $currentStock available",
                            processingTimeMs = System.currentTimeMillis() - startTime
                        )
                    }
                }
                
                "SET_STOCK" -> {
                    inventory[command.productId] = command.quantity
                    
                    resultData["previousStock"] = currentStock
                    resultData["newStock"] = command.quantity
                    resultData["operation"] = "SET_STOCK"
                    
                    logger.info("Set stock: productId=${command.productId}, " +
                        "newStock=${command.quantity}")
                }
                
                "CHECK_STOCK" -> {
                    resultData["currentStock"] = currentStock
                    resultData["productId"] = command.productId
                    resultData["operation"] = "CHECK_STOCK"
                    
                    logger.info("Checked stock: productId=${command.productId}, " +
                        "currentStock=$currentStock")
                }
                
                else -> {
                    return CommandResult(
                        commandId = command.commandId,
                        status = "FAILED",
                        result = mapOf("error" to "Unknown command type: ${command.commandType}"),
                        message = "Unsupported command type",
                        processingTimeMs = System.currentTimeMillis() - startTime
                    )
                }
            }
            
            return CommandResult(
                commandId = command.commandId,
                status = "SUCCESS",
                result = resultData,
                message = "Command executed successfully",
                processingTimeMs = System.currentTimeMillis() - startTime
            )
            
        } catch (e: Exception) {
            logger.error("Error executing inventory update: commandId=${command.commandId}", e)
            
            return CommandResult(
                commandId = command.commandId,
                status = "ERROR",
                result = mapOf("error" to (e.message ?: "Unknown error")),
                message = "Command execution failed",
                processingTimeMs = System.currentTimeMillis() - startTime
            )
        }
    }
    
    fun validateCommand(command: InventoryCommand): Boolean {
        // Validate required fields
        if (command.commandId.isBlank() || command.productId.isBlank()) {
            logger.warn("Missing required fields: commandId or productId")
            return false
        }
        
        // Validate command type
        val validCommandTypes = setOf(
            "ADD_STOCK", "REMOVE_STOCK", "SET_STOCK", "CHECK_STOCK",
            "SYNC_INVENTORY", "UPDATE_CATALOG", "NOTIFY_SUPPLIERS"
        )
        
        if (command.commandType !in validCommandTypes) {
            logger.warn("Invalid command type: ${command.commandType}")
            return false
        }
        
        // Validate quantity for stock operations
        if (command.commandType in setOf("ADD_STOCK", "REMOVE_STOCK", "SET_STOCK")) {
            if (command.quantity < 0) {
                logger.warn("Invalid quantity: ${command.quantity}")
                return false
            }
        }
        
        // Validate product ID format
        if (!command.productId.matches(Regex("^[A-Z0-9_]{3,20}$"))) {
            logger.warn("Invalid product ID format: ${command.productId}")
            return false
        }
        
        return true
    }
    
    private fun sendCommandResult(result: CommandResult) {
        try {
            kafkaTemplate.send("command-results", result.commandId, result)
            logger.debug("Command result sent: commandId=${result.commandId}, status=${result.status}")
        } catch (e: Exception) {
            logger.error("Failed to send command result: commandId=${result.commandId}", e)
        }
    }
    
    private fun triggerInventorySyncApi(command: InventoryCommand) {
        // Simulate triggering external inventory sync API
        logger.info("Triggering inventory sync API for command: ${command.commandId}")
        
        val apiCall = mapOf(
            "method" to "POST",
            "url" to "https://api.inventory-system.com/sync",
            "body" to mapOf(
                "productId" to command.productId,
                "reason" to command.reason,
                "triggeredBy" to command.commandId
            )
        )
        
        // Send to API triggers topic
        kafkaTemplate.send("api-triggers", command.commandId, apiCall)
    }
    
    private fun triggerCatalogUpdateApi(command: InventoryCommand) {
        // Simulate triggering catalog update API
        logger.info("Triggering catalog update API for command: ${command.commandId}")
        
        val apiCall = mapOf(
            "method" to "PUT",
            "url" to "https://api.catalog-service.com/products/${command.productId}",
            "body" to mapOf(
                "stockLevel" to inventory.getOrDefault(command.productId, 0),
                "lastUpdated" to System.currentTimeMillis(),
                "triggeredBy" to command.commandId
            )
        )
        
        kafkaTemplate.send("api-triggers", command.commandId, apiCall)
    }
    
    private fun triggerSupplierNotificationApi(command: InventoryCommand) {
        // Simulate triggering supplier notification API
        logger.info("Triggering supplier notification API for command: ${command.commandId}")
        
        val currentStock = inventory.getOrDefault(command.productId, 0)
        
        if (currentStock < 50) { // Low stock threshold
            val apiCall = mapOf(
                "method" to "POST",
                "url" to "https://api.supplier-portal.com/notifications",
                "body" to mapOf(
                    "productId" to command.productId,
                    "currentStock" to currentStock,
                    "alertType" to "LOW_STOCK",
                    "triggeredBy" to command.commandId
                )
            )
            
            kafkaTemplate.send("api-triggers", command.commandId, apiCall)
        }
    }
    
    // Utility methods
    fun getCurrentInventory(): Map<String, Int> = inventory.toMap()
    
    fun getProcessingStatistics(): Map<String, Any> {
        return mapOf(
            "commandsProcessed" to commandsProcessed.get(),
            "successfulCommands" to successfulCommands.get(),
            "failedCommands" to failedCommands.get(),
            "successRate" to if (commandsProcessed.get() > 0) {
                (successfulCommands.get().toDouble() / commandsProcessed.get()) * 100
            } else 0.0,
            "currentInventoryCount" to inventory.size
        )
    }
    
    fun resetStatistics() {
        commandsProcessed.set(0)
        successfulCommands.set(0)
        failedCommands.set(0)
        logger.info("Command processor statistics reset")
    }
}