# Workshop: Why Kafka? Understanding Event-Driven Architecture

## 🎯 Objective
Understand the fundamentals of event-driven architecture and why Apache Kafka is the leading solution for building resilient, scalable systems.

## 📋 Workshop Tasks

### Task 1: Event-Driven vs Traditional Architecture Analysis
Complete the architecture comparison in `analysis/ArchitectureComparison.kt`

### Task 2: Identify Use Cases 
Fill in real-world scenarios where Kafka excels in `scenarios/KafkaUseCases.kt`

### Task 3: Design Event Flow
Create an event-driven design for an e-commerce system in `design/EventFlowDesign.kt`

## 🧠 Key Concepts to Understand
- Event-driven architecture principles
- Publish-subscribe pattern
- Event sourcing concepts
- System decoupling benefits
- Scalability through events

## ✅ Success Criteria
- [ ] Complete architecture comparison analysis
- [ ] Identify 5+ real-world Kafka use cases
- [ ] Design event flow for e-commerce scenario
- [ ] Understand why events > direct API calls
- [ ] Grasp decoupling benefits

## 🔍 Mermaid Diagram: Event-Driven Architecture
```mermaid
graph TB
    subgraph "Traditional Architecture"
        A1[Order Service] --> B1[Payment Service]
        A1 --> C1[Inventory Service]
        A1 --> D1[Email Service]
        A1 --> E1[Shipping Service]
    end
    
    subgraph "Event-Driven Architecture"
        A2[Order Service] --> K[Kafka]
        K --> B2[Payment Service]
        K --> C2[Inventory Service] 
        K --> D2[Email Service]
        K --> E2[Shipping Service]
        K --> F2[Analytics Service]
        K --> G2[Audit Service]
    end
    
    style K fill:#ff6b6b
    style A2 fill:#4ecdc4
    style A1 fill:#ffe66d
```

## 🚀 Next Steps
Move to [Lesson 2: Environment Setup](../lesson_2/README.md) to get your Kafka environment running!