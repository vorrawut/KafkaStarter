package com.learning.KafkaStarter.service

import com.learning.KafkaStarter.model.BankTransferEvent
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.kafka.support.SendResult
import org.springframework.stereotype.Service
import java.util.concurrent.CompletableFuture

@Service
class BankTransferProducer {
    
    @Autowired
    private lateinit var kafkaTemplate: KafkaTemplate<String, BankTransferEvent>
    
    private val logger = org.slf4j.LoggerFactory.getLogger(BankTransferProducer::class.java)
    
    fun sendBankTransfer(bankTransferEvent: BankTransferEvent, topicName: String = "bank-transfers") {
        try {
            // Send bank transfer event with transfer ID as key for consistent partitioning
            val future: CompletableFuture<SendResult<String, BankTransferEvent>> = kafkaTemplate.send(
                topicName,
                bankTransferEvent.transferId, // Use transfer ID as key
                bankTransferEvent
            )
            
            future.whenComplete { result, throwable ->
                if (throwable == null) {
                    val metadata = result.recordMetadata
                    logger.info("Bank transfer sent successfully: transferId=${bankTransferEvent.transferId}, " +
                        "topic=$topicName, partition=${metadata.partition()}, offset=${metadata.offset()}")
                } else {
                    logger.error("Failed to send bank transfer: transferId=${bankTransferEvent.transferId}, " +
                        "topic=$topicName", throwable)
                }
            }
            
        } catch (e: Exception) {
            logger.error("Exception while sending bank transfer: transferId=${bankTransferEvent.transferId}, " +
                "topic=$topicName", e)
            throw RuntimeException("Failed to send bank transfer", e)
        }
    }
    
    fun sendIdempotentBankTransfer(bankTransferEvent: BankTransferEvent) {
        try {
            // Send bank transfer to idempotent processing topic
            // Use idempotency key for consistent partitioning and deduplication
            val future = kafkaTemplate.send(
                "bank-transfers-idempotent",
                bankTransferEvent.idempotencyKey, // Use idempotency key as partition key
                bankTransferEvent
            )
            
            future.whenComplete { result, throwable ->
                if (throwable == null) {
                    val metadata = result.recordMetadata
                    logger.info("Idempotent bank transfer sent: transferId=${bankTransferEvent.transferId}, " +
                        "idempotencyKey=${bankTransferEvent.idempotencyKey}, " +
                        "partition=${metadata.partition()}, offset=${metadata.offset()}")
                } else {
                    logger.error("Failed to send idempotent bank transfer: " +
                        "transferId=${bankTransferEvent.transferId}, " +
                        "idempotencyKey=${bankTransferEvent.idempotencyKey}", throwable)
                }
            }
            
        } catch (e: Exception) {
            logger.error("Exception while sending idempotent bank transfer: " +
                "transferId=${bankTransferEvent.transferId}", e)
            throw RuntimeException("Failed to send idempotent bank transfer", e)
        }
    }
    
    fun sendDuplicateTransfer(bankTransferEvent: BankTransferEvent, count: Int = 3) {
        try {
            // Send duplicate transfers for testing idempotency
            // All transfers will have the same idempotency key
            val originalIdempotencyKey = bankTransferEvent.idempotencyKey
            
            logger.info("Sending $count duplicate transfers with idempotencyKey=$originalIdempotencyKey")
            
            repeat(count) { index ->
                // Create a copy with the same idempotency key but potentially different transfer ID
                val duplicateTransfer = bankTransferEvent.copy(
                    transferId = "${bankTransferEvent.transferId}-duplicate-$index",
                    idempotencyKey = originalIdempotencyKey, // Keep same idempotency key
                    metadata = bankTransferEvent.metadata + mapOf(
                        "duplicateIndex" to index,
                        "totalDuplicates" to count,
                        "originalTransferId" to bankTransferEvent.transferId
                    )
                )
                
                sendIdempotentBankTransfer(duplicateTransfer)
                
                // Small delay between sends to simulate real-world duplicate scenarios
                Thread.sleep(100)
            }
            
            logger.info("Sent $count duplicate transfers for idempotency testing")
            
        } catch (e: Exception) {
            logger.error("Exception while sending duplicate transfers: " +
                "transferId=${bankTransferEvent.transferId}, count=$count", e)
            throw RuntimeException("Failed to send duplicate transfers", e)
        }
    }
    
    fun sendBatchTransfers(transfers: List<BankTransferEvent>, topicName: String = "bank-transfers") {
        if (transfers.isEmpty()) {
            logger.warn("Empty transfers list provided for batch send")
            return
        }
        
        try {
            val futures = mutableListOf<CompletableFuture<SendResult<String, BankTransferEvent>>>()
            
            // Send all transfers and collect futures
            transfers.forEach { transfer ->
                val future = kafkaTemplate.send(topicName, transfer.transferId, transfer)
                futures.add(future)
            }
            
            // Wait for all sends to complete
            val allFutures = CompletableFuture.allOf(*futures.toTypedArray())
            
            allFutures.whenComplete { _, throwable ->
                if (throwable == null) {
                    logger.info("Batch bank transfers sent successfully: count=${transfers.size}, topic=$topicName")
                    
                    // Log individual results
                    futures.forEachIndexed { index, future ->
                        try {
                            val result = future.get()
                            val metadata = result.recordMetadata
                            val transfer = transfers[index]
                            logger.debug("Batch transfer ${index + 1}/${transfers.size}: " +
                                "transferId=${transfer.transferId}, partition=${metadata.partition()}, " +
                                "offset=${metadata.offset()}")
                        } catch (e: Exception) {
                            logger.error("Failed to send batch transfer ${index + 1}/${transfers.size}: " +
                                "transferId=${transfers[index].transferId}", e)
                        }
                    }
                } else {
                    logger.error("Batch bank transfers send failed: topic=$topicName", throwable)
                }
            }
            
        } catch (e: Exception) {
            logger.error("Exception during batch transfers send: count=${transfers.size}, topic=$topicName", e)
            throw RuntimeException("Failed to send batch transfers", e)
        }
    }
    
    fun sendTransferWithExactlyOnceSemantics(bankTransferEvent: BankTransferEvent) {
        try {
            // Send with exactly-once semantics (idempotent producer + transactional)
            // Note: In a full transactional setup, you would also configure
            // transactional.id and use @Transactional annotations
            
            logger.info("Sending transfer with exactly-once semantics: " +
                "transferId=${bankTransferEvent.transferId}")
            
            sendIdempotentBankTransfer(bankTransferEvent)
            
        } catch (e: Exception) {
            logger.error("Exception while sending transfer with exactly-once semantics: " +
                "transferId=${bankTransferEvent.transferId}", e)
            throw RuntimeException("Failed to send transfer with exactly-once semantics", e)
        }
    }
}