# Workshop

## Let's build a Kafka basic application!

Let's start with the basic structure of a Kafka application.

![basic-structure.png](workshop_images/basic-structure.png)

### Kafka Configuration

#### Kafka Producer 

Try adding the following code to the `KafkaConfig` class.
```kotlin
    // Producer Configuration
    @Bean
    fun producerFactory(): ProducerFactory<String, String> {
        val configProps = HashMap<String, Any>()
        configProps[ProducerConfig.BOOTSTRAP_SERVERS_CONFIG] = bootstrapServers
        configProps[ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG] = StringSerializer::class.java
        configProps[ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG] = StringSerializer::class.java

        return DefaultKafkaProducerFactory(configProps)
    }

    @Bean
    fun kafkaTemplate(): KafkaTemplate<String, String> {
        return KafkaTemplate(producerFactory())
    }
```
- **Boostrap Servers** - is the address of the Kafka server.
- **Key Serializer** - is the serializer for the key.
- **Value Serializer** - is the serializer for the value.

### Kafka Producer Service

```kotlin
    fun sendMessage(message: String) {
        logger.info("Sending message to topic $TOPIC_NAME: $message")
        val key = UUID.randomUUID().toString()
        kafkaTemplate.send(TOPIC_NAME, key, message)
            .whenComplete { result, exception ->
                if (exception != null) {
                    logger.error("Failed to send message: ${exception.message}")
                    // Error handling should be implemented here
                } else {
                    logger.info("Message sent successfully to ${result?.recordMetadata?.topic()}:${result?.recordMetadata?.partition()}")
                }
            }
    }
```

### A Controller

A basic controller for sending messages to Kafka topics. Allowing the user to send a message to the topic.
```kotlin
    @RestController
    @RequestMapping("/api/messages")
    class MessageController(
        private val producerService: ProducerService
    ) {
    
        @PostMapping("/send")
        fun sendMessage(@RequestBody request: String) =
            producerService.sendMessage(request)
    }
```

### Application Properties

```properties
# Application Configuration
spring.application.name=kafka-starter-main

# Kafka Configuration
spring.kafka.bootstrap-servers=localhost:9092
spring.kafka.consumer.group-id=kafka-starter-group-1
```

### Run the application

### Open swagger and try sending a message to the topic.

http://localhost:8090/swagger-ui/index.html#

```
curl -X 'POST' \
  'http://localhost:8090/api/messages/send' \
  -H 'accept: */*' \
  -H 'Content-Type: application/json' \
  -d '"test"'
  ```

