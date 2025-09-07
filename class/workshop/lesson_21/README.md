# Workshop: Deployment & Scaling Best Practices

## 🎯 Objective  
Master production deployment strategies, auto-scaling patterns, and operational best practices for running Kafka clusters at scale in cloud and on-premises environments.

## 📋 Workshop Tasks

### Task 1: Deployment Strategies
Implement deployment automation in `deployment/DeploymentManager.kt`

### Task 2: Auto-scaling Configuration  
Build auto-scaling logic in `scaling/AutoScaler.kt`

### Task 3: Performance Tuning
Optimize configurations in `tuning/PerformanceTuner.kt`

### Task 4: Operational Runbooks
Create operational procedures in `operations/RunbookManager.kt`

### Task 5: Disaster Recovery
Implement DR procedures in `disaster/DisasterRecoveryManager.kt`

## 🏗️ Production Deployment Architecture
```mermaid
graph TB
    subgraph "Multi-Environment Pipeline"
        DEV[Development<br/>Single node, fast iteration]
        STAGE[Staging<br/>Production-like, testing]
        PROD[Production<br/>Multi-AZ, HA setup]
        
        DEV -->|CI/CD| STAGE
        STAGE -->|Validation| PROD
    end
    
    subgraph "Production Kafka Cluster"
        subgraph "Availability Zone A"
            ZK1[Zookeeper 1]
            B1[Broker 1]
            B4[Broker 4]
        end
        
        subgraph "Availability Zone B"
            ZK2[Zookeeper 2]
            B2[Broker 2]
            B5[Broker 5]
        end
        
        subgraph "Availability Zone C"
            ZK3[Zookeeper 3]
            B3[Broker 3]
            B6[Broker 6]
        end
    end
    
    subgraph "Load Balancers & Proxies"
        LB[Load Balancer<br/>Health checks, SSL termination]
        PROXY[Kafka Proxy<br/>Connection pooling, routing]
    end
    
    subgraph "Monitoring & Observability"
        METRICS[Metrics Collection<br/>Prometheus, JMX]
        LOGS[Log Aggregation<br/>ELK Stack]
        ALERTS[Alerting<br/>PagerDuty, Slack]
        DASH[Dashboards<br/>Grafana, Kibana]
    end
    
    subgraph "Auto-scaling"
        HPA[Horizontal Pod Autoscaler]
        VPA[Vertical Pod Autoscaler]
        CLUSTER[Cluster Autoscaler]
    end
    
    LB --> PROXY
    PROXY --> B1
    PROXY --> B2
    PROXY --> B3
    
    B1 --> METRICS
    B2 --> METRICS
    B3 --> METRICS
    
    METRICS --> ALERTS
    METRICS --> DASH
    LOGS --> DASH
    
    METRICS --> HPA
    METRICS --> VPA
    HPA --> CLUSTER
    
    style PROD fill:#4ecdc4
    style LB fill:#ff6b6b
    style ALERTS fill:#ffe66d
```

## 🚀 Deployment Strategies

### 1. **Blue-Green Deployment**
```mermaid
sequenceDiagram
    participant LB as Load Balancer
    participant Blue as Blue Cluster (Current)
    participant Green as Green Cluster (New)
    participant Clients as Client Applications
    
    Note over Blue,Green: Initial State - Blue Active
    LB->>Blue: Route 100% traffic
    
    Note over Blue,Green: Deploy to Green
    Green->>Green: Deploy new version
    Green->>Green: Run validation tests
    
    Note over Blue,Green: Gradual Traffic Shift
    LB->>Blue: Route 90% traffic
    LB->>Green: Route 10% traffic
    
    LB->>Blue: Route 50% traffic
    LB->>Green: Route 50% traffic
    
    LB->>Blue: Route 0% traffic
    LB->>Green: Route 100% traffic
    
    Note over Blue,Green: Blue becomes standby
    Blue->>Blue: Scale down or keep as rollback
```

