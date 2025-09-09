# Getting Started - 5 Minutes to Kafka

## Step 1: Start Kafka
```bash
cd kafka-producer-consumer-lab
./commands.sh start
```

## Step 2: Create a Topic
```bash
./commands.sh create-topic messages
```

## Step 3: Open Two Terminals

**Terminal 1 - Producer:**
```bash
./commands.sh producer messages
```

**Terminal 2 - Consumer:**
```bash
./commands.sh consumer messages
```

## Step 4: Send Messages
In Terminal 1, type:
```
Hello World
This is my first Kafka message
Kafka is working!
```

Watch them appear in Terminal 2!

## Step 5: Try Consumer Groups
**Terminal 3:**
```bash
./commands.sh consumer-group messages group1
```

**Terminal 4:**
```bash
./commands.sh consumer-group messages group1
```

Send more messages and see how they're distributed!

## Step 6: View in UI
Open: http://localhost:8080

## All Commands
```bash
./commands.sh
```

## Clean Up
```bash
./commands.sh clean
```

That's it! You're ready to explore Kafka! 🚀
