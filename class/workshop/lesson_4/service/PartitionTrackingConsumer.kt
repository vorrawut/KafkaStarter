package com.learning.KafkaStarter.service

import com.learning.KafkaStarter.model.UserActivity
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.stereotype.Service

// TODO: Add @Service annotation
// TODO: Create @KafkaListener for partition tracking
// TODO: Implement partition rebalancing listeners

@Service
class PartitionTrackingConsumer {
    
    // TODO: Create @KafkaListener method that accepts partition and offset headers
    // TODO: Log partition assignment information
    // TODO: Implement manual offset management
    // TODO: Track consumer lag by partition
    
    // TODO: Add @KafkaListener for partition rebalancing events
    // TODO: Implement ConsumerRebalanceListener to track partition assignments
    
    // HINT: Use @Header annotations to capture partition and offset info
    // HINT: ConsumerRebalanceListener.onPartitionsAssigned/Revoked for rebalancing
    // HINT: Use Acknowledgment parameter for manual offset commits
}