### 2. **Rolling Deployment**
```mermaid
graph TB
    subgraph "Rolling Update Process"
        START[Start Rolling Update] --> N1[Update Node 1]
        N1 --> V1[Validate Node 1]
        V1 --> N2[Update Node 2]
        N2 --> V2[Validate Node 2]
        V2 --> N3[Update Node 3]
        N3 --> V3[Validate Node 3]
        V3 --> COMPLETE[Update Complete]
        
        V1 -->|Failure| ROLLBACK1[Rollback Node 1]
        V2 -->|Failure| ROLLBACK2[Rollback Nodes 1-2]
        V3 -->|Failure| ROLLBACK3[Rollback All Nodes]
    end
    
    style COMPLETE fill:#4ecdc4
    style ROLLBACK1 fill:#ff6b6b
    style ROLLBACK2 fill:#ff6b6b
    style ROLLBACK3 fill:#ff6b6b
```

### 3. **Canary Deployment**
```mermaid
graph TB
    subgraph "Canary Deployment Flow"
        PROD[Production 100%] --> CANARY[Deploy to Canary 5%]
        CANARY --> MONITOR[Monitor Metrics]
        
        MONITOR -->|Success| EXPAND1[Expand to 25%]
        MONITOR -->|Failure| ROLLBACK[Rollback Canary]
        
        EXPAND1 --> MONITOR2[Monitor Extended]
        MONITOR2 -->|Success| EXPAND2[Expand to 100%]
        MONITOR2 -->|Failure| ROLLBACK
        
        EXPAND2 --> COMPLETE[Full Deployment]
    end
    
    style COMPLETE fill:#4ecdc4
    style ROLLBACK fill:#ff6b6b
```

## ⚖️ Auto-scaling Strategies

### Horizontal Scaling Metrics
```mermaid
graph TB
    subgraph "Scaling Triggers"
        CPU[CPU Usage > 70%]
        MEMORY[Memory Usage > 80%]
        LAG[Consumer Lag > 1000]
        THROUGHPUT[Throughput > 80% capacity]
        CONNECTIONS[Connection count > 500]
    end
    
    subgraph "Scaling Actions"
        SCALE_OUT[Scale Out<br/>Add brokers/consumers]
        SCALE_UP[Scale Up<br/>Increase resources]
        SCALE_IN[Scale In<br/>Remove brokers/consumers]
        SCALE_DOWN[Scale Down<br/>Reduce resources]
    end
    
    subgraph "Safety Checks"
        MIN_REPLICAS[Minimum Replicas]
        MAX_REPLICAS[Maximum Replicas]
        COOLDOWN[Cooldown Period]
        HEALTH_CHECK[Health Validation]
    end
    
    CPU --> SCALE_OUT
    MEMORY --> SCALE_UP
    LAG --> SCALE_OUT
    THROUGHPUT --> SCALE_OUT
    
    SCALE_OUT --> MIN_REPLICAS
    SCALE_UP --> MAX_REPLICAS
    SCALE_IN --> COOLDOWN
    SCALE_DOWN --> HEALTH_CHECK
    
    style SCALE_OUT fill:#4ecdc4
    style SCALE_UP fill:#4ecdc4
    style SCALE_IN fill:#ffe66d
    style SCALE_DOWN fill:#ffe66d
```

### Consumer Auto-scaling
```kotlin
@Component
class KafkaConsumerAutoScaler {
    
    fun evaluateScaling(groupId: String): ScalingDecision {
        val metrics = consumerGroupMetrics.getMetrics(groupId)
        val avgLag = metrics.partitions.map { it.lag }.average()
        val processingRate = metrics.processingRate
        val currentInstances = metrics.activeConsumers
        
        return when {
            avgLag > 1000 && currentInstances < maxInstances -> {
                ScalingDecision.SCALE_OUT(
                    targetInstances = minOf(currentInstances + 1, maxInstances),
                    reason = "High consumer lag detected: $avgLag"
                )
            }
            avgLag < 100 && currentInstances > minInstances -> {
                ScalingDecision.SCALE_IN(
                    targetInstances = maxOf(currentInstances - 1, minInstances),
                    reason = "Low consumer lag: $avgLag"
                )
            }
            processingRate < 0.5 && currentInstances > minInstances -> {
                ScalingDecision.SCALE_IN(
                    targetInstances = maxOf(currentInstances - 1, minInstances),
                    reason = "Low processing rate: $processingRate"
                )
            }
            else -> ScalingDecision.NO_ACTION("Metrics within acceptable range")
        }
    }
}
```

## 🔧 Performance Tuning

