package com.learning.KafkaStarter.service

import com.learning.KafkaStarter.model.BankTransferEvent
import com.learning.KafkaStarter.model.TransferResult
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.kafka.support.Acknowledgment
import org.springframework.kafka.support.KafkaHeaders
import org.springframework.messaging.handler.annotation.Header
import org.springframework.messaging.handler.annotation.Payload
import org.springframework.stereotype.Service
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong

@Service
class BankTransferConsumer {
    
    private val logger = org.slf4j.LoggerFactory.getLogger(BankTransferConsumer::class.java)
    
    // In-memory store for processed transfers (in production, use database)
    private val processedTransfers = ConcurrentHashMap<String, TransferResult>()
    
    // Counters for tracking processing statistics
    private val processedCount = AtomicLong(0)
    private val duplicateCount = AtomicLong(0)
    private val failedCount = AtomicLong(0)
    private val acknowledgedCount = AtomicLong(0)
    
    @KafkaListener(
        topics = ["bank-transfers"],
        containerFactory = "manualAckContainerFactory"
    )
    fun processBankTransferWithManualAck(
        @Payload bankTransferEvent: BankTransferEvent,
        @Header(KafkaHeaders.RECEIVED_PARTITION_ID) partition: Int,
        @Header(KafkaHeaders.OFFSET) offset: Long,
        acknowledgment: Acknowledgment
    ) {
        val startTime = System.currentTimeMillis()
        
        try {
            logger.info("Processing bank transfer with manual ack: transferId=${bankTransferEvent.transferId}, " +
                "amount=${bankTransferEvent.amount}, partition=$partition, offset=$offset")
            
            // Process the transfer
            val success = processTransfer(bankTransferEvent)
            
            if (success) {
                // Only acknowledge if processing was successful
                acknowledgment.acknowledge()
                acknowledgedCount.incrementAndGet()
                processedCount.incrementAndGet()
                
                val processingTime = System.currentTimeMillis() - startTime
                logger.info("Bank transfer processed successfully and acknowledged: " +
                    "transferId=${bankTransferEvent.transferId}, processingTime=${processingTime}ms")
                
                // Store result for tracking
                val result = TransferResult(
                    transferId = bankTransferEvent.transferId,
                    status = "COMPLETED",
                    resultCode = "SUCCESS",
                    message = "Transfer processed successfully",
                    processingTimeMs = processingTime
                )
                
                processedTransfers[bankTransferEvent.transferId] = result
                
            } else {
                failedCount.incrementAndGet()
                logger.error("Bank transfer processing failed - NOT acknowledging: " +
                    "transferId=${bankTransferEvent.transferId}")
                
                // DO NOT acknowledge - this will cause the message to be redelivered
                // In a production system, you might want to implement retry logic
                // or send to a dead letter topic after a certain number of attempts
            }
            
        } catch (e: Exception) {
            failedCount.incrementAndGet()
            logger.error("Exception during bank transfer processing - NOT acknowledging: " +
                "transferId=${bankTransferEvent.transferId}", e)
            
            // DO NOT acknowledge on exception - message will be redelivered
            // In production, implement proper error handling and monitoring
        }
    }
    
    @KafkaListener(
        topics = ["bank-transfers-idempotent"],
        containerFactory = "idempotentContainerFactory"
    )
    fun processIdempotentBankTransfer(
        consumerRecord: ConsumerRecord<String, BankTransferEvent>
    ) {
        val bankTransferEvent = consumerRecord.value()
        val startTime = System.currentTimeMillis()
        
        try {
            logger.info("Processing idempotent bank transfer: transferId=${bankTransferEvent.transferId}, " +
                "idempotencyKey=${bankTransferEvent.idempotencyKey}, " +
                "partition=${consumerRecord.partition()}, offset=${consumerRecord.offset()}")
            
            // Check if transfer was already processed using idempotency key
            if (isTransferAlreadyProcessed(bankTransferEvent.idempotencyKey)) {
                duplicateCount.incrementAndGet()
                
                val existingResult = processedTransfers[bankTransferEvent.idempotencyKey]
                logger.info("Duplicate transfer detected - skipping processing: " +
                    "transferId=${bankTransferEvent.transferId}, " +
                    "idempotencyKey=${bankTransferEvent.idempotencyKey}, " +
                    "originalResult=${existingResult?.status}")
                
                return // Skip processing - this is a duplicate
            }
            
            // Process the transfer since it's not a duplicate
            val success = processTransfer(bankTransferEvent)
            
            if (success) {
                processedCount.incrementAndGet()
                val processingTime = System.currentTimeMillis() - startTime
                
                logger.info("Idempotent bank transfer processed successfully: " +
                    "transferId=${bankTransferEvent.transferId}, processingTime=${processingTime}ms")
                
                // Store result with idempotency key for future duplicate detection
                val result = TransferResult(
                    transferId = bankTransferEvent.transferId,
                    status = "COMPLETED",
                    resultCode = "SUCCESS",
                    message = "Idempotent transfer processed successfully",
                    processingTimeMs = processingTime,
                    idempotent = true
                )
                
                processedTransfers[bankTransferEvent.idempotencyKey] = result
                
            } else {
                failedCount.incrementAndGet()
                logger.error("Idempotent bank transfer processing failed: " +
                    "transferId=${bankTransferEvent.transferId}")
                
                // Store failure result to prevent reprocessing
                val result = TransferResult(
                    transferId = bankTransferEvent.transferId,
                    status = "FAILED",
                    resultCode = "PROCESSING_ERROR",
                    message = "Transfer processing failed",
                    processingTimeMs = System.currentTimeMillis() - startTime,
                    idempotent = true
                )
                
                processedTransfers[bankTransferEvent.idempotencyKey] = result
            }
            
        } catch (e: Exception) {
            failedCount.incrementAndGet()
            logger.error("Exception during idempotent bank transfer processing: " +
                "transferId=${bankTransferEvent.transferId}", e)
            
            // Store exception result to prevent reprocessing
            val result = TransferResult(
                transferId = bankTransferEvent.transferId,
                status = "ERROR",
                resultCode = "EXCEPTION",
                message = "Exception during processing: ${e.message}",
                processingTimeMs = System.currentTimeMillis() - startTime,
                idempotent = true
            )
            
            processedTransfers[bankTransferEvent.idempotencyKey] = result
        }
    }
    
