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

### Local setup

Create a docker-compose file

```aiignore
version: '3.8'

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
      - "8080:8080"
    environment:
      KAFKA_CLUSTERS_0_NAME: lab-cluster
      KAFKA_CLUSTERS_0_BOOTSTRAPSERVERS: kafka:29092
```
Then run the docker compose
```bash
# Start Kafka
docker-compose up -d

# Check if running
docker-compose ps
```

### Try sending message over internet!