### Broker Configuration Optimization
```mermaid
graph TB
    subgraph "Memory Tuning"
        HEAP[JVM Heap Size<br/&gt;6-8GB recommended]
        PAGE_CACHE[OS Page Cache<br/&gt;50%+ of system RAM]
        SOCKET_BUFFER[Socket Buffers<br/&gt;128KB-1MB]
    end
    
    subgraph "Disk Tuning"
        LOG_DIRS[Multiple Log Directories<br/>Separate disks]
        LOG_SEGMENT[Log Segment Size<br/&gt;1GB default]
        LOG_RETENTION[Log Retention<br/>Time vs Size based]
    end
    
    subgraph "Network Tuning"
        NUM_NETWORK[Network Threads<br/&gt;3-8 threads]
        NUM_IO[I/O Threads<br/&gt;8-16 threads]
        REPLICA_FETCH[Replica Fetch Size<br/&gt;1MB default]
    end
    
    subgraph "Producer Tuning"
        BATCH_SIZE[Batch Size<br/&gt;16KB-1MB]
        LINGER_MS[Linger Time<br/&gt;5-100ms]
        COMPRESSION[Compression<br/>LZ4, Snappy, GZIP]
    end
    
    style HEAP fill:#ff6b6b
    style PAGE_CACHE fill:#4ecdc4
    style LOG_DIRS fill:#a8e6cf
    style BATCH_SIZE fill:#ffe66d
```

### Performance Tuning Matrix
```kotlin
data class PerformanceTuningConfig(
    // Memory Configuration
    val heapSize: String = "6g",
    val pageCacheSize: String = "auto", // 50% of system RAM
    
    // Thread Configuration  
    val networkThreads: Int = 3,
    val ioThreads: Int = 8,
    val backgroundThreads: Int = 10,
    
    // Log Configuration
    val logSegmentBytes: Long = 1073741824, // 1GB
    val logRetentionHours: Int = 168, // 7 days
    val logRollMs: Long = 604800000, // 7 days
    
    // Replication Configuration
    val replicaFetchMaxBytes: Int = 1048576, // 1MB
    val replicaFetchWaitMaxMs: Int = 500,
    val replicaLagTimeMaxMs: Long = 30000,
    
    // Producer Optimization
    val batchSize: Int = 65536, // 64KB
    val lingerMs: Int = 5,
    val compressionType: String = "lz4",
    val acks: String = "all",
    
    // Consumer Optimization
    val fetchMinBytes: Int = 1024, // 1KB
    val fetchMaxWaitMs: Int = 500,
    val maxPollRecords: Int = 500,
    val maxPartitionFetchBytes: Int = 1048576 // 1MB
) {
    
    fun generateBrokerConfig(): Properties {
        return Properties().apply {
            // Memory settings
            setProperty("heap.opts", "-Xmx$heapSize -Xms$heapSize")
            
            // Thread settings
            setProperty("num.network.threads", networkThreads.toString())
            setProperty("num.io.threads", ioThreads.toString())
            setProperty("background.threads", backgroundThreads.toString())
            
            // Log settings
            setProperty("log.segment.bytes", logSegmentBytes.toString())
            setProperty("log.retention.hours", logRetentionHours.toString())
            setProperty("log.roll.ms", logRollMs.toString())
            
            // Replication settings
            setProperty("replica.fetch.max.bytes", replicaFetchMaxBytes.toString())
            setProperty("replica.fetch.wait.max.ms", replicaFetchWaitMaxMs.toString())
            setProperty("replica.lag.time.max.ms", replicaLagTimeMaxMs.toString())
        }
    }
}
```

## ✅ Success Criteria
- [ ] Multi-environment deployment pipeline working
- [ ] Auto-scaling responds correctly to load changes
- [ ] Performance tuning achieves target throughput/latency
- [ ] Blue-green deployment tested and documented
- [ ] Disaster recovery procedures tested and validated
- [ ] Monitoring and alerting covers all critical metrics
- [ ] Operational runbooks complete and accessible

## 🚀 Getting Started

