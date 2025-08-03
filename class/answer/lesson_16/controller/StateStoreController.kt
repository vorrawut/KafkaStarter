package com.learning.KafkaStarter.controller

import com.learning.KafkaStarter.model.UserProfile
import com.learning.KafkaStarter.service.StateStoreQueryService
import com.learning.KafkaStarter.service.FaultToleranceService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/state-store")
class StateStoreController {
    
    @Autowired
    private lateinit var queryService: StateStoreQueryService
    
    @Autowired
    private lateinit var faultToleranceService: FaultToleranceService
    
    @GetMapping("/users/{userId}")
    fun getUserProfile(@PathVariable userId: String): ResponseEntity<UserProfile> {
        val profile = queryService.getUserProfile(userId)
        return if (profile != null) {
            ResponseEntity.ok(profile)
        } else {
            ResponseEntity.notFound().build()
        }
    }
    
    @GetMapping("/users")
    fun getAllUserProfiles(
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "50") size: Int
    ): ResponseEntity<Map<String, Any>> {
        val allProfiles = queryService.getAllUserProfiles()
        val startIndex = page * size
        val endIndex = minOf(startIndex + size, allProfiles.size)
        
        val pagedProfiles = if (startIndex < allProfiles.size) {
            allProfiles.subList(startIndex, endIndex)
        } else {
            emptyList()
        }
        
        return ResponseEntity.ok(mapOf(
            "profiles" to pagedProfiles,
            "totalCount" to allProfiles.size,
            "page" to page,
            "size" to size,
            "hasNext" to (endIndex < allProfiles.size)
        ))
    }
    
    @GetMapping("/users/range")
    fun getUserProfilesInRange(
        @RequestParam startKey: String,
        @RequestParam endKey: String
    ): ResponseEntity<List<UserProfile>> {
        val profiles = queryService.getUserProfilesInRange(startKey, endKey)
        return ResponseEntity.ok(profiles)
    }
    
    @GetMapping("/users/top-by-events")
    fun getTopUsersByEventCount(
        @RequestParam(defaultValue = "10") limit: Int
    ): ResponseEntity<List<UserProfile>> {
        val topUsers = queryService.getTopUsersByEventCount(limit)
        return ResponseEntity.ok(topUsers)
    }
    
    @GetMapping("/users/top-by-value")
    fun getTopUsersByValue(
        @RequestParam(defaultValue = "10") limit: Int
    ): ResponseEntity<List<UserProfile>> {
        val topUsers = queryService.getTopUsersByValue(limit)
        return ResponseEntity.ok(topUsers)
    }
    
    @GetMapping("/health")
    fun getStateStoreHealth(): ResponseEntity<Map<String, Any>> {
        val health = queryService.getStateStoreHealth()
        return ResponseEntity.ok(health)
    }
    
    @PostMapping("/backup")
    fun createBackup(@RequestParam backupPath: String): ResponseEntity<Map<String, Any>> {
        return try {
            val metadata = faultToleranceService.performStateBackup(backupPath)
            ResponseEntity.ok(mapOf(
                "success" to true,
                "backupMetadata" to metadata,
                "message" to "Backup created successfully"
            ))
        } catch (e: Exception) {
            ResponseEntity.internalServerError().body(mapOf(
                "success" to false,
                "error" to (e.message ?: "Backup failed"),
                "timestamp" to System.currentTimeMillis()
            ))
        }
    }
    
    @PostMapping("/restore")
    fun restoreFromBackup(@RequestParam backupPath: String): ResponseEntity<Map<String, Any>> {
        return try {
            val success = faultToleranceService.restoreFromBackup(backupPath)
            ResponseEntity.ok(mapOf(
                "success" to success,
                "message" to if (success) "Restore completed successfully" else "Restore failed",
                "timestamp" to System.currentTimeMillis()
            ))
        } catch (e: Exception) {
            ResponseEntity.internalServerError().body(mapOf(
                "success" to false,
                "error" to (e.message ?: "Restore failed"),
                "timestamp" to System.currentTimeMillis()
            ))
        }
    }
    
    @GetMapping("/standby-replicas")
    fun getStandbyReplicaStatus(): ResponseEntity<Map<String, Any>> {
        val status = faultToleranceService.checkStandbyReplicas()
        return ResponseEntity.ok(status)
    }
    
    @GetMapping("/consistency")
    fun validateStateConsistency(): ResponseEntity<Map<String, Any>> {
        val consistency = faultToleranceService.validateStateConsistency()
        return ResponseEntity.ok(consistency)
    }
    
    @PostMapping("/simulate-failure")
    fun simulateFailure(): ResponseEntity<Map<String, Any>> {
        val simulation = faultToleranceService.simulateFailure()
        return ResponseEntity.ok(simulation)
    }
}