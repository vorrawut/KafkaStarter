# 🎪 Kafka Mastery Demo System

## 🎯 Real-World E-Commerce Platform

This demo system showcases **all 20 lessons** of the Kafka curriculum in action through a complete e-commerce platform that demonstrates enterprise-grade event-driven architecture.

## 🏗️ System Architecture

```
┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
│   User Service  │    │  Order Service  │    │Inventory Service│
│                 │    │                 │    │                 │
│ • Registration  │    │ • Order Create  │    │ • Stock Mgmt    │
│ • Authentication│    │ • State Machine │    │ • Reservations  │
│ • User Events   │    │ • Order Events  │    │ • Availability  │
└─────────┬───────┘    └─────────┬───────┘    └─────────┬───────┘
          │                      │                      │
          │              ┌───────▼──────────────────────▼───────┐
          │              │           Apache Kafka                │
          │              │                                       │
          │              │ Topics: user-events, order-events,    │
          │              │         inventory-events, payments,   │
          │              │         notifications, analytics      │
          └──────────────┤                                       │
                         └───────┬──────────────────────┬───────┘
                                 │                      │
┌─────────────────┐    ┌─────────▼───────┐    ┌─────────▼───────┐
│ Payment Service │    │Notification Svc │    │ Analytics Svc   │
│                 │    │                 │    │                 │
│ • Payment Proc  │    │ • Email/SMS     │    │ • Real-time     │
│ • Error Handling│    │ • Push Notifs   │    │ • Dashboards    │
│ • Retry Logic   │    │ • Fan-out       │    │ • Kafka Streams │
└─────────────────┘    └─────────────────┘    └─────────────────┘
```

## 🎓 Lessons Demonstrated

### 📚 **Foundation Patterns** (Lessons 1-6)
- **Event-driven architecture** across all services
- **Producer/Consumer patterns** in every microservice
- **Schema Registry** with Avro for type safety
- **Development tools** for debugging and monitoring

### 🛡️ **Resilient Messaging** (Lessons 7-13)
- **Consumer groups** for horizontal scaling
- **Error handling & DLT** in payment processing
- **Exactly-once processing** for financial transactions
- **Message transformation** for data enrichment
- **Fan-out notifications** for multi-channel delivery
- **Request-reply patterns** for synchronous operations

### 🌊 **Stream Processing** (Lessons 14-17)
- **Kafka Streams** for real-time analytics
- **Windowing** for time-based aggregations
- **State stores** for session management
- **Real-time dashboard** with live updates

### 🚀 **Production Ready** (Lessons 18-20)
- **Security** with SSL/SASL authentication
- **Monitoring** with Prometheus metrics
- **Deployment** with Docker Compose and scaling

## 🚀 Quick Start

### 1. Start Infrastructure
```bash
cd docker
docker-compose -f docker-compose-demo.yml up -d
```

### 2. Run Demo Services
```bash
# Start all services
./scripts/start-demo.sh

# Or start individually
cd demo/user-service && ./gradlew bootRun &
cd demo/order-service && ./gradlew bootRun &
cd demo/inventory-service && ./gradlew bootRun &
cd demo/payment-service && ./gradlew bootRun &
cd demo/notification-service && ./gradlew bootRun &
cd demo/analytics-service && ./gradlew bootRun &
```

### 3. Test E-Commerce Flow
```bash
# Complete e-commerce flow
./scripts/demo-flow.sh
```

### 4. View Real-Time Dashboard
- **Analytics Dashboard**: http://localhost:3000
- **Kafka UI**: http://localhost:8080  
- **Prometheus**: http://localhost:9090
- **Grafana**: http://localhost:3001

## 📊 Demo Scenarios

### 🛒 **Scenario 1: Happy Path Order**
1. User registers and logs in
2. User places order for available items
3. Inventory reserves stock
4. Payment processes successfully
5. Notifications sent (email, SMS, push)
6. Analytics tracks conversion

### ⚠️ **Scenario 2: Payment Failure**
1. User places order
2. Inventory reserves stock
3. Payment fails (insufficient funds)
4. Order moves to pending payment
5. Stock reservation expires
6. Retry notifications sent

### 📈 **Scenario 3: High Load**
1. Multiple concurrent orders
2. Consumer groups scale processing
3. Circuit breakers protect services
4. Real-time metrics show performance
5. Auto-scaling triggers if needed

### 🔒 **Scenario 4: Security Test**
1. Authentication required for orders
2. ACLs prevent unauthorized access
3. Sensitive data encrypted
4. Audit trail captured

## 🔧 Service Details

### 🧑 **User Service** (Port 8101)
- User registration and authentication
- User preference management
- Event publishing for user lifecycle

### 📦 **Order Service** (Port 8102)  
- Order creation and state management
- Order event stream processing
- Integration with inventory and payment

### 📋 **Inventory Service** (Port 8103)
- Real-time stock management
- Reservation and commitment logic
- Stock level monitoring

### 💳 **Payment Service** (Port 8104)
- Payment processing with retry logic
- Error handling and dead letter topics
- Financial transaction auditing

### 🔔 **Notification Service** (Port 8105)
- Multi-channel notifications (email, SMS, push)
- Fan-out pattern implementation  
- Template management

### 📊 **Analytics Service** (Port 8106)
- Real-time analytics with Kafka Streams
- Business metrics and KPIs
- Live dashboard updates

## 📈 Monitoring & Observability

### 🎯 **Key Metrics**
- Order conversion rate
- Payment success rate
- Inventory turnover
- Notification delivery rates
- System throughput and latency

### 🚨 **Alerts**
- High error rates
- Consumer lag
- Low inventory levels
- Payment failures
- System performance degradation

## 🧪 Testing

### Unit Tests
```bash
./gradlew test
```

### Integration Tests
```bash
./gradlew integrationTest
```

### Load Tests
```bash
./scripts/load-test.sh
```

### Chaos Engineering
```bash
./scripts/chaos-test.sh
```

## 📚 Learning Outcomes

After exploring this demo, you'll understand:

✅ **How to architect** event-driven microservices  
✅ **How to handle failures** gracefully in distributed systems  
✅ **How to process streams** in real-time for analytics  
✅ **How to secure** production Kafka deployments  
✅ **How to monitor** and scale Kafka applications  
✅ **How to test** complex event-driven systems  

## 🎯 Next Steps

1. **Explore the code** - Each service demonstrates specific patterns
2. **Run scenarios** - See how events flow through the system  
3. **Modify configurations** - Experiment with different settings
4. **Add features** - Extend the system with new capabilities
5. **Deploy to cloud** - Use the deployment patterns from Lesson 20

---

**This demo represents the culmination of your Kafka learning journey - from basic concepts to production-ready, enterprise-grade event-driven systems!** 🚀