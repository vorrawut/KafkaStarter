package com.learning.KafkaStarter.controller

import com.learning.KafkaStarter.model.BankTransferEvent
import com.learning.KafkaStarter.model.TransferResult
import com.learning.KafkaStarter.service.BankTransferProducer
import com.learning.KafkaStarter.service.BankTransferConsumer
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.time.Instant
import java.util.*
import kotlin.random.Random

@RestController
@RequestMapping("/api/manual-ack")
class ManualAckController {
    
    @Autowired
    private lateinit var bankTransferProducer: BankTransferProducer
    
    @Autowired
    private lateinit var bankTransferConsumer: BankTransferConsumer
    
    private val logger = org.slf4j.LoggerFactory.getLogger(ManualAckController::class.java)
    
    @PostMapping("/transfers")
    fun sendBankTransfer(@RequestBody bankTransferEvent: BankTransferEvent): ResponseEntity<Map<String, Any>> {
        return try {
            // Send bank transfer with manual acknowledgment processing
            bankTransferProducer.sendBankTransfer(bankTransferEvent, "bank-transfers")
            
            logger.info("Bank transfer sent for manual ack processing: transferId=${bankTransferEvent.transferId}")
            
            ResponseEntity.ok(
                mapOf(
                    "success" to true,
                    "message" to "Bank transfer sent for manual acknowledgment processing",
                    "transferId" to bankTransferEvent.transferId,
                    "amount" to bankTransferEvent.amount,
                    "fromAccount" to bankTransferEvent.fromAccount,
                    "toAccount" to bankTransferEvent.toAccount,
                    "topic" to "bank-transfers",
                    "timestamp" to System.currentTimeMillis()
                )
            )
        } catch (e: Exception) {
            logger.error("Failed to send bank transfer: transferId=${bankTransferEvent.transferId}", e)
            
            ResponseEntity.internalServerError().body(
                mapOf(
                    "success" to false,
                    "error" to (e.message ?: "Unknown error"),
                    "transferId" to bankTransferEvent.transferId,
                    "timestamp" to System.currentTimeMillis()
                )
            )
        }
    }
    
    @PostMapping("/transfers/idempotent")
    fun sendIdempotentBankTransfer(@RequestBody bankTransferEvent: BankTransferEvent): ResponseEntity<Map<String, Any>> {
        return try {
            // Send bank transfer with idempotent processing
            bankTransferProducer.sendIdempotentBankTransfer(bankTransferEvent)
            
            logger.info("Idempotent bank transfer sent: transferId=${bankTransferEvent.transferId}, " +
                "idempotencyKey=${bankTransferEvent.idempotencyKey}")
            
            ResponseEntity.ok(
                mapOf(
                    "success" to true,
                    "message" to "Idempotent bank transfer sent",
                    "transferId" to bankTransferEvent.transferId,
                    "idempotencyKey" to bankTransferEvent.idempotencyKey,
                    "amount" to bankTransferEvent.amount,
                    "topic" to "bank-transfers-idempotent",
                    "timestamp" to System.currentTimeMillis()
                )
            )
        } catch (e: Exception) {
            logger.error("Failed to send idempotent bank transfer: transferId=${bankTransferEvent.transferId}", e)
            
            ResponseEntity.internalServerError().body(
                mapOf(
                    "success" to false,
                    "error" to (e.message ?: "Unknown error"),
                    "transferId" to bankTransferEvent.transferId,
                    "timestamp" to System.currentTimeMillis()
                )
            )
        }
    }
    
    @PostMapping("/transfers/duplicate")
    fun sendDuplicateTransfers(
        @RequestBody bankTransferEvent: BankTransferEvent,
        @RequestParam(defaultValue = "3") count: Int
    ): ResponseEntity<Map<String, Any>> {
        return try {
            // Send duplicate transfers for testing idempotency
            bankTransferProducer.sendDuplicateTransfer(bankTransferEvent, count)
            
            logger.info("Duplicate transfers sent for idempotency testing: " +
                "originalTransferId=${bankTransferEvent.transferId}, " +
                "idempotencyKey=${bankTransferEvent.idempotencyKey}, count=$count")
            
            ResponseEntity.ok(
                mapOf(
                    "success" to true,
                    "message" to "Duplicate transfers sent for idempotency testing",
                    "originalTransferId" to bankTransferEvent.transferId,
                    "idempotencyKey" to bankTransferEvent.idempotencyKey,
                    "duplicateCount" to count,
                    "expectedBehavior" to "Only one transfer should be processed due to idempotency",
                    "timestamp" to System.currentTimeMillis()
                )
            )
        } catch (e: Exception) {
            logger.error("Failed to send duplicate transfers: transferId=${bankTransferEvent.transferId}", e)
            
            ResponseEntity.internalServerError().body(
                mapOf(
                    "success" to false,
                    "error" to (e.message ?: "Unknown error"),
                    "transferId" to bankTransferEvent.transferId,
                    "timestamp" to System.currentTimeMillis()
                )
            )
        }
    }
    
