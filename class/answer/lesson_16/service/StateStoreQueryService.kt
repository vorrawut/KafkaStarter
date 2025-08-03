package com.learning.KafkaStarter.service

import com.learning.KafkaStarter.model.UserProfile
import org.apache.kafka.streams.KafkaStreams
import org.apache.kafka.streams.StoreQueryParameters
import org.apache.kafka.streams.errors.InvalidStateStoreException
import org.apache.kafka.streams.state.KeyValueIterator
import org.apache.kafka.streams.state.QueryableStoreTypes
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class StateStoreQueryService {
    
    @Autowired
    private lateinit var kafkaStreams: KafkaStreams
    
    fun getUserProfile(userId: String): UserProfile? {
        return try {
            // Get state store for user profiles
            val store = kafkaStreams.store(
                StoreQueryParameters.fromNameAndType(
                    "user-profiles",
                    QueryableStoreTypes.keyValueStore<String, UserProfile>()
                )
            )
            
            // Query the store for user profile
            store.get(userId)
        } catch (e: InvalidStateStoreException) {
            // Handle state store not available (rebalancing, etc.)
            null
        } catch (e: Exception) {
            // Handle other query exceptions
            throw RuntimeException("Failed to query user profile for $userId", e)
        }
    }
    
    fun getAllUserProfiles(): List<UserProfile> {
        return try {
            // Get all entries from state store
            val store = kafkaStreams.store(
                StoreQueryParameters.fromNameAndType(
                    "user-profiles",
                    QueryableStoreTypes.keyValueStore<String, UserProfile>()
                )
            )
            
            // Iterate through all entries
            val profiles = mutableListOf<UserProfile>()
            store.all().use { iterator ->
                while (iterator.hasNext()) {
                    profiles.add(iterator.next().value)
                }
            }
            
            profiles
        } catch (e: InvalidStateStoreException) {
            // Handle state store not available
            emptyList()
        } catch (e: Exception) {
            // Handle exceptions and return empty list
            throw RuntimeException("Failed to query all user profiles", e)
        }
    }
    
    fun getUserProfilesInRange(startKey: String, endKey: String): List<UserProfile> {
        return try {
            // Query state store with key range
            val store = kafkaStreams.store(
                StoreQueryParameters.fromNameAndType(
                    "user-profiles",
                    QueryableStoreTypes.keyValueStore<String, UserProfile>()
                )
            )
            
            // Use range query
            val rangeProfiles = mutableListOf<UserProfile>()
            store.range(startKey, endKey).use { iterator ->
                while (iterator.hasNext()) {
                    rangeProfiles.add(iterator.next().value)
                }
            }
            
            rangeProfiles
        } catch (e: InvalidStateStoreException) {
            // Handle state store not available
            emptyList()
        } catch (e: Exception) {
            // Handle range query exceptions
            throw RuntimeException("Failed to query user profiles in range $startKey to $endKey", e)
        }
    }
    
    fun getStateStoreHealth(): Map<String, Any> {
        return try {
            // Check state store availability and health
            val isStoreAvailable = try {
                kafkaStreams.store(
                    StoreQueryParameters.fromNameAndType(
                        "user-profiles",
                        QueryableStoreTypes.keyValueStore<String, UserProfile>()
                    )
                )
                true
            } catch (e: Exception) {
                false
            }
            
            // Get stream state
            val streamState = kafkaStreams.state()
            
            // Get thread metadata
            val threadsMetadata = try {
                kafkaStreams.localThreadsMetadata()
            } catch (e: Exception) {
                emptyList()
            }
            
            // Get approximate entry count if store is available
            val entryCount = if (isStoreAvailable) {
                try {
                    val store = kafkaStreams.store(
                        StoreQueryParameters.fromNameAndType(
                            "user-profiles",
                            QueryableStoreTypes.keyValueStore<String, UserProfile>()
                        )
                    )
                    store.approximateNumEntries()
                } catch (e: Exception) {
                    -1L
                }
            } else {
                -1L
            }
            
            mapOf(
                "storeAvailable" to isStoreAvailable,
                "streamState" to streamState.toString(),
                "isRunning" to (streamState == KafkaStreams.State.RUNNING),
                "entryCount" to entryCount,
                "threadsCount" to threadsMetadata.size,
                "activeTasksCount" to threadsMetadata.sumOf { it.activeTasks().size },
                "standbyTasksCount" to threadsMetadata.sumOf { it.standbyTasks().size },
                "lastChecked" to System.currentTimeMillis()
            )
        } catch (e: Exception) {
            // Return health status with error information
            mapOf(
                "storeAvailable" to false,
                "error" to (e.message ?: "Unknown error"),
                "lastChecked" to System.currentTimeMillis()
            )
        }
    }
    
    fun getTopUsersByEventCount(limit: Int = 10): List<UserProfile> {
        return try {
            getAllUserProfiles()
                .sortedByDescending { it.eventCount }
                .take(limit)
        } catch (e: Exception) {
            emptyList()
        }
    }
    
    fun getTopUsersByValue(limit: Int = 10): List<UserProfile> {
        return try {
            getAllUserProfiles()
                .sortedByDescending { it.totalValue }
                .take(limit)
        } catch (e: Exception) {
            emptyList()
        }
    }
}