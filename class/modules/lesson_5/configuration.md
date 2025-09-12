# Configuration

---

## 📦 Installation

Since we are using Kafka with Spring Boot, you will need to add these libraries to your project in your `build.gradle.kts` file in addition to the usual JUnit and Spring dependencies:

```kotlin
dependencies {
    implementation("org.springframework.kafka:spring-kafka")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
    testImplementation("org.springframework.kafka:spring-kafka-test:2.6.5")
}
```

### ⚙️ Kafka Configuration
For configuration, we’re using the default values and updating any that are necessary.

```kotlin
@ConfigurationProperties("kafka")
data class KafkaProperties(
    val bootstrapServers: String,
    val clientIdPrefix: String,
)

@Configuration
class KafkaConfig {
    @Bean
    fun producerFactory(kafkaProperties: KafkaProperties): DefaultKafkaProducerFactory<String, String> {
        val senderProps: MutableMap<String, Any> = mutableMapOf()

        senderProps[ProducerConfig.BOOTSTRAP_SERVERS_CONFIG] = kafkaProperties.bootstrapServers
        senderProps[ProducerConfig.CLIENT_ID_CONFIG] = kafkaProperties.clientIdPrefix
        senderProps[ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG] = StringSerializer::class.java
        senderProps[ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG] = StringSerializer::class.java

        return DefaultKafkaProducerFactory<String, String>(senderProps)
    }

    @Bean
    fun kafkaTemplate(producerFactory: ProducerFactory<String, String>) = KafkaTemplate(producerFactory)
}
```

### Application Configuration (application.yml)
With Spring Boot, you can configure Kafka properties (like broker IPs) directly in your application.yml.
Spring will automatically pick them up and apply them.

```kotlin
kafka:
  security-protocol: "${KAFKA_SECURITY_PROTOCOL:PLAINTEXT}"
  username: "${KAFKA_USERNAME:user}"
  password: "${KAFKA_PASSWORD:password}"
  bootstrap-servers: "${KAFKA_BOOTSTRAP_SERVERS:localhost:9092}"
  sasl:
    mechanism: "${KAFKA_SASL_MECHANISM:PLAIN}"
  ssl:
    enabled: false
    protocol: "TLS"
    enabled-protocols: "TLSv1.2,TLSv1.1,TLSv1"
```

⚠️ Sensitive information should be passed via environment variables, with default values for local development.

