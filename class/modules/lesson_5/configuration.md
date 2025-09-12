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
@Configuration
open class Config {

    @Autowired
    private lateinit var kafkaProperties: KafkaProperties

    private fun consumerFactory(): ConsumerFactory<String, Foo> {
        val configs = kafkaProperties.buildConsumerProperties()
        configs[ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG] = StringDeserializer::class.java
        configs[ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG] = JsonDeserializer::class.java

        return DefaultKafkaConsumerFactory(
            configs,
            StringDeserializer(),
            JsonDeserializer<Foo>()
        )
    }

    @Bean(name = ["kafkaListenerContainerFactory"])
    open fun kafkaListenerContainerFactory(): ConcurrentKafkaListenerContainerFactory<String, Foo>? {
        val factory = ConcurrentKafkaListenerContainerFactory<String, Foo>()
        factory.consumerFactory = consumerFactory()
        return factory
    }
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