    fun processTransfer(bankTransferEvent: BankTransferEvent): Boolean {
        // Simulate transfer processing logic with various scenarios
        try {
            logger.debug("Processing transfer: from=${bankTransferEvent.fromAccount}, " +
                "to=${bankTransferEvent.toAccount}, amount=${bankTransferEvent.amount}")
            
            // Simulate account validation
            if (!validateAccount(bankTransferEvent.fromAccount) || 
                !validateAccount(bankTransferEvent.toAccount)) {
                logger.error("Invalid account in transfer: ${bankTransferEvent.transferId}")
                return false
            }
            
            // Simulate balance check
            if (!checkBalance(bankTransferEvent.fromAccount, bankTransferEvent.amount)) {
                logger.error("Insufficient balance for transfer: ${bankTransferEvent.transferId}")
                return false
            }
            
            // Simulate transfer execution with processing time
            val processingTimeMs = (50..200).random()
            Thread.sleep(processingTimeMs.toLong())
            
            // Simulate occasional processing failures for testing
            if (bankTransferEvent.amount > 10000.0 && bankTransferEvent.transferId.hashCode() % 10 == 0) {
                logger.error("Simulated processing failure for high-value transfer: ${bankTransferEvent.transferId}")
                return false
            }
            
            // Execute the transfer (simulate database operations)
            executeTransfer(bankTransferEvent)
            
            logger.debug("Transfer executed successfully: ${bankTransferEvent.transferId}")
            return true
            
        } catch (e: Exception) {
            logger.error("Exception during transfer processing: ${bankTransferEvent.transferId}", e)
            return false
        }
    }
    
    fun isTransferAlreadyProcessed(idempotencyKey: String): Boolean {
        // Check if transfer was already processed using idempotency key
        return processedTransfers.containsKey(idempotencyKey)
    }
    
    private fun validateAccount(accountNumber: String): Boolean {
        // Simulate account validation
        return accountNumber.isNotBlank() && 
               accountNumber.length >= 8 && 
               !accountNumber.startsWith("INVALID_")
    }
    
    private fun checkBalance(fromAccount: String, amount: Double): Boolean {
        // Simulate balance check (mock implementation)
        val mockBalance = fromAccount.hashCode().toDouble().let { 
            if (it < 0) -it else it 
        } % 100000.0
        
        return mockBalance >= amount
    }
    
    private fun executeTransfer(bankTransferEvent: BankTransferEvent) {
        // Simulate actual transfer execution
        // In a real system, this would:
        // 1. Start database transaction
        // 2. Debit from account
        // 3. Credit to account  
        // 4. Create transfer record
        // 5. Commit transaction
        
        logger.debug("Executing transfer: ${bankTransferEvent.transferId} - " +
            "Debiting ${bankTransferEvent.amount} from ${bankTransferEvent.fromAccount}, " +
            "Crediting ${bankTransferEvent.amount} to ${bankTransferEvent.toAccount}")
    }
    
    // Statistics and utility methods
    fun getProcessingStats(): Map<String, Any> {
        return mapOf(
            "processedCount" to processedCount.get(),
            "duplicateCount" to duplicateCount.get(),
            "failedCount" to failedCount.get(),
            "acknowledgedCount" to acknowledgedCount.get(),
            "totalProcessedTransfers" to processedTransfers.size,
            "successRate" to if (processedCount.get() + failedCount.get() > 0) {
                (processedCount.get().toDouble() / (processedCount.get() + failedCount.get())) * 100
            } else 0.0,
            "duplicateRate" to if (processedCount.get() + duplicateCount.get() > 0) {
                (duplicateCount.get().toDouble() / (processedCount.get() + duplicateCount.get())) * 100
            } else 0.0
        )
    }
    
    fun getTransferResult(idempotencyKey: String): TransferResult? {
        return processedTransfers[idempotencyKey]
    }
    
    fun resetCounters() {
        processedCount.set(0)
        duplicateCount.set(0)
        failedCount.set(0)
        acknowledgedCount.set(0)
        processedTransfers.clear()
        logger.info("Bank transfer processing counters and cache reset")
    }
    
    fun getAllProcessedTransfers(): Map<String, TransferResult> {
        return processedTransfers.toMap()
    }
}