### 1. Infrastructure as Code
```yaml
# Terraform example for AWS
resource "aws_msk_cluster" "kafka_cluster" {
  cluster_name           = "kafka-production"
  kafka_version         = "2.8.1"
  number_of_broker_nodes = 6
  
  broker_node_group_info {
    instance_type   = "kafka.m5.xlarge"
    ebs_volume_size = 1000
    client_subnets = [
      aws_subnet.private_a.id,
      aws_subnet.private_b.id,
      aws_subnet.private_c.id,
    ]
    security_groups = [aws_security_group.kafka.id]
  }
  
  encryption_info {
    encryption_at_rest_kms_key_id = aws_kms_key.kafka.arn
    encryption_in_transit {
      client_broker = "TLS"
      in_cluster    = true
    }
  }
  
  configuration_info {
    arn      = aws_msk_configuration.kafka_config.arn
    revision = aws_msk_configuration.kafka_config.latest_revision
  }
  
  logging_info {
    broker_logs {
      cloudwatch_logs {
        enabled   = true
        log_group = aws_cloudwatch_log_group.kafka.name
      }
      s3 {
        enabled = true
        bucket  = aws_s3_bucket.kafka_logs.id
        prefix  = "kafka-broker-logs"
      }
    }
  }
  
  tags = {
    Environment = "production"
    Team        = "platform"
  }
}
```

### 2. Kubernetes Deployment
```yaml
apiVersion: apps/v1
kind: StatefulSet
metadata:
  name: kafka
spec:
  serviceName: kafka-headless
  replicas: 6
  selector:
    matchLabels:
      app: kafka
  template:
    metadata:
      labels:
        app: kafka
    spec:
      affinity:
        podAntiAffinity:
          requiredDuringSchedulingIgnoredDuringExecution:
          - labelSelector:
              matchLabels:
                app: kafka
            topologyKey: kubernetes.io/hostname
      containers:
      - name: kafka
        image: confluentinc/cp-kafka:7.4.0
        ports:
        - containerPort: 9092
        env:
        - name: KAFKA_BROKER_ID
          valueFrom:
            fieldRef:
              fieldPath: metadata.name
        - name: KAFKA_ZOOKEEPER_CONNECT
          value: "zookeeper:2181"
        - name: KAFKA_ADVERTISED_LISTENERS
          value: "PLAINTEXT://$(POD_IP):9092"
        resources:
          requests:
            memory: "4Gi"
            cpu: "1"
          limits:
            memory: "8Gi"
            cpu: "2"
        volumeMounts:
        - name: kafka-storage
          mountPath: /var/lib/kafka/data
  volumeClaimTemplates:
  - metadata:
      name: kafka-storage
    spec:
      accessModes: ["ReadWriteOnce"]
      resources:
        requests:
          storage: 1000Gi
      storageClassName: fast-ssd
```

### 3. Monitoring Setup
```yaml
# Prometheus monitoring
apiVersion: v1
kind: ServiceMonitor
metadata:
  name: kafka-metrics
spec:
  selector:
    matchLabels:
      app: kafka
  endpoints:
  - port: metrics
    interval: 30s
    path: /metrics
    
---
apiVersion: monitoring.coreos.com/v1
kind: PrometheusRule
metadata:
  name: kafka-alerts
spec:
  groups:
  - name: kafka
    rules:
    - alert: KafkaBrokerDown
      expr: up{job="kafka"} == 0
      for: 1m
      labels:
        severity: critical
      annotations:
        summary: "Kafka broker is down"
        
    - alert: KafkaConsumerLag
      expr: kafka_consumer_lag_sum > 1000
      for: 5m
      labels:
        severity: warning
      annotations:
        summary: "High consumer lag detected"
        
    - alert: KafkaDiskUsage
      expr: kafka_log_size_bytes / kafka_log_size_limit_bytes > 0.85
      for: 5m
      labels:
        severity: warning
      annotations:
        summary: "Kafka disk usage is high"
```

## 💾 Disaster Recovery

### Backup Strategies
```mermaid
graph TB
    subgraph "Backup Types"
        CONFIG[Configuration Backup<br/>Topics, ACLs, Settings]
        DATA[Data Backup<br/>Log segments, Offsets]
        METADATA[Metadata Backup<br/>Zookeeper state]
    end
    
    subgraph "Backup Frequency"
        REALTIME[Real-time<br/>Cross-region replication]
        HOURLY[Hourly<br/>Incremental backups]
        DAILY[Daily<br/>Full backups]
        WEEKLY[Weekly<br/>Archive backups]
    end
    
    subgraph "Recovery Scenarios"
        BROKER_FAIL[Broker Failure<br/>Replace & sync]
        CLUSTER_FAIL[Cluster Failure<br/>Restore from backup]
        REGION_FAIL[Region Failure<br/>Failover to DR region]
        DATA_CORRUPT[Data Corruption<br/>Point-in-time recovery]
    end
    
    CONFIG --> DAILY
    DATA --> REALTIME
    METADATA --> HOURLY
    
    REALTIME --> REGION_FAIL
    DAILY --> CLUSTER_FAIL
    HOURLY --> DATA_CORRUPT
    
    style REGION_FAIL fill:#ff6b6b
    style CLUSTER_FAIL fill:#ffe66d
    style DATA_CORRUPT fill:#a8e6cf
```

