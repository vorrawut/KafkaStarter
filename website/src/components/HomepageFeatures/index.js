import React from 'react';
import clsx from 'clsx';
import styles from './styles.module.css';

const FeatureList = [
  {
    title: '🧱 Phase 1: Foundations',
    description: (
      <>
        Master Kafka fundamentals with hands-on Spring Boot + Kotlin development.
        Learn producers, consumers, topics, partitions, schema management, and development tools.
      </>
    ),
  },
  {
    title: '🛡️ Phase 2: Resilient Messaging',
    description: (
      <>
        Build robust event-driven systems with consumer groups, error handling, 
        exactly-once processing, message transformation, and hybrid architectures.
      </>
    ),
  },
  {
    title: '🌊 Phase 3: Streaming & State',
    description: (
      <>
        Create real-time applications with Kafka Streams, windowing, joins, 
        state stores, and interactive dashboards for live data processing.
      </>
    ),
  },
  {
    title: '🚀 Phase 4: Production',
    description: (
      <>
        Deploy enterprise-ready systems with security, comprehensive monitoring, 
        auto-scaling, and operational excellence best practices.
      </>
    ),
  },
  {
    title: '🎨 60+ Mermaid Diagrams',
    description: (
      <>
        Beautiful visual learning aids that make complex Kafka concepts 
        easy to understand with architecture diagrams and flow charts.
      </>
    ),
  },
  {
    title: '🎪 Real-World Demo',
    description: (
      <>
        Complete e-commerce application showcasing all patterns with 
        microservices, real-time analytics, and production deployment.
      </>
    ),
  },
];

function Feature({title, description}) {
  return (
    <div className={clsx('col col--4')}>
      <div className="text--center">
        <div className={styles.featureIcon}>
          {title.split(' ')[0]}
        </div>
      </div>
      <div className="text--center padding-horiz--md">
        <h3>{title}</h3>
        <p>{description}</p>
      </div>
    </div>
  );
}

export default function HomepageFeatures() {
  return (
    <section className={styles.features}>
      <div className="container">
        <div className="row">
          <div className="col col--12">
            <div className="text--center">
              <h2>🏆 The Most Comprehensive Kafka Learning Experience Ever Created</h2>
              <p className="hero__subtitle">
                20 complete lessons • 47 markdown files • Production-ready patterns • 100% validated curriculum
              </p>
            </div>
          </div>
        </div>
        <div className="row">
          {FeatureList.map((props, idx) => (
            <Feature key={idx} {...props} />
          ))}
        </div>
        <div className="row">
          <div className="col col--12">
            <div className="text--center margin-top--lg">
              <div className={styles.statsContainer}>
                <div className={styles.stat}>
                  <h3>📚 20</h3>
                  <p>Progressive Lessons</p>
                </div>
                <div className={styles.stat}>
                  <h3>🎨 60+</h3>
                  <p>Mermaid Diagrams</p>
                </div>
                <div className={styles.stat}>
                  <h3>✅ 91/91</h3>
                  <p>Validation Checks Passed</p>
                </div>
                <div className={styles.stat}>
                  <h3>🚀 100%</h3>
                  <p>Curriculum Complete</p>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </section>
  );
}