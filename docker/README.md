# Kafka Learning Environment

This Docker Compose setup provides a complete Kafka learning environment with:

## Services

- **Kafka Broker** (localhost:9092) - Main Kafka server
- **Zookeeper** (localhost:2181) - Kafka coordination service
- **Schema Registry** (localhost:8081) - Schema management for Avro/Protobuf
- **Kafka UI** (localhost:8080) - Modern web UI for Kafka management
- **AKHQ** (localhost:8082) - Alternative Kafka web interface
- **Kafka Connect** (localhost:8083) - Kafka Connect runtime

## Quick Start

```bash
# Start all services
cd docker
docker-compose up -d

# Check service status
docker-compose ps

# View logs
docker-compose logs kafka

# Stop all services
docker-compose down

# Clean up (removes volumes)
docker-compose down -v
```

## Accessing Services

- **Kafka UI**: http://localhost:8080 (Primary UI - recommended)
- **AKHQ**: http://localhost:8082 (Alternative UI)
- **Schema Registry**: http://localhost:8081

## Useful Commands

```bash
# Create a topic
docker exec kafka-starter-broker kafka-topics --create --topic test-topic --bootstrap-server localhost:9092 --partitions 3 --replication-factor 1

# List topics
docker exec kafka-starter-broker kafka-topics --list --bootstrap-server localhost:9092

# Produce messages
docker exec -it kafka-starter-broker kafka-console-producer --topic test-topic --bootstrap-server localhost:9092

# Consume messages
docker exec -it kafka-starter-broker kafka-console-consumer --topic test-topic --from-beginning --bootstrap-server localhost:9092

# View consumer groups
docker exec kafka-starter-broker kafka-consumer-groups --bootstrap-server localhost:9092 --list
```

## Environment Variables

The setup uses development-friendly settings:
- Single broker setup
- Auto-create topics enabled
- Replication factor = 1
- JMX monitoring enabled
- Schema Registry integrated

Perfect for learning and development!