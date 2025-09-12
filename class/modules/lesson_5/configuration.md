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

---

## Kafka Consumers & Consumer Groups
Apache Kafka uses consumers and consumer groups to process messages from topics efficiently.
In Spring Boot with Kotlin, you can configure consumers either directly with @KafkaListener or globally via @Configuration.

#### 🔑 Kafka Consumer Groups
- A Consumer Group is a team of consumers working together to read from one or more topics.
- Each consumer in the group handles a subset of partitions → no overlap inside the group.

Benefits:
- Scalability → add more consumers for parallel processing
- Fault tolerance → if one consumer fails, Kafka reassigns its partitions

👉 Rule of thumb:
- Same group → consumers share the work.
- Different groups → each group gets the full stream.

#### ⚡ Configuring Consumers with @KafkaListener

Use the annotation for quick, per-listener setup:

```kotlin
@KafkaListener(
    topics = ["\${kafka.topic.name:test-topic}"],
    groupId = "\${kafka.consumer.groupId:test-consumer-group}",
    clientIdPrefix = "neo",
    concurrency = "4"
)
fun receive(message: String) {
println("Received: $message")
}
```

- groupId → which group this consumer belongs to
- clientIdPrefix → prefix for generating unique ConsumerIds (e.g., neo-1, neo-2)
- concurrency → number of parallel consumers for this listener

#### ⚙️ Configuring Consumers with @Configuration

Use this when you want global, reusable setup for all consumers.

```kotlin
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.common.serialization.StringDeserializer
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.kafka.annotation.EnableKafka
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory
import org.springframework.kafka.core.DefaultKafkaConsumerFactory

@EnableKafka
@Configuration
class DataKafkaConfig {

    @Bean
    fun consumerFactory(): DefaultKafkaConsumerFactory<String, String> {
        val props = mapOf(
            ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG to "localhost:9092",
            ConsumerConfig.GROUP_ID_CONFIG to "test-consumer-group",
            ConsumerConfig.CLIENT_ID_CONFIG to "neo-client",
            ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG to StringDeserializer::class.java,
            ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG to StringDeserializer::class.java
        )
        return DefaultKafkaConsumerFactory(props)
    }

    @Bean
    fun kafkaListenerContainerFactory(): ConcurrentKafkaListenerContainerFactory<String, String> {
        val factory = ConcurrentKafkaListenerContainerFactory<String, String>()
        factory.consumerFactory = consumerFactory()
        factory.setConcurrency(3) // run 3 consumers in parallel
        return factory
    }
}
```

Now, your listener can be simple:
```kotlin
@KafkaListener(topics = ["test-topic"])
fun receive(message: String) {
println("Received: $message")
}
```


It will automatically use:
- groupId = test-consumer-group
- clientId = neo-client
- concurrency = 3



