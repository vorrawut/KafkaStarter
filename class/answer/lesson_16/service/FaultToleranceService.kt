package com.learning.KafkaStarter.service

import com.learning.KafkaStarter.model.BackupMetadata
import org.apache.kafka.streams.KafkaStreams
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.io.File
import java.time.Duration
import java.time.Instant
import java.util.*

@Service
class FaultToleranceService {
    
    @Autowired
    private lateinit var kafkaStreams: KafkaStreams
    
    fun performStateBackup(backupPath: String): BackupMetadata {
        return try {
            val backupId = UUID.randomUUID().toString()
            val timestamp = Instant.now().toEpochMilli()
            
            // Get current state directory
            val stateDir = File("/tmp/kafka-streams/lesson16")
            val backupDir = File(backupPath, backupId)
            
            // Create backup directory
            backupDir.mkdirs()
            
            // List of state stores to backup
            val storeNames = listOf("user-profiles", "user-profiles-timestamped")
            
            // Backup state files
            var totalSize = 0L
            storeNames.forEach { storeName ->
                val storeDir = File(stateDir, storeName)
                if (storeDir.exists()) {
                    val backupStoreDir = File(backupDir, storeName)
                    storeDir.copyRecursively(backupStoreDir, overwrite = true)
                    totalSize += calculateDirectorySize(backupStoreDir)
                }
            }
            
            // Create backup metadata
            val metadata = BackupMetadata(
                backupId = backupId,
                timestamp = timestamp,
                storeNames = storeNames.filter { File(stateDir, it).exists() },
                backupSize = totalSize
            )
            
            // Save metadata
            val metadataFile = File(backupDir, "backup-metadata.json")
            metadataFile.writeText(
                com.fasterxml.jackson.module.kotlin.jacksonObjectMapper().writeValueAsString(metadata)
            )
            
            metadata
        } catch (e: Exception) {
            throw RuntimeException("Backup failed: ${e.message}", e)
        }
    }
    
    fun restoreFromBackup(backupPath: String): Boolean {
        return try {
            val backupDir = File(backupPath)
            if (!backupDir.exists()) {
                throw IllegalArgumentException("Backup directory does not exist: $backupPath")
            }
            
            // Read backup metadata
            val metadataFile = File(backupDir, "backup-metadata.json")
            if (!metadataFile.exists()) {
                throw IllegalArgumentException("Backup metadata not found")
            }
            
            val metadata = com.fasterxml.jackson.module.kotlin.jacksonObjectMapper()
                .readValue(metadataFile, BackupMetadata::class.java)
            
            // Stop streams if running (in production, this should be coordinated)
            val wasRunning = kafkaStreams.state() == KafkaStreams.State.RUNNING
            if (wasRunning) {
                kafkaStreams.close(Duration.ofSeconds(30))
            }
            
            // Restore state directories
            val stateDir = File("/tmp/kafka-streams/lesson16")
            metadata.storeNames.forEach { storeName ->
                val backupStoreDir = File(backupDir, storeName)
                val stateStoreDir = File(stateDir, storeName)
                
                if (backupStoreDir.exists()) {
                    // Remove existing state
                    if (stateStoreDir.exists()) {
                        stateStoreDir.deleteRecursively()
                    }
                    
                    // Restore from backup
                    backupStoreDir.copyRecursively(stateStoreDir)
                }
            }
            
            // Note: In a real implementation, you would restart the streams application here
            // For this example, we'll just return success
            true
        } catch (e: Exception) {
            throw RuntimeException("Restore failed: ${e.message}", e)
        }
    }
    
