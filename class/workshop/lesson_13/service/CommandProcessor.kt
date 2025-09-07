package com.learning.KafkaStarter.service

import com.learning.KafkaStarter.model.InventoryCommand
import com.learning.KafkaStarter.model.CommandResult
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.kafka.support.KafkaHeaders
import org.springframework.messaging.handler.annotation.Header
import org.springframework.messaging.handler.annotation.Payload
import org.springframework.stereotype.Service

@Service
class CommandProcessor {
    
    @KafkaListener(topics = ["inventory-commands"])
    fun processInventoryCommand(
        @Payload command: InventoryCommand,
        @Header(KafkaHeaders.RECEIVED_PARTITION_ID) partition: Int,
        @Header(KafkaHeaders.OFFSET) offset: Long
    ) {
        try {
            // TODO: Process inventory command and execute business logic
            // HINT: Execute command, update inventory, and send result
            TODO("Process inventory command")
        } catch (e: Exception) {
            // TODO: Handle command processing exceptions
            TODO("Handle command processing exception")
        }
    }
    
    @KafkaListener(topics = ["api-commands"])
    fun processApiCommand(
        @Payload command: InventoryCommand,
        @Header(KafkaHeaders.RECEIVED_TOPIC) topic: String
    ) {
        try {
            // TODO: Process API command and trigger REST calls
            // HINT: Parse command and make appropriate API calls
            TODO("Process API command")
        } catch (e: Exception) {
            // TODO: Handle API command processing exceptions
            TODO("Handle API command processing exception")
        }
    }
    
    fun executeInventoryUpdate(command: InventoryCommand): CommandResult {
        // TODO: Execute inventory update logic
        // HINT: Update inventory database and return result
        TODO("Execute inventory update")
    }
    
    fun validateCommand(command: InventoryCommand): Boolean {
        // TODO: Validate command before processing
        // HINT: Check required fields and business rules
        TODO("Validate command")
    }
}