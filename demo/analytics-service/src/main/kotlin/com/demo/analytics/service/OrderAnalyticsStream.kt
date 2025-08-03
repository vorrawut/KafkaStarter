package com.demo.analytics.service

import com.demo.analytics.model.*
import com.fasterxml.jackson.databind.ObjectMapper
import org.apache.kafka.common.serialization.Serdes
import org.apache.kafka.streams.StreamsBuilder
import org.apache.kafka.streams.kstream.*
import org.apache.kafka.streams.state.Stores
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.time.Duration
import java.time.Instant
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong

@Service
class OrderAnalyticsStream {
    
    @Autowired
    private lateinit var streamsBuilder: StreamsBuilder
    
    @Autowired
    private lateinit var objectMapper: ObjectMapper
    
    private val logger = org.slf4j.LoggerFactory.getLogger(OrderAnalyticsStream::class.java)
    
    // Real-time metrics (in memory for demo, use state stores in production)
    private val realTimeMetrics = ConcurrentHashMap<String, AtomicLong>()
    
    fun buildOrderAnalyticsTopology() {
        // Create order events stream
        val orderEvents: KStream<String, String> = streamsBuilder.stream("order-events")
        
        // Parse order events
        val parsedOrderEvents: KStream<String, OrderEvent> = orderEvents
            .mapValues { value ->
                try {
                    objectMapper.readValue(value, OrderEvent::class.java)
                } catch (e: Exception) {
                    logger.error("Failed to parse order event: $value", e)
                    null
                }
            }
            .filter { _, event -> event != null }
        
        // Real-time order count by status
        buildOrderCountByStatusStream(parsedOrderEvents)
        
        // Real-time revenue tracking
        buildRevenueTrackingStream(parsedOrderEvents)
        
        // Conversion funnel analysis
        buildConversionFunnelStream(parsedOrderEvents)
        
        // Order processing time analysis
        buildOrderProcessingTimeStream(parsedOrderEvents)
        
        // User behavior analytics
        buildUserBehaviorStream(parsedOrderEvents)
        
        logger.info("Order analytics topology built successfully")
    }
    
    private fun buildOrderCountByStatusStream(orderEvents: KStream<String, OrderEvent>) {
        // Count orders by status with 1-minute tumbling windows
        orderEvents
            .filter { _, event -> event.eventType == "STATUS_CHANGED" || event.eventType == "CREATED" }
            .groupBy { _, event -> event.status }
            .windowedBy(TimeWindows.of(Duration.ofMinutes(1)))
            .count()
            .toStream()
            .foreach { windowedKey, count ->
                val status = windowedKey.key()
                val windowStart = Instant.ofEpochMilli(windowedKey.window().start())
                val windowEnd = Instant.ofEpochMilli(windowedKey.window().end())
                
                logger.debug("Order count by status: status=$status, count=$count, " +
                    "window=$windowStart to $windowEnd")
                
                // Update real-time metrics
                realTimeMetrics.computeIfAbsent("orders_$status") { AtomicLong(0) }.set(count)
            }
    }
    
    private fun buildRevenueTrackingStream(orderEvents: KStream<String, OrderEvent>) {
        // Track revenue from paid orders
        orderEvents
            .filter { _, event -> event.status == "PAID" }
            .groupByKey()
            .windowedBy(TimeWindows.of(Duration.ofHours(1)))
            .aggregate(
                { BigDecimalSerde.zero() }, // initializer
                { _, event, aggregate ->
                    aggregate.add(event.totalAmount)
                }, // aggregator
                Materialized.`as`<String, BigDecimal>(Stores.persistentTimestampedWindowStore(
                    "revenue-store",
                    Duration.ofDays(7),
                    Duration.ofHours(1),
                    false
                )).withValueSerde(BigDecimalSerde())
            )
            .toStream()
            .foreach { windowedKey, revenue ->
                val hour = Instant.ofEpochMilli(windowedKey.window().start())
                
                logger.debug("Hourly revenue: revenue=$revenue, hour=$hour")
                
                // Update real-time metrics
                realTimeMetrics.computeIfAbsent("revenue_hourly") { AtomicLong(0) }
                    .set(revenue.longValueExact())
            }
    }
    
    private fun buildConversionFunnelStream(orderEvents: KStream<String, OrderEvent>) {
        // Track conversion funnel: CREATED -> PAID -> CONFIRMED
        orderEvents
            .groupByKey()
            .aggregate(
                { ConversionFunnelState() }, // initializer
                { _, event, state ->
                    when (event.status) {
                        "PENDING" -> state.copy(orderCreated = true)
                        "PAID" -> state.copy(paymentCompleted = true)
                        "CONFIRMED" -> state.copy(orderConfirmed = true)
                        "CANCELLED" -> state.copy(orderCancelled = true)
                        else -> state
                    }
                }, // aggregator
                Materialized.`as`<String, ConversionFunnelState>(Stores.persistentKeyValueStore(
                    "conversion-funnel-store"
                )).withValueSerde(ConversionFunnelSerde())
            )
            .toStream()
            .foreach { orderId, state ->
                // Calculate conversion rates
                if (state.orderCreated) {
                    realTimeMetrics.computeIfAbsent("funnel_created") { AtomicLong(0) }.incrementAndGet()
                }
                if (state.paymentCompleted) {
                    realTimeMetrics.computeIfAbsent("funnel_paid") { AtomicLong(0) }.incrementAndGet()
                }
                if (state.orderConfirmed) {
                    realTimeMetrics.computeIfAbsent("funnel_confirmed") { AtomicLong(0) }.incrementAndGet()
                }
                if (state.orderCancelled) {
                    realTimeMetrics.computeIfAbsent("funnel_cancelled") { AtomicLong(0) }.incrementAndGet()
                }
            }
    }
    
