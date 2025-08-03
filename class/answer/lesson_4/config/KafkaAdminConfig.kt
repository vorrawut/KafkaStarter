package com.learning.KafkaStarter.config

import org.apache.kafka.clients.admin.AdminClient
import org.apache.kafka.clients.admin.AdminClientConfig
import org.springframework.boot.autoconfigure.kafka.KafkaProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.kafka.core.KafkaAdmin

@Configuration
class KafkaAdminConfig(
    private val kafkaProperties: KafkaProperties
) {
    
    @Bean
    fun kafkaAdmin(): KafkaAdmin {
        val configs = mapOf<String, Any>(
            AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG to kafkaProperties.bootstrapServers,
            AdminClientConfig.REQUEST_TIMEOUT_MS_CONFIG to 30000,
            AdminClientConfig.DEFAULT_API_TIMEOUT_MS_CONFIG to 60000
        )
        return KafkaAdmin(configs)
    }
    
    @Bean
    fun adminClient(): AdminClient {
        val configs = mapOf<String, Any>(
            AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG to kafkaProperties.bootstrapServers,
            AdminClientConfig.REQUEST_TIMEOUT_MS_CONFIG to 30000,
            AdminClientConfig.DEFAULT_API_TIMEOUT_MS_CONFIG to 60000,
            AdminClientConfig.RETRIES_CONFIG to 3
        )
        return AdminClient.create(configs)
    }
}