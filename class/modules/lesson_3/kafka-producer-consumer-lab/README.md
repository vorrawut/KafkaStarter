# Simple Kafka Producer & Consumer Lab

## Quick Start

```bash
# Start Kafka
docker-compose up -d

# Check if running
docker-compose ps
```

## Open Kafka UI
http://localhost:8080

## Basic Commands

### Create Topic
```bash
docker exec lab-kafka kafka-topics --create --topic my-topic --bootstrap-server localhost:9092 --partitions 3 --replication-factor 1
```

### Start Producer
```bash
docker exec -it lab-kafka kafka-console-producer --topic my-topic --bootstrap-server localhost:9092
```

### Start Consumer
```bash
docker exec -it lab-kafka kafka-console-consumer --topic my-topic --bootstrap-server localhost:9092 --from-beginning
```

### Producer with Keys
```bash
docker exec -it lab-kafka kafka-console-producer --topic my-topic --bootstrap-server localhost:9092 --property "parse.key=true" --property "key.separator=:"
```

### Consumer with Keys
```bash
docker exec -it lab-kafka kafka-console-consumer --topic my-topic --bootstrap-server localhost:9092 --from-beginning --property "print.key=true" --property "key.separator= => "
```

### Consumer Groups
```bash
# Start consumer in group
docker exec -it lab-kafka kafka-console-consumer --topic my-topic --bootstrap-server localhost:9092 --group my-group

# Check group status
docker exec lab-kafka kafka-consumer-groups --bootstrap-server localhost:9092 --describe --group my-group
```

## Clean Up
```bash
docker-compose down -v
```

That's it! Use Kafka UI at http://localhost:8080 to see everything visually.