    private fun buildOrderProcessingTimeStream(orderEvents: KStream<String, OrderEvent>) {
        // Track time between order creation and confirmation
        orderEvents
            .groupByKey()
            .aggregate(
                { OrderProcessingState() }, // initializer
                { _, event, state ->
                    when (event.status) {
                        "PENDING" -> state.copy(createdAt = event.timestamp)
                        "CONFIRMED" -> state.copy(confirmedAt = event.timestamp)
                        else -> state
                    }
                }, // aggregator
                Materialized.`as`<String, OrderProcessingState>(Stores.persistentKeyValueStore(
                    "order-processing-store"
                )).withValueSerde(OrderProcessingSerde())
            )
            .toStream()
            .filter { _, state -> state.createdAt > 0 && state.confirmedAt > 0 }
            .mapValues { state -> state.confirmedAt - state.createdAt }
            .groupBy { _, _ -> "processing_time" }
            .windowedBy(TimeWindows.of(Duration.ofMinutes(5)))
            .aggregate(
                { ProcessingTimeStats() }, // initializer
                { _, processingTime, stats ->
                    stats.copy(
                        count = stats.count + 1,
                        totalTime = stats.totalTime + processingTime,
                        minTime = minOf(stats.minTime, processingTime),
                        maxTime = maxOf(stats.maxTime, processingTime)
                    )
                }, // aggregator
                Materialized.`as`<String, ProcessingTimeStats>(Stores.persistentTimestampedWindowStore(
                    "processing-time-stats-store",
                    Duration.ofHours(24),
                    Duration.ofMinutes(5),
                    false
                )).withValueSerde(ProcessingTimeStatsSerde())
            )
            .toStream()
            .foreach { windowedKey, stats ->
                val avgTime = if (stats.count > 0) stats.totalTime / stats.count else 0
                
                logger.debug("Order processing time stats: avg=${avgTime}ms, min=${stats.minTime}ms, " +
                    "max=${stats.maxTime}ms, count=${stats.count}")
                
                // Update real-time metrics
                realTimeMetrics.computeIfAbsent("avg_processing_time") { AtomicLong(0) }.set(avgTime)
                realTimeMetrics.computeIfAbsent("min_processing_time") { AtomicLong(0) }.set(stats.minTime)
                realTimeMetrics.computeIfAbsent("max_processing_time") { AtomicLong(0) }.set(stats.maxTime)
            }
    }
    
    private fun buildUserBehaviorStream(orderEvents: KStream<String, OrderEvent>) {
        // Track user order patterns
        orderEvents
            .filter { _, event -> event.eventType == "CREATED" }
            .groupBy { _, event -> event.userId.toString() }
            .windowedBy(TimeWindows.of(Duration.ofDays(1)))
            .count()
            .toStream()
            .groupBy { _, _ -> "user_orders_per_day" }
            .windowedBy(TimeWindows.of(Duration.ofDays(1)))
            .aggregate(
                { UserOrderStats() }, // initializer
                { _, ordersPerUser, stats ->
                    stats.copy(
                        totalUsers = stats.totalUsers + 1,
                        totalOrders = stats.totalOrders + ordersPerUser
                    )
                }, // aggregator
                Materialized.`as`<String, UserOrderStats>(Stores.persistentTimestampedWindowStore(
                    "user-order-stats-store",
                    Duration.ofDays(30),
                    Duration.ofDays(1),
                    false
                )).withValueSerde(UserOrderStatsSerde())
            )
            .toStream()
            .foreach { windowedKey, stats ->
                val avgOrdersPerUser = if (stats.totalUsers > 0) {
                    stats.totalOrders.toDouble() / stats.totalUsers
                } else 0.0
                
                logger.debug("User order stats: avgOrdersPerUser=$avgOrdersPerUser, " +
                    "totalUsers=${stats.totalUsers}, totalOrders=${stats.totalOrders}")
                
                // Update real-time metrics
                realTimeMetrics.computeIfAbsent("avg_orders_per_user") { AtomicLong(0) }
                    .set((avgOrdersPerUser * 100).toLong()) // Store as cents for precision
            }
    }
    
    // Public methods to access real-time metrics
    fun getRealTimeMetrics(): Map<String, Long> {
        return realTimeMetrics.mapValues { it.value.get() }
    }
    
    fun getMetric(key: String): Long {
        return realTimeMetrics[key]?.get() ?: 0L
    }
    
    fun resetMetrics() {
        realTimeMetrics.clear()
        logger.info("Real-time metrics reset")
    }
}

// Supporting data classes and serdes would be defined here
// (Simplified for demo - in real system, use proper Kafka Streams serdes)