    @PostMapping("/transfers/generate")
    fun generateTransfers(
        @RequestParam(defaultValue = "5") count: Int,
        @RequestParam(defaultValue = "false") withFailures: Boolean,
        @RequestParam(defaultValue = "manual-ack") processingType: String
    ): ResponseEntity<Map<String, Any>> {
        return try {
            val accounts = listOf("ACC-001", "ACC-002", "ACC-003", "ACC-004", "ACC-005")
            val transferIds = mutableListOf<String>()
            
            repeat(count) { index ->
                val transferId = "transfer-${UUID.randomUUID()}"
                val fromAccount = accounts.random()
                var toAccount = accounts.random()
                
                // Ensure different from/to accounts
                while (toAccount == fromAccount) {
                    toAccount = accounts.random()
                }
                
                // Generate amount (with some potential failures if requested)
                val amount = if (withFailures && index % 3 == 0) {
                    if (Random.nextBoolean()) -100.0 else 15000.0 // Invalid or high-value
                } else {
                    Random.nextDouble(100.0, 5000.0)
                }
                
                // Generate bank transfer
                val bankTransfer = BankTransferEvent(
                    transferId = transferId,
                    fromAccount = if (withFailures && index % 4 == 0) "INVALID_$fromAccount" else fromAccount,
                    toAccount = toAccount,
                    amount = amount,
                    currency = "USD",
                    reference = "Generated transfer $index",
                    status = "PENDING",
                    timestamp = Instant.now().toEpochMilli(),
                    idempotencyKey = UUID.randomUUID().toString(),
                    metadata = mapOf(
                        "generated" to true,
                        "batchIndex" to index,
                        "batchSize" to count,
                        "withFailures" to withFailures
                    )
                )
                
                // Send based on processing type
                when (processingType) {
                    "manual-ack" -> bankTransferProducer.sendBankTransfer(bankTransfer, "bank-transfers")
                    "idempotent" -> bankTransferProducer.sendIdempotentBankTransfer(bankTransfer)
                    "exactly-once" -> bankTransferProducer.sendTransferWithExactlyOnceSemantics(bankTransfer)
                    else -> bankTransferProducer.sendBankTransfer(bankTransfer, "bank-transfers")
                }
                
                transferIds.add(transferId)
            }
            
            logger.info("Generated $count transfers with processingType=$processingType, withFailures=$withFailures")
            
            ResponseEntity.ok(
                mapOf(
                    "success" to true,
                    "message" to "Bank transfers generated successfully",
                    "count" to count,
                    "processingType" to processingType,
                    "withFailures" to withFailures,
                    "transferIds" to transferIds,
                    "timestamp" to System.currentTimeMillis()
                )
            )
        } catch (e: Exception) {
            logger.error("Failed to generate transfers: count=$count", e)
            
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
    
    @GetMapping("/stats")
    fun getProcessingStats(): ResponseEntity<Map<String, Any>> {
        return try {
            val stats = bankTransferConsumer.getProcessingStats()
            
            ResponseEntity.ok(
                mapOf(
                    "success" to true,
                    "statistics" to stats,
                    "processingTypes" to mapOf(
                        "manual-ack" to "Messages acknowledged only after successful processing",
                        "idempotent" to "Duplicate messages detected and skipped using idempotency keys",
                        "exactly-once" to "Combination of idempotent producer and consumer for exactly-once semantics"
                    ),
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
    
    @GetMapping("/transfers/{idempotencyKey}")
    fun getTransferResult(@PathVariable idempotencyKey: String): ResponseEntity<Map<String, Any>> {
        return try {
            val result = bankTransferConsumer.getTransferResult(idempotencyKey)
            
            if (result != null) {
                ResponseEntity.ok(
                    mapOf(
                        "success" to true,
                        "found" to true,
                        "result" to result,
                        "timestamp" to System.currentTimeMillis()
                    )
                )
            } else {
                ResponseEntity.ok(
                    mapOf(
                        "success" to true,
                        "found" to false,
                        "message" to "Transfer not found or not yet processed",
                        "idempotencyKey" to idempotencyKey,
                        "timestamp" to System.currentTimeMillis()
                    )
                )
            }
        } catch (e: Exception) {
            ResponseEntity.internalServerError().body(
                mapOf(
                    "success" to false,
                    "error" to (e.message ?: "Unknown error"),
                    "idempotencyKey" to idempotencyKey,
                    "timestamp" to System.currentTimeMillis()
                )
            )
        }
    }
    
    @GetMapping("/transfers/all")
    fun getAllProcessedTransfers(): ResponseEntity<Map<String, Any>> {
        return try {
            val allTransfers = bankTransferConsumer.getAllProcessedTransfers()
            
            ResponseEntity.ok(
                mapOf(
                    "success" to true,
                    "totalProcessedTransfers" to allTransfers.size,
                    "transfers" to allTransfers,
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
    
    @PostMapping("/reset")
    fun resetProcessingStats(): ResponseEntity<Map<String, Any>> {
        return try {
            bankTransferConsumer.resetCounters()
            
            ResponseEntity.ok(
                mapOf(
                    "success" to true,
                    "message" to "Processing statistics and transfer cache reset",
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
    
    @PostMapping("/test-scenarios")
    fun runTestScenarios(): ResponseEntity<Map<String, Any>> {
        return try {
            val results = mutableMapOf<String, Any>()
            
            // Scenario 1: Manual ACK with successful processing
            val successfulTransfer = BankTransferEvent(
                transferId = "test-success-${UUID.randomUUID()}",
                fromAccount = "ACC-001",
                toAccount = "ACC-002",
                amount = 500.0,
                reference = "Manual ACK success test",
                status = "PENDING",
                idempotencyKey = UUID.randomUUID().toString()
            )
            bankTransferProducer.sendBankTransfer(successfulTransfer)
            results["scenario1"] = "Manual ACK successful transfer sent"
            
            // Scenario 2: Manual ACK with failure (should not be acknowledged)
            val failingTransfer = BankTransferEvent(
                transferId = "test-fail-${UUID.randomUUID()}",
                fromAccount = "INVALID_ACC-001",
                toAccount = "ACC-002",
                amount = -100.0, // Invalid amount
                reference = "Manual ACK failure test",
                status = "PENDING",
                idempotencyKey = UUID.randomUUID().toString()
            )
            bankTransferProducer.sendBankTransfer(failingTransfer)
            results["scenario2"] = "Manual ACK failing transfer sent (should not be acknowledged)"
            
            // Scenario 3: Idempotent processing with duplicates
            val idempotencyKey = UUID.randomUUID().toString()
            val idempotentTransfer = BankTransferEvent(
                transferId = "test-idempotent-${UUID.randomUUID()}",
                fromAccount = "ACC-003",
                toAccount = "ACC-004",
                amount = 750.0,
                reference = "Idempotent test",
                status = "PENDING",
                idempotencyKey = idempotencyKey
            )
            bankTransferProducer.sendDuplicateTransfer(idempotentTransfer, 3)
            results["scenario3"] = "Idempotent transfer with 3 duplicates sent (only one should be processed)"
            
            // Scenario 4: Exactly-once semantics
            val exactlyOnceTransfer = BankTransferEvent(
                transferId = "test-exactly-once-${UUID.randomUUID()}",
                fromAccount = "ACC-004",
                toAccount = "ACC-005",
                amount = 1000.0,
                reference = "Exactly-once test",
                status = "PENDING",
                idempotencyKey = UUID.randomUUID().toString()
            )
            bankTransferProducer.sendTransferWithExactlyOnceSemantics(exactlyOnceTransfer)
            results["scenario4"] = "Exactly-once transfer sent"
            
            logger.info("All test scenarios executed successfully")
            
            ResponseEntity.ok(
                mapOf(
                    "success" to true,
                    "message" to "All test scenarios executed",
                    "scenarios" to results,
                    "instructions" to "Check processing stats after a few seconds to see the results",
                    "timestamp" to System.currentTimeMillis()
                )
            )
        } catch (e: Exception) {
            logger.error("Failed to run test scenarios", e)
            
            ResponseEntity.internalServerError().body(
                mapOf(
                    "success" to false,
                    "error" to (e.message ?: "Unknown error"),
                    "timestamp" to System.currentTimeMillis()
                )
            )
        }
    }
}