### Recovery Procedures
```kotlin
@Service
class DisasterRecoveryService {
    
    fun executeRecoveryPlan(scenario: DisasterScenario): RecoveryResult {
        return when (scenario) {
            DisasterScenario.BROKER_FAILURE -> {
                replaceFailedBroker()
            }
            DisasterScenario.CLUSTER_FAILURE -> {
                restoreFromBackup()
            }
            DisasterScenario.REGION_FAILURE -> {
                failoverToDisasterRecoveryRegion()
            }
            DisasterScenario.DATA_CORRUPTION -> {
                performPointInTimeRecovery()
            }
        }
    }
    
    private fun replaceFailedBroker(): RecoveryResult {
        // 1. Provision new broker
        // 2. Configure with same broker.id
        // 3. Start broker and let it sync
        // 4. Verify all partitions are replicated
        return RecoveryResult.success("Broker replaced and synced")
    }
    
    private fun restoreFromBackup(): RecoveryResult {
        // 1. Stop all applications
        // 2. Restore Zookeeper state
        // 3. Restore Kafka log directories
        // 4. Start cluster in order
        // 5. Verify data integrity
        return RecoveryResult.success("Cluster restored from backup")
    }
}
```

## 🎯 Best Practices

### Deployment Excellence
- **Infrastructure as Code** - Use Terraform, CloudFormation, or Pulumi
- **Immutable deployments** - Never modify running instances
- **Gradual rollouts** - Use canary or blue-green strategies
- **Automated validation** - Test every deployment automatically

### Scaling Wisdom
- **Proactive monitoring** - Scale before hitting limits
- **Predictable patterns** - Use historical data for planning
- **Cost optimization** - Balance performance with cost
- **Capacity planning** - Plan for peak loads and growth

### Operational Excellence
- **Runbook automation** - Automate common operational tasks
- **Incident response** - Have clear escalation procedures
- **Performance baselines** - Know your normal operating parameters
- **Regular testing** - Test disaster recovery procedures regularly

## 🔍 Troubleshooting

### Common Production Issues
1. **Memory leaks** - Monitor heap usage and GC patterns
2. **Network partitions** - Implement proper timeouts and retries
3. **Disk space** - Monitor log growth and implement retention
4. **Consumer lag** - Scale consumers or optimize processing

### Operational Commands
```bash
# Check cluster health
kafka-broker-api-versions --bootstrap-server localhost:9092

# Monitor resource usage
kafka-run-class kafka.tools.JmxTool \
  --object-name kafka.server:type=BrokerTopicMetrics,name=MessagesInPerSec

# Graceful shutdown
kafka-server-stop

# Rolling restart
kubectl rollout restart statefulset/kafka
```

## 🎉 Curriculum Complete!

**Congratulations!** You've completed the comprehensive Kafka Mastery Curriculum! You now have the skills to:

✅ **Design** event-driven architectures from scratch  
✅ **Build** production-ready Kafka applications  
✅ **Scale** systems to handle millions of events  
✅ **Secure** clusters with enterprise-grade security  
✅ **Monitor** and operate Kafka in production  
✅ **Deploy** with confidence using modern DevOps practices  

## 🚀 What's Next?

### Continue Learning
- **Advanced Kafka Connect** - Integration with external systems
- **KSQL** - SQL for stream processing
- **Multi-datacenter replication** - Global event streaming
- **Custom serializers** - Specialized data formats

### Apply Your Skills
- **Build real projects** - Apply concepts to your work
- **Contribute to community** - Share your learning journey
- **Mentor others** - Help developers learn Kafka
- **Stay updated** - Follow Kafka development and best practices

---

*"From Kafka beginner to production expert - you've mastered one of the most important technologies in modern software architecture. Now go build amazing systems!"* 🚀