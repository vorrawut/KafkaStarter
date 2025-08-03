# 🚀 Kafka Mastery with Spring Boot + Kotlin
## From Beginner to Expert in 20 Progressive Lessons

Transform from a Kafka beginner to a production-ready expert through hands-on, real-world lessons using Kotlin and Spring Boot.

## 🎯 What You'll Master

- **Event-Driven Architecture**: Design resilient, scalable microservices
- **Kafka Fundamentals**: Topics, partitions, consumers, producers
- **Advanced Patterns**: Streaming, state management, exactly-once processing
- **Production Skills**: Monitoring, security, scaling, and deployment

## 📚 Curriculum Overview

### 🧱 Phase 1: Foundations of Kafka (Lessons 1-6)
Master the core concepts and get hands-on with basic Kafka operations.

| Lesson | Topic | Skills | Duration |
|--------|-------|--------|----------|
| 1 | Why Kafka? Event-Driven Systems | Architecture patterns, use cases | 30 min |
| 2 | Kafka Setup with Docker | Environment setup, tooling | 25 min |
| 3 | Hello Kafka: Producer/Consumer | First app, JSON messages | 40 min |
| 4 | Topics, Partitions & Offsets | Core concepts, CLI tools | 35 min |
| 5 | Message Schemas (Avro/Protobuf) | Schema evolution, type safety | 45 min |
| 6 | Kafka Dev Tools | Debugging, testing, monitoring | 30 min |

### 🔧 Phase 2: Building Resilient Messaging (Lessons 7-13)
Learn production-ready patterns for reliable message processing.

| Lesson | Topic | Skills | Duration |
|--------|-------|--------|----------|
| 7 | Consumer Groups & Load Balancing | Scaling consumers, offset management | 40 min |
| 8 | Retry Strategies & Dead Letter Topics | Error handling, resilience | 45 min |
| 9 | Manual Ack & Idempotent Consumers | Exactly-once processing | 50 min |
| 10 | Message Transformation & Filtering | Data processing patterns | 35 min |
| 11 | Fan-out Pattern: Notification System | Microservice coordination | 60 min |
| 12 | Kafka-Triggered REST & Command APIs | Hybrid architectures | 45 min |
| 13 | Request-Reply with Kafka | Synchronous patterns | 40 min |

### 🌀 Phase 3: Kafka Streams & State (Lessons 14-17)
Master stream processing and stateful applications.

| Lesson | Topic | Skills | Duration |
|--------|-------|--------|----------|
| 14 | Intro to Kafka Streams API | Stream transformations | 45 min |
| 15 | Windowing, Joins & Aggregations | Time-based processing | 60 min |
| 16 | Local State Stores | Fault-tolerant state | 50 min |
| 17 | Building a Realtime Dashboard | End-to-end streaming app | 75 min |

### 🚀 Phase 4: Production & Scaling (Lessons 18-20)
Deploy and scale Kafka applications in production.

| Lesson | Topic | Skills | Duration |
|--------|-------|--------|----------|
| 18 | Kafka Security 101 | SSL/SASL, ACLs, authentication | 50 min |
| 19 | Observability & Monitoring | Metrics, alerting, debugging | 60 min |
| 20 | Deployment Best Practices | Cloud deployment, CI/CD | 45 min |

## 🏗️ Project Structure

```
KafkaStarter/
├── class/                    # Learning curriculum
│   ├── workshop/lesson_x/    # Starter code with TODOs
│   ├── modules/lesson_x/     # Documentation & concepts
│   └── answer/lesson_x/      # Complete solutions
├── demo/                     # Complete mini-system example
├── docker/                   # Docker Compose setup
└── test_integration/         # Testing frameworks
```

## 🚀 Quick Start

1. **Setup Environment**
   ```bash
   cd docker
   docker-compose up -d
   ```

2. **Verify Setup**
   - Kafka UI: http://localhost:8080
   - AKHQ: http://localhost:8082

3. **Start Learning**
   ```bash
   cd class/workshop/lesson_1
   # Follow the workshop instructions
   ```

4. **Run Tests**
   ```bash
   ./gradlew test
   ```

## 📖 Learning Path

Each lesson follows a consistent structure:
- **Workshop**: Hands-on coding with step-by-step guidance
- **Concepts**: Theory and best practices
- **Solution**: Complete working implementation
- **Tests**: Verification and understanding

## 🎯 Learning Outcomes

By completing this curriculum, you'll be able to:

✅ **Design event-driven architectures** with confidence  
✅ **Build production-ready Kafka applications** with Spring Boot  
✅ **Handle complex streaming scenarios** with Kafka Streams  
✅ **Implement robust error handling** and retry strategies  
✅ **Monitor and scale Kafka systems** in production  
✅ **Apply security best practices** for enterprise deployment  

## 🤝 Target Audience

- **Backend developers** new to event-driven systems
- **Spring Boot engineers** wanting to add Kafka skills
- **Microservice architects** designing resilient systems
- **DevOps engineers** deploying message-driven applications

## 💡 Prerequisites

- Basic Kotlin knowledge
- Spring Boot familiarity
- Docker understanding
- Command line comfort

---

## 🚀 Ready to become a Kafka expert?

Start with [Lesson 1: Why Kafka?](class/modules/lesson_1/concept.md) and begin your journey to mastering event-driven systems!

---

*This curriculum emphasizes practical, production-ready skills through hands-on coding. Every lesson builds on real-world scenarios you'll encounter in modern software development.*