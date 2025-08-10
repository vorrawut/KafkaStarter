# Concept

## Topics, Partitions & Offsets - Kafka's Storage Model

## 🎯 Learning Objectives

By the end of this lesson, you will:
- **Master** Kafka's storage architecture: topics, partitions, and offsets
- **Design** optimal topic structures for performance and scalability  
- **Implement** effective partitioning strategies for your use cases
- **Manage** consumer offsets for reliable message processing
- **Optimize** partition distribution and consumer group coordination
- **Monitor** topic health and partition performance

## 🏗️ Kafka Storage Architecture Deep Dive

Understanding Kafka's storage model is crucial for building scalable, performant applications:

```mermaid
graph TB
    subgraph "Kafka Cluster"
        subgraph "Topic: user-events (3 partitions)"
            subgraph "Partition 0 (Leader: Broker-1)"
                P0M1["Offset 0: {user: alice}"]
                P0M2["Offset 1: {user: bob}"]
                P0M3["Offset 2: {user: charlie}"]
                P0M4["Offset 3: {user: david}"]
            end
            
            subgraph "Partition 1 (Leader: Broker-2)"
                P1M1["Offset 0: {user: eve}"]
                P1M2["Offset 1: {user: frank}"]
                P1M3["Offset 2: {user: grace}"]
            end
            
            subgraph "Partition 2 (Leader: Broker-3)"
                P2M1["Offset 0: {user: henry}"]
                P2M2["Offset 1: {user: iris}"]
                P2M3["Offset 2: {user: jack}"]
                P2M4["Offset 3: {user: kate}"]
                P2M5["Offset 4: {user: leo}"]
            end
        end
    end
    
    subgraph "Producer Distribution"
        PROD[Producer]
        PART[Partitioner]
        
        PROD --> PART
        PART -->|"hash(alice) % 3 = 0"| P0M1
        PART -->|"hash(eve) % 3 = 1"| P1M1  
        PART -->|"hash(henry) % 3 = 2"| P2M1
    end
    
    style P0M4 fill:#ff6b6b
    style P1M3 fill:#4ecdc4
    style P2M5 fill:#a8e6cf
```

## 📚 Core Concepts

### **Topics**: Logical Event Categories
- **Named streams** of related events (e.g., `user-events`, `order-events`, `payment-events`)
- **Logical abstraction** over physical storage
- **Configurable retention** based on time, size, or compaction
- **Schema evolution** support through versioning

### **Partitions**: Physical Storage & Parallelism Units
- **Ordered, immutable** sequence of records within each partition
- **Parallel processing** - each partition can be consumed independently
- **Fault tolerance** - replicated across multiple brokers
- **Load distribution** - enables horizontal scaling

### **Offsets**: Message Position Tracking
- **Sequential, unique** identifiers within each partition
- **Monotonically increasing** - never decrease within a partition
- **Consumer progress** tracking mechanism
- **Replay capability** - consumers can reset and reprocess from any offset

## 🔄 Producer Partitioning Strategies

### 1. **Key-Based Partitioning (Default)**
```mermaid
flowchart TD
    subgraph "Messages with Keys"
        M1["Message: key=user-123 value={email john@example.com}"]
        M2["Message: key=user-456 value={email jane@example.com}"]
        M3["Message: key=user-789 value={email bob@example.com}"]
    end
    
    subgraph "Partitioning Process"
        HASH["Hash Function<br/>hash(key) % partition_count"]
    end
    
    subgraph "Target Partitions"
        P0["Partition 0 Always gets user-123 events"]
        P1["Partition 1 Always gets user-456 events"]
        P2["Partition 2 Always gets user-789 events"]
    end
    
    M1 --> HASH
    M2 --> HASH
    M3 --> HASH
    
    HASH -->|"hash(user-123) % 3 = 0"| P0
    HASH -->|"hash(user-456) % 3 = 1"| P1
    HASH -->|"hash(user-789) % 3 = 2"| P2
    
    style HASH fill:#ffe66d
    style P0 fill:#ff6b6b
    style P1 fill:#4ecdc4
    style P2 fill:#a8e6cf
```

**Benefits:**
- **Ordering guarantee** - all events for the same key go to the same partition
- **Stateful processing** - related events stay together
- **Partition affinity** - enables stateful stream processing

**Use Cases:**
- User activity tracking (key = userId)
- Order processing (key = orderId)
- Account transactions (key = accountId)

### 2. **Round-Robin Partitioning**
```mermaid
sequenceDiagram
    participant P as Producer
    participant P0 as Partition 0
    participant P1 as Partition 1
    participant P2 as Partition 2
    
    P->>P0: Message 1
    P->>P1: Message 2
    P->>P2: Message 3
    P->>P0: Message 4
    P->>P1: Message 5
    P->>P2: Message 6
    
    Note over P,P2: Even distribution, no ordering
```

