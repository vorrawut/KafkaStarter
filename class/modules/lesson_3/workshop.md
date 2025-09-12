# Workshop

## First Producer/Consumer

### Kafka CLI
Start by installing the Kafka CLI

brew - ``` brew install kafka ```

Once we are installed, run the following command to verify the installation.

``` 
kafka-console-consumer --version
```
Should return the version, in this case should be `4.1.0`
```aiignore
4.1.0
```
If you are seeing this, that's mean you are able to use the Kafka CLI!

#### Highlight CLI commands
- **kafka-topics** - Create, delete, describe, list, and manage Kafka topics
- **kafka-console-producer** - Produce messages to Kafka topics from command line
- **kafka-console-consumer** - Consume messages from Kafka topics to command line
- **kafka-consumer-groups** - Manage Kafka consumer groups
- ...

### Local setup

Create a docker-compose file

```yaml
services:
  zookeeper:
    image: confluentinc/cp-zookeeper:7.4.0
    container_name: lab-zookeeper
    ports:
      - "2181:2181"
    environment:
      ZOOKEEPER_CLIENT_PORT: 2181
      ZOOKEEPER_TICK_TIME: 2000

  kafka:
    image: confluentinc/cp-kafka:7.4.0
    container_name: lab-kafka
    depends_on:
      - zookeeper
    ports:
      - "9092:9092"
    environment:
      KAFKA_BROKER_ID: 1
      KAFKA_ZOOKEEPER_CONNECT: 'zookeeper:2181'
      KAFKA_LISTENER_SECURITY_PROTOCOL_MAP: PLAINTEXT:PLAINTEXT,PLAINTEXT_HOST:PLAINTEXT
      KAFKA_ADVERTISED_LISTENERS: PLAINTEXT://kafka:29092,PLAINTEXT_HOST://localhost:9092
      KAFKA_LISTENERS: PLAINTEXT://kafka:29092,PLAINTEXT_HOST://0.0.0.0:9092
      KAFKA_INTER_BROKER_LISTENER_NAME: PLAINTEXT
      KAFKA_OFFSETS_TOPIC_REPLICATION_FACTOR: 1
      KAFKA_AUTO_CREATE_TOPICS_ENABLE: 'true'

  kafka-ui:
    image: provectuslabs/kafka-ui:latest
    container_name: lab-kafka-ui
    depends_on:
      - kafka
    ports:
      - "8081:8080"
    environment:
      KAFKA_CLUSTERS_0_NAME: lab-cluster
      KAFKA_CLUSTERS_0_BOOTSTRAPSERVERS: kafka:29092
```
Then run the docker compose
```aiignore
# Start Kafka
docker-compose up -d

# Check if running
docker-compose ps
```

```aiignore
NAME            IMAGE                             COMMAND                  SERVICE     CREATED          STATUS          PORTS
lab-kafka       confluentinc/cp-kafka:7.4.0       "/etc/confluent/dock…"   kafka       18 seconds ago   Up 17 seconds   0.0.0.0:9092->9092/tcp, [::]:9092->9092/tcp
lab-kafka-ui    provectuslabs/kafka-ui:latest     "/bin/sh -c 'java --…"   kafka-ui    18 seconds ago   Up 17 seconds   0.0.0.0:8080->8080/tcp, [::]:8080->8080/tcp
lab-zookeeper   confluentinc/cp-zookeeper:7.4.0   "/etc/confluent/dock…"   zookeeper   18 seconds ago   Up 17 seconds   2888/tcp, 0.0.0.0:2181->2181/tcp, [::]:2181->2181/tcp, 3888/tcp
```

#### Create your first topic

```aiignore
kafka-topics --create --topic first-topic --bootstrap-server localhost:9092

# Check your newly created topic
kafka-topics --list --bootstrap-server localhost:9092
```

#### Start your local Producer / Consumer

```aiignore
# Start producer
kafka-console-producer   --bootstrap-server localhost:9092   --topic test-topic

# Start consumer
kafka-console-consumer   --bootstrap-server localhost:9092   --topic test-topic
```
Check the consumer from the beginning
```aiignore
kafka-console-consumer   --bootstrap-server localhost:9092   --topic test-topic   --from-beginning
```

### Try sending message over internet!
Create a properties file named `client.properties` and save into any folder!
```properties
bootstrap.servers=pkc-l7j7w.asia-east1.gcp.confluent.cloud:9092
security.protocol=SASL_SSL
sasl.mechanism=PLAIN
sasl.jaas.config=org.apache.kafka.common.security.plain.PlainLoginModule required \
  username="3OFJGVL3G3YQY2UC" \
  password="cfltYpG9GFIC5Fx0Z8WuayLW7sbPv8Ef7ywbGqU5r4qi0PjSJTe3KN0IlnLyugzQ";
ssl.endpoint.identification.algorithm=https
client.dns.lookup=use_all_dns_ips
```

```aiignore
kafka-console-producer   --bootstrap-server pkc-l7j7w.asia-east1.gcp.confluent.cloud:9092   --topic hello-topic   --producer.config client.properties
```