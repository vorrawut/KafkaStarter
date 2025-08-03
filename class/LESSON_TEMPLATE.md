# 📖 Lesson Template for Instructors

This template provides the structure for creating new lessons in the Kafka curriculum.

## 📁 Directory Structure per Lesson

```
class/
├── workshop/lesson_X/          # Student starter code
│   ├── README.md              # Workshop instructions
│   ├── *.kt files             # Starter code with TODOs
│   └── application.yml        # Configuration template
├── modules/lesson_X/           # Learning materials  
│   ├── workshop_X.md          # Step-by-step walkthrough
│   ├── concept.md             # Theory and best practices
│   └── diagram.md (optional)  # Mermaid visualizations
└── answer/lesson_X/            # Complete solutions
    ├── *.kt files             # Full implementation
    ├── application.yml        # Complete configuration
    └── test/                  # Integration tests
```

## 📝 Content Templates

### Workshop Instructions (`workshop_X.md`)

```markdown
# Lesson X Workshop: [Topic Name]

## 🎯 What We Want to Build
[Clear description of the end goal]

## 📋 Expected Result
[What should work by the end - specific outcomes]

## 🚀 Step-by-Step Code Walkthrough

### Step 1: [Component Name]
[Detailed implementation steps with code snippets]

### Step 2: [Next Component]
[Continue building incrementally]

## 🔧 How to Run
[Exact commands to test the implementation]

## ✅ Success Criteria
[Checklist of working features]

## 🔍 Debugging Tips
[Common issues and solutions]
```

### Concept Guide (`concept.md`)

```markdown
# Lesson X: [Topic Name] - [Subtitle]

## 🎯 Objective
[Learning goals and key takeaways]

## 🧱 Core Components
[Technical concepts being introduced]

## 🔧 Implementation Patterns
[Code patterns and best practices]

## 📊 Architecture Diagrams
[Mermaid diagrams showing data flow]

## 🎯 Key Benefits
[Why this pattern/concept matters]

## ✅ Best Practices
[Production-ready guidelines]

## 🚀 What's Next?
[Link to next lesson]
```

### Starter Code Guidelines

#### TODO Comments Format
```kotlin
// TODO: Brief description of what to implement
// TODO: More detailed explanation if needed
// HINT: Helpful tip for implementation
```

#### Scaffold Structure
```kotlin
@Service
class ExampleService(
    // TODO: Inject required dependencies
) {
    
    // TODO: Implement main method
    fun mainMethod() {
        // TODO: Add implementation here
        // HINT: Use Spring's KafkaTemplate for publishing
    }
}
```

## 🎯 Learning Objectives Template

Each lesson should have 3-5 clear learning objectives:

### Knowledge Objectives (What students will know)
- Understand [concept]
- Know when to use [pattern]
- Recognize [anti-patterns]

### Skill Objectives (What students will do)
- Implement [feature]
- Configure [component]
- Debug [common issues]

### Application Objectives (How they'll use it)
- Apply [pattern] to real scenarios
- Integrate with existing systems
- Make architectural decisions

## 🛠️ Technical Requirements

### Code Quality Standards
- [ ] **Kotlin idioms** - Use data classes, null safety, etc.
- [ ] **Spring Boot patterns** - Proper annotations and injection
- [ ] **Error handling** - Graceful failure scenarios
- [ ] **Logging** - Appropriate log levels and messages
- [ ] **Testing** - Unit and integration tests

### Documentation Standards
- [ ] **Clear explanations** - Avoid unnecessary jargon
- [ ] **Code comments** - Explain complex logic
- [ ] **Examples** - Concrete, runnable code
- [ ] **Diagrams** - Visual representation of concepts
- [ ] **Links** - References to official docs

## 📊 Lesson Progression

### Prerequisites
- List concepts from previous lessons
- Required setup or configuration
- Assumed knowledge level

### New Concepts Introduced
- Primary learning focus (1-2 main concepts)
- Supporting concepts
- Related patterns or practices

### Next Lesson Preparation
- Concepts that will build on this lesson
- Setup required for next lesson
- Optional advanced reading

## 🧪 Testing Guidelines

### Workshop Validation
- [ ] Code compiles without errors
- [ ] Application starts successfully  
- [ ] Core functionality works
- [ ] Tests pass

### Learning Validation
- [ ] Students can explain key concepts
- [ ] Students can modify code confidently
- [ ] Students can troubleshoot issues
- [ ] Students can apply to new scenarios

## 📝 Assessment Ideas

### Quick Checks
- Modify existing code to add new feature
- Explain what happens if configuration changes
- Predict behavior of code modifications

### Practical Exercises
- Apply pattern to different use case
- Debug intentionally broken code
- Optimize for different requirements

### Discussion Questions
- When would you use this pattern?
- What are the trade-offs involved?
- How does this fit with previous lessons?

## 🔄 Iterative Improvement

### Student Feedback Collection
- What was unclear?
- What took longer than expected?
- What would help understanding?
- What real-world scenarios relate?

### Content Updates
- Simplify complex explanations
- Add more examples for difficult concepts
- Update for new versions/best practices
- Improve error messages and debugging

### Engagement Metrics
- Time to complete lesson
- Success rate on first try
- Questions asked during lesson
- Confidence level after completion

---

## 📚 Example Lesson Outline

**Lesson 8: Retry Strategies & Dead Letter Topics**

### Learning Objectives
- **Know**: Understand different failure scenarios in message processing
- **Do**: Implement retry logic and dead letter topic configuration
- **Apply**: Design fault-tolerant message processing systems

### Prerequisites
- Consumer groups (Lesson 7)
- Basic error handling concepts
- Spring Boot error handling

### Workshop Activities
1. Simulate processing failures
2. Configure retry policies
3. Implement dead letter topic
4. Add monitoring and alerting

### Assessment
- Can students explain when messages go to DLT?
- Can they configure different retry strategies?
- Can they troubleshoot stuck consumers?

---

This template ensures consistency across all lessons while maintaining flexibility for different learning styles and technical complexity levels.