    fun checkStandbyReplicas(): Map<String, Any> {
        return try {
            val localThreadsMetadata = kafkaStreams.localThreadsMetadata()
            
            val standbyInfo = localThreadsMetadata.flatMap { thread ->
                thread.standbyTasks().map { task ->
                    mapOf(
                        "taskId" to task.taskId().toString(),
                        "topicPartitions" to task.topicPartitions().map { "${it.topic()}-${it.partition()}" },
                        "threadName" to thread.threadName()
                    )
                }
            }
            
            val activeInfo = localThreadsMetadata.flatMap { thread ->
                thread.activeTasks().map { task ->
                    mapOf(
                        "taskId" to task.taskId().toString(),
                        "topicPartitions" to task.topicPartitions().map { "${it.topic()}-${it.partition()}" },
                        "threadName" to thread.threadName()
                    )
                }
            }
            
            mapOf(
                "standbyTasks" to standbyInfo,
                "activeTasks" to activeInfo,
                "standbyTaskCount" to standbyInfo.size,
                "activeTaskCount" to activeInfo.size,
                "totalThreads" to localThreadsMetadata.size,
                "streamState" to kafkaStreams.state().toString(),
                "checkTimestamp" to System.currentTimeMillis()
            )
        } catch (e: Exception) {
            mapOf(
                "error" to (e.message ?: "Unknown error"),
                "checkTimestamp" to System.currentTimeMillis()
            )
        }
    }
    
    fun validateStateConsistency(): Map<String, Any> {
        return try {
            val storeNames = listOf("user-profiles", "user-profiles-timestamped")
            val consistencyResults = mutableMapOf<String, Any>()
            
            storeNames.forEach { storeName ->
                try {
                    val store = kafkaStreams.store(
                        org.apache.kafka.streams.StoreQueryParameters.fromNameAndType(
                            storeName,
                            org.apache.kafka.streams.state.QueryableStoreTypes.keyValueStore<String, Any>()
                        )
                    )
                    
                    val entryCount = store.approximateNumEntries()
                    val isAccessible = true
                    
                    consistencyResults[storeName] = mapOf(
                        "accessible" to isAccessible,
                        "entryCount" to entryCount,
                        "status" to "HEALTHY"
                    )
                } catch (e: Exception) {
                    consistencyResults[storeName] = mapOf(
                        "accessible" to false,
                        "error" to (e.message ?: "Unknown error"),
                        "status" to "UNHEALTHY"
                    )
                }
            }
            
            val overallHealthy = consistencyResults.values.all { 
                (it as Map<*, *>)["accessible"] == true 
            }
            
            mapOf(
                "overallHealth" to if (overallHealthy) "HEALTHY" else "UNHEALTHY",
                "storeHealth" to consistencyResults,
                "streamState" to kafkaStreams.state().toString(),
                "validationTimestamp" to System.currentTimeMillis()
            )
        } catch (e: Exception) {
            mapOf(
                "overallHealth" to "ERROR",
                "error" to (e.message ?: "Unknown error"),
                "validationTimestamp" to System.currentTimeMillis()
            )
        }
    }
    
    fun simulateFailure(): Map<String, Any> {
        return try {
            // Simulate various failure scenarios for testing
            val failureType = listOf("NETWORK_PARTITION", "DISK_FULL", "MEMORY_PRESSURE").random()
            
            when (failureType) {
                "NETWORK_PARTITION" -> {
                    // Simulate network issues by temporarily making queries fail
                    mapOf(
                        "failureType" to failureType,
                        "description" to "Simulated network partition - queries may fail temporarily",
                        "impact" to "State store queries unavailable",
                        "recovery" to "Automatic recovery when network is restored"
                    )
                }
                "DISK_FULL" -> {
                    // Simulate disk space issues
                    mapOf(
                        "failureType" to failureType,
                        "description" to "Simulated disk full condition",
                        "impact" to "State store writes may fail",
                        "recovery" to "Free up disk space or enable compaction"
                    )
                }
                "MEMORY_PRESSURE" -> {
                    // Simulate memory pressure
                    mapOf(
                        "failureType" to failureType,
                        "description" to "Simulated memory pressure - cache eviction",
                        "impact" to "Increased latency due to cache misses",
                        "recovery" to "Increase heap size or reduce cache size"
                    )
                }
                else -> {
                    mapOf(
                        "failureType" to "UNKNOWN",
                        "description" to "Unknown failure type"
                    )
                }
            }
        } catch (e: Exception) {
            mapOf(
                "error" to "Failed to simulate failure: ${e.message}",
                "timestamp" to System.currentTimeMillis()
            )
        }
    }
    
    private fun calculateDirectorySize(directory: File): Long {
        var size = 0L
        directory.walkTopDown().forEach { file ->
            if (file.isFile) {
                size += file.length()
            }
        }
        return size
    }
}