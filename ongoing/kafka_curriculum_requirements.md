# Kafka Curriculum Requirements

## Project Goal
Create a comprehensive Kafka learning curriculum using Spring Boot and Kotlin that transforms beginners into production-ready Kafka experts through 20 progressive, hands-on lessons.

## Key Requirements

### 📋 Curriculum Structure
- **20 lessons** divided into 4 phases
- **Self-contained lessons** (20-60 minutes each)
- **Progressive difficulty** from basic concepts to production deployment
- **Real-world scenarios** and practical use cases

### 🏗️ Technical Stack
- **Language**: Kotlin
- **Framework**: Spring Boot 3.x+
- **Build**: Gradle Kotlin DSL
- **Messaging**: Apache Kafka
- **Schema**: Avro, Protobuf
- **Testing**: Embedded Kafka, TestContainers
- **Monitoring**: Prometheus, Grafana-ready

### 📁 Directory Structure
```
class/workshop/lesson_x/   # Starter code with TODOs
class/modules/lesson_x/    # Step-by-step documentation
class/answer/lesson_x/     # Complete working solutions
```

### 📖 Documentation Format
Each lesson includes:
- `workshop_x.md` - Step-by-step coding walkthrough
- `concept.md` - Theory and best practices  
- `diagram.md` - Mermaid visualizations (optional)

### 🎯 Learning Phases

#### Phase 1: Foundations (Lessons 1-6)
- Kafka fundamentals and setup
- Basic producer/consumer patterns
- Schema management
- Development tooling

#### Phase 2: Resilient Messaging (Lessons 7-13)
- Consumer groups and scaling
- Error handling and retry strategies
- Idempotent processing
- Microservice coordination patterns

#### Phase 3: Streaming & State (Lessons 14-17)
- Kafka Streams API
- Windowing and aggregations
- State stores and fault tolerance
- Real-time dashboard building

#### Phase 4: Production (Lessons 18-20)
- Security implementation
- Monitoring and observability
- Deployment and scaling best practices

### 🚀 Production Readiness
- **Docker Compose** setup with full Kafka ecosystem
- **Integration testing** with embedded Kafka
- **Monitoring** setup with JMX and Prometheus
- **Security** configurations and best practices
- **CI/CD** patterns and deployment strategies

### 🎪 Demo System
Complete mini-system showcasing:
- User registration flow
- Email/SMS notification services
- Audit log streaming
- Real-time dashboard
- State management

## Success Criteria
- Each lesson is **runnable independently**
- **Complete working solutions** for all lessons
- **Production-ready patterns** throughout
- **Comprehensive testing** framework
- **Clear documentation** with visual aids

## Status: In Progress ✅

Current implementation focuses on:
1. ✅ Project structure setup
2. ✅ Docker environment creation  
3. 🔄 Phase 1 lesson implementation
4. ⏳ Testing framework setup
5. ⏳ Demo system creation