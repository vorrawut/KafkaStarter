package com.learning.KafkaStarter.avro

// This is a placeholder for the generated Avro class
// In a real implementation, this would be generated from the .avsc schema file
// using the Avro Maven/Gradle plugin

data class UserCreatedEvent(
    val userId: CharSequence,
    val email: CharSequence,
    val timestamp: Long,
    val registrationMethod: CharSequence
) {
    companion object {
        fun newBuilder(): Builder = Builder()
        
        fun getClassSchema(): org.apache.avro.Schema {
            return org.apache.avro.Schema.Parser().parse("""
                {
                  "type": "record",
                  "name": "UserCreatedEvent",
                  "namespace": "com.learning.KafkaStarter.avro",
                  "fields": [
                    {"name": "userId", "type": "string"},
                    {"name": "email", "type": "string"},
                    {"name": "timestamp", "type": "long"},
                    {"name": "registrationMethod", "type": "string", "default": "unknown"}
                  ]
                }
            """.trimIndent())
        }
    }
    
    class Builder {
        private var userId: CharSequence = ""
        private var email: CharSequence = ""
        private var timestamp: Long = 0L
        private var registrationMethod: CharSequence = "unknown"
        
        fun setUserId(userId: CharSequence): Builder {
            this.userId = userId
            return this
        }
        
        fun setEmail(email: CharSequence): Builder {
            this.email = email
            return this
        }
        
        fun setTimestamp(timestamp: Long): Builder {
            this.timestamp = timestamp
            return this
        }
        
        fun setRegistrationMethod(method: CharSequence): Builder {
            this.registrationMethod = method
            return this
        }
        
        fun build(): UserCreatedEvent {
            return UserCreatedEvent(userId, email, timestamp, registrationMethod)
        }
    }
}