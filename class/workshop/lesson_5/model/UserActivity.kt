package com.learning.KafkaStarter.model

import java.time.Instant

// TODO: Create data class for UserActivity events
// TODO: Include fields: userId, activityType, metadata, timestamp
// TODO: Add JSON annotations for serialization
// TODO: Consider partition key strategy for this event type

// HINT: userId is a good candidate for partition key
// HINT: activityType could be enum: LOGIN, LOGOUT, PURCHASE, UPDATE_PROFILE