**Benefits:**
- **Even distribution** across all partitions
- **Maximum throughput** - utilizes all partitions equally
- **Load balancing** - prevents hot partitions

**Use Cases:**
- Metrics and logging (order doesn't matter)
- Bulk data ingestion
- Analytics events

### 3. **Custom Partitioning**
```kotlin
class GeographicPartitioner : Partitioner {
    override fun partition(
        topic: String,
        key: Any?,
        keyBytes: ByteArray?,
        value: Any?,
        valueBytes: ByteArray?,
        cluster: Cluster
    ): Int {
        val userLocation = extractLocation(value)
        return when (userLocation.region) {
            "US" -> 0
            "EU" -> 1
            "ASIA" -> 2
            else -> 3
        }
    }
}
```

## 🎯 Consumer Groups & Partition Assignment

### Consumer Group Coordination
```mermaid
graph TB
    subgraph "Topic: order-events (6 partitions)"
        P0[Partition 0]
        P1[Partition 1] 
        P2[Partition 2]
        P3[Partition 3]
        P4[Partition 4]
        P5[Partition 5]
    end
    
    subgraph "Consumer Group: order-processors (3 consumers)"
        C1[Consumer 1<br/>Processes P0, P1]
        C2[Consumer 2<br/>Processes P2, P3]
        C3[Consumer 3<br/>Processes P4, P5]
    end
    
    subgraph "Consumer Group: analytics (2 consumers)"
        A1[Consumer 1<br/>Processes P0, P1, P2]
        A2[Consumer 2<br/>Processes P3, P4, P5]
    end
    
    P0 --> C1
    P1 --> C1
    P2 --> C2
    P3 --> C2
    P4 --> C3
    P5 --> C3
    
    P0 --> A1
    P1 --> A1
    P2 --> A1
    P3 --> A2
    P4 --> A2
    P5 --> A2
    
    style C1 fill:#ff6b6b
    style C2 fill:#4ecdc4
    style C3 fill:#a8e6cf
    style A1 fill:#ffe66d
    style A2 fill:#ffa8e6
```

### Partition Assignment Strategies

1. **Range Assignment** (Default)
   - Assigns continuous ranges of partitions to consumers
   - Good for ordered processing within consumer

2. **Round-Robin Assignment**
   - Distributes partitions evenly across consumers
   - Better load balancing for varying partition sizes

3. **Sticky Assignment**
   - Minimizes partition reassignment during rebalancing
   - Reduces state transfer in stateful applications

## 📊 Offset Management Strategies

### Automatic vs Manual Offset Management
```mermaid
sequenceDiagram
    participant C as Consumer
    participant K as Kafka Broker
    participant A as Application Logic
    participant DB as Database
    
    Note over C,DB: Auto-commit (Default)
    C->>K: Poll messages
    K-->>C: Batch [offset 100-105]
    C->>A: Process messages
    A->>DB: Store data
    Note over C: Auto-commit every 5 seconds
    C->>K: Commit offset 105
    
    Note over C,DB: Manual commit (Reliable)
    C->>K: Poll messages  
    K-->>C: Batch [offset 106-110]
    C->>A: Process messages
    A->>DB: Store data
    A-->>C: Success confirmation
    C->>K: Manual commit offset 110
```

### Offset Commit Patterns

#### 1. **Auto-Commit (Simple)**
```kotlin
@KafkaListener(topics = ["user-events"])
fun handleUserEvent(event: UserEvent) {
    // Process event
    processUserEvent(event)
    // Offset automatically committed every 5 seconds
}
```

#### 2. **Manual Sync Commit (Reliable)**
```kotlin
@KafkaListener(
    topics = ["user-events"],
    containerFactory = "manualCommitContainerFactory"
)
fun handleUserEventManual(
    event: UserEvent,
    acknowledgment: Acknowledgment
) {
    try {
        processUserEvent(event)
        acknowledgment.acknowledge() // Sync commit
    } catch (e: Exception) {
        // Don't commit - message will be redelivered
        logger.error("Processing failed for ${event.eventId}", e)
    }
}
```

#### 3. **Batch Commit (Performance)**
```kotlin
@KafkaListener(topics = ["user-events"])
fun handleUserEventBatch(events: List<UserEvent>) {
    events.forEach { event ->
        try {
            processUserEvent(event)
        } catch (e: Exception) {
            logger.error("Failed to process ${event.eventId}", e)
            // Handle individual failures
        }
    }
    // Commit after entire batch
}
```

## 🎛️ Topic Configuration & Management

### Essential Topic Configurations
```kotlin
val topicConfig = mapOf(
    // Retention settings
    TopicConfig.RETENTION_MS_CONFIG to "604800000", // 7 days
    TopicConfig.RETENTION_BYTES_CONFIG to "1073741824", // 1GB
    
    // Performance settings
    TopicConfig.COMPRESSION_TYPE_CONFIG to "lz4",
    TopicConfig.MIN_IN_SYNC_REPLICAS_CONFIG to "2",
    
    // Cleanup settings
    TopicConfig.CLEANUP_POLICY_CONFIG to "delete", // or "compact"
    TopicConfig.DELETE_RETENTION_MS_CONFIG to "86400000", // 1 day
    
    // Segment settings
    TopicConfig.SEGMENT_MS_CONFIG to "604800000", // 7 days
    TopicConfig.SEGMENT_BYTES_CONFIG to "1073741824" // 1GB
)
```

### Partition Sizing Guidelines

```mermaid
graph TB
    subgraph "Partition Count Decision Matrix"
        THROUGHPUT[Expected Throughput<br/>messages/second]
        CONSUMERS[Number of Consumers]
        RETENTION[Retention Period]
        SIZE[Message Size]
        
        THROUGHPUT --> CALC[Partition Count Calculator]
        CONSUMERS --> CALC
        RETENTION --> CALC
        SIZE --> CALC
        
        CALC --> RESULT[Recommended Partitions]
    end
    
    subgraph "Guidelines"
        G1[Low throughput + Few consumers = 1-3 partitions]
        G2[Medium throughput + Multiple consumers = 3-12 partitions]
        G3[High throughput + Many consumers = 12+ partitions]
        G4[Rule of thumb: 2-3x expected consumer count]
    end
    
    style CALC fill:#ffe66d
    style RESULT fill:#4ecdc4
```

**Best Practices:**
- **Start small**: Begin with 3-6 partitions
- **Plan for growth**: Consider 2-3x expected consumer count
- **Monitor performance**: Adjust based on throughput needs
- **Avoid over-partitioning**: Too many partitions increase overhead

## 📈 Performance Optimization

### Partition Distribution Monitoring
```mermaid
graph TB
    subgraph "Healthy Partition Distribution"
        HP0[Partition 0: 1000 messages]
        HP1[Partition 1: 980 messages]
        HP2[Partition 2: 1020 messages]
        BALANCED[✅ Balanced Load]
    end
    
    subgraph "Unhealthy Partition Distribution"
        UP0[Partition 0: 50 messages]
        UP1[Partition 1: 2000 messages]
        UP2[Partition 2: 100 messages]
        HOTSPOT[❌ Hot Partition]
    end
    
    HP0 --> BALANCED
    HP1 --> BALANCED
    HP2 --> BALANCED
    
    UP0 --> HOTSPOT
    UP1 --> HOTSPOT
    UP2 --> HOTSPOT
    
    style BALANCED fill:#4ecdc4
    style HOTSPOT fill:#ff6b6b
```

### Consumer Lag Monitoring
```kotlin
@Component
class ConsumerLagMonitor {
    
    fun checkConsumerLag(groupId: String): Map<String, Long> {
        AdminClient.create(adminConfig).use { admin ->
            val groupDescription = admin.describeConsumerGroups(listOf(groupId))
                .all().get()
            
            val memberAssignments = groupDescription[groupId]!!.members()
                .flatMap { it.assignment().topicPartitions() }
            
            val endOffsets = admin.listOffsets(
                memberAssignments.associateWith { 
                    OffsetSpec.latest() 
                }
            ).all().get()
            
            val groupOffsets = admin.listConsumerGroupOffsets(groupId)
                .partitionsToOffsetAndMetadata().get()
            
            return memberAssignments.associate { tp ->
                val endOffset = endOffsets[tp]?.offset() ?: 0
                val currentOffset = groupOffsets[tp]?.offset() ?: 0
                val lag = endOffset - currentOffset
                "${tp.topic()}-${tp.partition()}" to lag
            }
        }
    }
}
```

## 🛡️ Fault Tolerance & Recovery

### Partition Leadership & Replication
```mermaid
graph TB
    subgraph "Cluster: 3 Brokers"
        subgraph "Broker 1"
            B1P0L[Partition 0 Leader]
            B1P1F[Partition 1 Follower]
            B1P2F[Partition 2 Follower]
        end
        
        subgraph "Broker 2"
            B2P0F[Partition 0 Follower]
            B2P1L[Partition 1 Leader]
            B2P2F[Partition 2 Follower]
        end
        
        subgraph "Broker 3"
            B3P0F[Partition 0 Follower]
            B3P1F[Partition 1 Follower]
            B3P2L[Partition 2 Leader]
        end
    end
    
    subgraph "Producers & Consumers"
        PROD[Producers]
        CONS[Consumers]
    end
    
    PROD -->|Writes| B1P0L
    PROD -->|Writes| B2P1L
    PROD -->|Writes| B3P2L
    
    B1P0L -.->|Replication| B2P0F
    B1P0L -.->|Replication| B3P0F
    
    B1P0L -->|Reads| CONS
    B2P1L -->|Reads| CONS
    B3P2L -->|Reads| CONS
    
    style B1P0L fill:#ff6b6b
    style B2P1L fill:#ff6b6b
    style B3P2L fill:#ff6b6b
```

### Recovery Scenarios

#### **Broker Failure Recovery**
1. **Leader election** for affected partitions
2. **ISR (In-Sync Replica)** promotes to leader
3. **Producers/consumers** automatically reconnect
4. **No data loss** if `min.insync.replicas` configured properly

#### **Consumer Recovery**
1. **Group rebalancing** triggered
2. **Partitions reassigned** to remaining consumers
3. **Processing resumes** from last committed offset
4. **Duplicate processing** possible if manual commit used

## 🔍 Monitoring & Observability

### Key Metrics Dashboard
```mermaid
graph TB
    subgraph "Topic Metrics"
        TM1[Messages In Rate]
        TM2[Bytes In Rate]
        TM3[Message Size Avg]
        TM4[Error Rate]
    end
    
    subgraph "Partition Metrics"
        PM1[Partition Size]
        PM2[Leader Distribution]
        PM3[Under-replicated Partitions]
        PM4[Log End Offset]
    end
    
    subgraph "Consumer Metrics"
        CM1[Consumer Lag]
        CM2[Processing Rate]
        CM3[Commit Rate]
        CM4[Rebalance Rate]
    end
    
    TM1 --> ALERT1[High Throughput Alert]
    PM3 --> ALERT2[Replication Alert]
    CM1 --> ALERT3[Consumer Lag Alert]
    
    style ALERT1 fill:#ffe66d
    style ALERT2 fill:#ff6b6b
    style ALERT3 fill:#ffa8e6
```

### Health Check Implementation
```kotlin
@Service
class TopicHealthService {
    
    fun checkTopicHealth(topicName: String): TopicHealthReport {
        return AdminClient.create(adminConfig).use { admin ->
            val topicDescription = admin.describeTopics(listOf(topicName))
                .allTopicNames().get()[topicName]!!
            
            val issues = mutableListOf<String>()
            
            // Check partition health
            topicDescription.partitions().forEach { partition ->
                if (partition.leader() == null) {
                    issues.add("Partition ${partition.partition()} has no leader")
                }
                
                val inSyncReplicas = partition.isr().size
                val totalReplicas = partition.replicas().size
                if (inSyncReplicas < totalReplicas) {
                    issues.add("Partition ${partition.partition()} under-replicated")
                }
            }
            
            TopicHealthReport(
                topicName = topicName,
                partitionCount = topicDescription.partitions().size,
                isHealthy = issues.isEmpty(),
                issues = issues
            )
        }
    }
}
```

## ✅ Best Practices Summary

### Topic Design
- **Use descriptive names** with consistent naming conventions
- **Plan partition count** for future scale (start with 3-6, grow as needed)
- **Choose retention policies** based on business requirements
- **Group related events** in the same topic when possible

### Partitioning Strategy
- **Use meaningful keys** for related event ordering
- **Avoid hot partitions** through good key distribution
- **Monitor partition balance** and consumer lag regularly
- **Consider custom partitioners** for specific business logic

### Consumer Design
- **Design idempotent consumers** to handle duplicate messages
- **Use appropriate commit strategies** based on delivery guarantees needed
- **Monitor consumer lag** to ensure keeping up with production
- **Handle rebalancing gracefully** in consumer code

### Operations
- **Monitor key metrics** (throughput, lag, errors, partition health)
- **Set up alerting** for critical issues (under-replication, high lag)
- **Plan for failures** with proper replication and recovery procedures
- **Test disaster recovery** scenarios regularly

## 🚀 What's Next?

You've mastered Kafka's storage fundamentals! Now let's add structure to your events with schemas.

**Next**: [Lesson 5 - Schema Registry & Evolution](../lesson_5/concept.md) where you'll learn to manage data formats, ensure compatibility, and evolve your event schemas safely over time.

---

*"Understanding how Kafka stores and distributes your data is the key to building scalable, reliable systems. With topics, partitions, and offsets mastered, you're ready to add structure and evolution to your events!"*
