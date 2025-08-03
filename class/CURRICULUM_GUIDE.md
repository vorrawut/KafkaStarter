# 📚 Kafka Curriculum Guide

## 🎯 How to Use This Curriculum

This curriculum is designed for **progressive learning** - each lesson builds on previous concepts. Follow the lessons in order for the best learning experience.

## 📖 Learning Path Structure

Each lesson follows a consistent 3-part structure:

### 📝 **Workshop** (`class/workshop/lesson_x/`)
- **Starter code** with TODO comments
- **Guided implementation** with hints
- **Step-by-step instructions**

### 🧠 **Concepts** (`class/modules/lesson_x/`)
- **Theory and best practices**
- **Real-world context and use cases**
- **Architecture patterns and diagrams**

### ✅ **Solutions** (`class/answer/lesson_x/`)
- **Complete working implementation**
- **Production-ready code patterns**
- **Comprehensive test coverage**

## 🚀 Lesson Progress Tracker

Track your progress through the curriculum:

### 🧱 Phase 1: Foundations (Lessons 1-6)
- [ ] **Lesson 1**: Why Kafka? *(Theory)* → [Start](modules/lesson_1/concept.md)
- [ ] **Lesson 2**: Environment Setup *(Setup)* → [Start](modules/lesson_2/concept.md)  
- [ ] **Lesson 3**: First Producer/Consumer *(Hands-on)* → [Workshop](workshop/lesson_3/) | [Concepts](modules/lesson_3/concept.md)
- [ ] **Lesson 4**: Topics & Partitions *(Hands-on)* → [Workshop](workshop/lesson_4/)
- [ ] **Lesson 5**: Schema Registry *(Hands-on)* → [Workshop](workshop/lesson_5/)
- [ ] **Lesson 6**: Development Tools *(Hands-on)* → [Workshop](workshop/lesson_6/)

### 🔧 Phase 2: Resilient Messaging (Lessons 7-13)
- [ ] **Lesson 7**: Consumer Groups → [Workshop](workshop/lesson_7/)
- [ ] **Lesson 8**: Error Handling & DLT → [Workshop](workshop/lesson_8/)
- [ ] **Lesson 9**: Exactly-Once Processing → [Workshop](workshop/lesson_9/)
- [ ] **Lesson 10**: Message Transformation → [Workshop](workshop/lesson_10/)
- [ ] **Lesson 11**: Fan-out Notifications → [Workshop](workshop/lesson_11/)
- [ ] **Lesson 12**: Hybrid REST + Kafka → [Workshop](workshop/lesson_12/)
- [ ] **Lesson 13**: Request-Reply Pattern → [Workshop](workshop/lesson_13/)

### 🌀 Phase 3: Streaming & State (Lessons 14-17)
- [ ] **Lesson 14**: Kafka Streams Intro → [Workshop](workshop/lesson_14/)
- [ ] **Lesson 15**: Windowing & Joins → [Workshop](workshop/lesson_15/)
- [ ] **Lesson 16**: State Stores → [Workshop](workshop/lesson_16/)
- [ ] **Lesson 17**: Real-time Dashboard → [Workshop](workshop/lesson_17/)

### 🚀 Phase 4: Production (Lessons 18-20)
- [ ] **Lesson 18**: Security & ACLs → [Workshop](workshop/lesson_18/)
- [ ] **Lesson 19**: Monitoring & Metrics → [Workshop](workshop/lesson_19/)
- [ ] **Lesson 20**: Deployment & Scaling → [Workshop](workshop/lesson_20/)

## 📋 Prerequisites Checklist

Before starting:
- [ ] **Docker Desktop** installed and running
- [ ] **Java 21** or higher
- [ ] **Kotlin** basic familiarity
- [ ] **Spring Boot** basic understanding
- [ ] **IntelliJ IDEA** or preferred IDE

## 🛠️ Environment Setup

### 1. Start Kafka Environment
```bash
cd docker
docker-compose up -d
```

### 2. Verify Setup
- Kafka UI: http://localhost:8080
- AKHQ: http://localhost:8082  
- Schema Registry: http://localhost:8081

### 3. Test Application
```bash
./gradlew bootRun
curl http://localhost:8090/api/users/health
```

## 🎯 Learning Strategies

### 📚 **For Beginners**
1. **Read concepts first** - Understand the theory
2. **Follow workshops step-by-step** - Don't skip ahead
3. **Experiment** - Modify code and see what happens
4. **Use Kafka UI** - Visualize what's happening

### 🚀 **For Experienced Developers**
1. **Skim concepts** - Focus on Kafka-specific patterns
2. **Try workshop first** - Challenge yourself
3. **Compare with solutions** - Learn best practices
4. **Extend examples** - Add your own features

### 👥 **For Teams**
1. **Pair programming** - Work through workshops together
2. **Code reviews** - Compare solutions and discuss trade-offs
3. **Architecture discussions** - Apply patterns to your systems
4. **Production planning** - Discuss deployment strategies

## 🔧 Troubleshooting

### Common Issues

**Kafka Connection Issues**
```bash
docker-compose ps  # Check services are running
docker-compose logs kafka  # Check broker logs
```

**Port Conflicts**
```bash
lsof -i :9092  # Check what's using Kafka port
lsof -i :8080  # Check what's using UI port
```

**Memory Issues**
- Increase Docker memory to 4GB+
- Close unnecessary applications

**Build Issues**
```bash
./gradlew clean build  # Clean and rebuild
./gradlew --refresh-dependencies  # Refresh dependencies
```

## 📊 Assessment Criteria

### ✅ **Lesson Completion**
- [ ] Code compiles without errors
- [ ] Tests pass
- [ ] Application runs successfully
- [ ] Can demonstrate functionality

### 🎯 **Understanding Check**
- [ ] Can explain key concepts
- [ ] Can modify code and predict results
- [ ] Can troubleshoot common issues
- [ ] Can apply patterns to new scenarios

## 🏆 Graduation Criteria

After completing all lessons, you should be able to:

✅ **Design** event-driven architectures  
✅ **Build** production-ready Kafka applications  
✅ **Debug** Kafka connectivity and performance issues  
✅ **Implement** error handling and retry strategies  
✅ **Deploy** Kafka applications with monitoring  
✅ **Scale** systems based on throughput requirements  

## 💡 Next Steps After Graduation

### 🎪 **Build Real Projects**
- Implement microservice communication
- Create real-time analytics dashboard
- Build event sourcing system

### 📈 **Advanced Topics**
- Multi-datacenter replication
- KSQL and streaming SQL
- Custom serializers and interceptors
- Performance tuning and optimization

### 🌟 **Community Engagement**
- Contribute to Kafka ecosystem projects
- Share your learning journey
- Mentor other developers

---

**Happy Learning! 🚀**

*This curriculum is designed to take you from Kafka beginner to production expert. Take your time, experiment, and don't hesitate to go back and review earlier lessons.*