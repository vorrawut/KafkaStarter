package com.learning.KafkaStarter.controller

import com.learning.KafkaStarter.model.UserEvent
import org.apache.kafka.streams.KafkaStreams
import org.apache.kafka.streams.ThreadMetadata
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.ResponseEntity
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.web.bind.annotation.*
import java.util.*

@RestController
@RequestMapping("/api/streams")
class StreamsController {
    
    @Autowired
    private lateinit var kafkaTemplate: KafkaTemplate<String, Any>
    
    @Autowired
    private lateinit var kafkaStreams: KafkaStreams
    
    @PostMapping("/events")
    fun publishEvent(@RequestBody event: UserEvent): ResponseEntity<Map<String, Any>> {
        return try {
            // Generate event ID if not provided
            val eventId = event.data["eventId"] as? String ?: UUID.randomUUID().toString()
            
            // Publish event to source topic
            kafkaTemplate.send("user-events", eventId, event).get()
            
            ResponseEntity.ok(mapOf(
                "success" to true,
                "eventId" to eventId,
                "userId" to event.userId,
                "eventType" to event.eventType,
                "message" to "Event published successfully",
                "timestamp" to System.currentTimeMillis()
            ))
        } catch (e: Exception) {
            ResponseEntity.internalServerError().body(mapOf(
                "success" to false,
                "error" to (e.message ?: "Unknown error"),
                "timestamp" to System.currentTimeMillis()
            ))
        }
    }
    
    @PostMapping("/events/batch")
    fun publishBatchEvents(@RequestBody events: List<UserEvent>): ResponseEntity<Map<String, Any>> {
        return try {
            val publishedEvents = mutableListOf<String>()
            
            events.forEach { event ->
                val eventId = event.data["eventId"] as? String ?: UUID.randomUUID().toString()
                kafkaTemplate.send("user-events", eventId, event)
                publishedEvents.add(eventId)
            }
            
            ResponseEntity.ok(mapOf(
                "success" to true,
                "eventsPublished" to events.size,
                "eventIds" to publishedEvents,
                "message" to "Batch events published successfully",
                "timestamp" to System.currentTimeMillis()
            ))
        } catch (e: Exception) {
            ResponseEntity.internalServerError().body(mapOf(
                "success" to false,
                "error" to (e.message ?: "Unknown error"),
                "timestamp" to System.currentTimeMillis()
            ))
        }
    }
    
    @GetMapping("/status")
    fun getStreamsStatus(): ResponseEntity<Map<String, Any>> {
        return try {
            val localThreadsMetadata = kafkaStreams.localThreadsMetadata()
            
            val threadsInfo = localThreadsMetadata.map { thread ->
                mapOf(
                    "threadName" to thread.threadName(),
                    "threadState" to thread.threadState(),
                    "activeTasks" to thread.activeTasks().map { task ->
                        mapOf(
                            "taskId" to task.taskId().toString(),
                            "topicPartitions" to task.topicPartitions().map { 
                                "${it.topic()}-${it.partition()}" 
                            }
                        )
                    },
                    "standbyTasks" to thread.standbyTasks().map { task ->
                        mapOf(
                            "taskId" to task.taskId().toString(),
                            "topicPartitions" to task.topicPartitions().map { 
                                "${it.topic()}-${it.partition()}" 
                            }
                        )
                    }
                )
            }
            
            ResponseEntity.ok(mapOf(
                "state" to kafkaStreams.state().toString(),
                "isRunning" to (kafkaStreams.state() == KafkaStreams.State.RUNNING),
                "threadsCount" to localThreadsMetadata.size,
                "threadsMetadata" to threadsInfo,
                "applicationId" to getApplicationId(),
                "timestamp" to System.currentTimeMillis()
            ))
        } catch (e: Exception) {
            ResponseEntity.internalServerError().body(mapOf(
                "error" to (e.message ?: "Unknown error"),
                "timestamp" to System.currentTimeMillis()
            ))
        }
    }
    
    @GetMapping("/metrics")
    fun getStreamsMetrics(): ResponseEntity<Map<String, Any>> {
        return try {
            val metrics = kafkaStreams.metrics()
            
            // Extract key metrics
            val processRateMetrics = metrics.filter { 
                it.key.name() == "process-rate" 
            }.map { 
                it.key.tags()["thread-id"] to it.value.metricValue() 
            }.toMap()
            
            val processLatencyMetrics = metrics.filter { 
                it.key.name() == "process-latency-avg" 
            }.map { 
                it.key.tags()["thread-id"] to it.value.metricValue() 
            }.toMap()
            
            val commitRateMetric = metrics.find { 
                it.key.name() == "commit-rate" 
            }?.value?.metricValue()
            
            ResponseEntity.ok(mapOf(
                "totalMetrics" to metrics.size,
                "processRate" to processRateMetrics,
                "processLatency" to processLatencyMetrics,
                "commitRate" to commitRateMetric,
                "timestamp" to System.currentTimeMillis()
            ))
        } catch (e: Exception) {
            ResponseEntity.internalServerError().body(mapOf(
                "error" to (e.message ?: "Unknown error"),
                "timestamp" to System.currentTimeMillis()
            ))
        }
    }
    
    @PostMapping("/test-data")
    fun generateTestData(@RequestParam(defaultValue = "10") count: Int): ResponseEntity<Map<String, Any>> {
        return try {
            val eventTypes = listOf("LOGIN", "PAGE_VIEW", "CLICK", "PURCHASE", "LOGOUT")
            val userIds = (1..5).map { "user-$it" }
            val generatedEvents = mutableListOf<String>()
            
            repeat(count) {
                val event = UserEvent(
                    userId = userIds.random(),
                    eventType = eventTypes.random(),
                    timestamp = System.currentTimeMillis(),
                    data = mapOf(
                        "eventId" to UUID.randomUUID().toString(),
                        "userAgent" to "Mozilla/5.0 (Mobile; rv:40.0) Gecko/40.0 Firefox/40.0",
                        "ip" to "192.168.1.${(1..254).random()}",
                        "sessionId" to UUID.randomUUID().toString()
                    )
                )
                
                val eventId = event.data["eventId"] as String
                kafkaTemplate.send("user-events", eventId, event)
                generatedEvents.add(eventId)
            }
            
            ResponseEntity.ok(mapOf(
                "success" to true,
                "generatedEvents" to count,
                "eventIds" to generatedEvents,
                "message" to "Test data generated successfully"
            ))
        } catch (e: Exception) {
            ResponseEntity.internalServerError().body(mapOf(
                "success" to false,
                "error" to (e.message ?: "Unknown error")
            ))
        }
    }
    
    private fun getApplicationId(): String {
        return try {
            // Extract application ID from Kafka Streams configuration
            "lesson14-streams-app"
        } catch (e: Exception) {
            "unknown"
        }
    }
}