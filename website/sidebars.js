/**
 * Creating a sidebar enables you to:
 - create an ordered group of docs
 - render a "Next" and "Previous" button
 - provide table of contents on the right side of each doc
 */

// @ts-check

/** @type {import('@docusaurus/plugin-content-docs').SidebarsConfig} */
const sidebars = {
  // By default, Docusaurus generates a sidebar from the docs folder structure
  tutorialSidebar: [
    {
      type: "doc",
      id: "CURRICULUM_GUIDE",
      label: "🎯 Curriculum Guide",
    },
    {
      type: "category",
      label: "🧱 Phase 1: Foundations",
      collapsed: false,
      items: [
        {
          type: "category",
          label: "Lesson 1: Why Kafka?",
          items: ["modules/lesson_1/concept", "modules/lesson_1/event", "modules/lesson_1/workshop"],
        },
        {
          type: "category",
          label: "Lesson 2: Kafka Infrastructure",
          items: ["modules/lesson_2/concept"],
        },
        {
          type: "category",
          label: "Lesson 3: First Producer/Consumer",
          items: ["modules/lesson_3/producer", "modules/lesson_3/consumer", "modules/lesson_3/workshop"],
        },
        {
          type: "category",
          label: "Lesson 4: Topics & Partitions",
          items: [
            "modules/lesson_4/topic",
            "modules/lesson_4/partition",
            "modules/lesson_4/offset",
            "modules/lesson_4/broker",
            "modules/lesson_4/replication",
          ],
        },
        {
          type: "category",
          label: "Lesson 5: Kafka in Spring boot",
          items: [
            "modules/lesson_5/overview",
            "modules/lesson_5/configuration",
            "modules/lesson_5/serialize_deserializer",
            "modules/lesson_5/workshop",
          ],
        },
        {
          type: "category",
          label: "Lesson 6: Schema Registry",
          items: ["modules/lesson_6/concept", "modules/lesson_6/workshop"],
        },
        {
          type: "category",
          label: "Lesson 7: Development Tools",
          items: ["modules/lesson_7/concept", "modules/lesson_7/workshop"],
        },
      ],
    },
    {
      type: "category",
      label: "🛡️ Phase 2: Resilient Messaging",
      collapsed: true,
      items: [
        {
          type: "category",
          label: "Lesson 8: Consumer Groups",
          items: ["modules/lesson_8/concept", "workshop/lesson_8/README"],
        },
        {
          type: "category",
          label: "Lesson 9: Error Handling & DLT",
          items: ["modules/lesson_9/concept", "workshop/lesson_9/README"],
        },
        {
          type: "category",
          label: "Lesson 10: Exactly-Once Processing",
          items: ["modules/lesson_10/concept", "workshop/lesson_10/README"],
        },
        {
          type: "category",
          label: "Lesson 11: Message Transformation",
          items: ["modules/lesson_11/concept", "workshop/lesson_11/README"],
        },
        {
          type: "category",
          label: "Lesson 12: Fan-out Notifications",
          items: ["modules/lesson_12/concept", "workshop/lesson_12/README"],
        },
        {
          type: "category",
          label: "Lesson 13: Hybrid REST + Kafka",
          items: ["modules/lesson_13/concept", "workshop/lesson_13/README"],
        },
        {
          type: "category",
          label: "Lesson 14: Request-Reply Pattern",
          items: ["modules/lesson_14/concept", "workshop/lesson_14/README"],
        },
      ],
    },
    {
      type: "category",
      label: "🌊 Phase 3: Streaming & State",
      collapsed: true,
      items: [
        {
          type: "category",
          label: "Lesson 15: Kafka Streams Intro",
          items: ["modules/lesson_15/concept", "workshop/lesson_15/README"],
        },
        {
          type: "category",
          label: "Lesson 16: Windowing & Joins",
          items: ["modules/lesson_16/concept", "workshop/lesson_16/README"],
        },
        {
          type: "category",
          label: "Lesson 17: State Stores",
          items: ["modules/lesson_17/concept", "workshop/lesson_17/README"],
        },
        {
          type: "category",
          label: "Lesson 18: Real-time Dashboard",
          items: ["modules/lesson_18/concept", "workshop/lesson_18/README"],
        },
      ],
    },
    {
      type: "category",
      label: "🚀 Phase 4: Production",
      collapsed: true,
      items: [
        {
          type: "category",
          label: "Lesson 19: Security & ACLs",
          items: ["modules/lesson_19/concept", "workshop/lesson_19/README"],
        },
        {
          type: "category",
          label: "Lesson 20: Monitoring & Metrics",
          items: ["modules/lesson_20/concept", "workshop/lesson_20/README"],
        },
        {
          type: "category",
          label: "Lesson 21: Deployment & Scaling",
          items: ["modules/lesson_21/concept", "workshop/lesson_21/README"],
        },
      ],
    },
    {
      type: "doc",
      id: "modules/practical_usecases_guideline",
      label: "👍 Practical Use Cases",
    },
  ],
};

module.exports = sidebars;
