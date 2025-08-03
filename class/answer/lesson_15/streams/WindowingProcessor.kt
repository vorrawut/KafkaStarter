package com.learning.KafkaStarter.streams

import com.learning.KafkaStarter.model.StockPrice
import com.learning.KafkaStarter.model.TradingEvent
import com.learning.KafkaStarter.model.MarketDataAggregate
import org.apache.kafka.common.serialization.Serdes
import org.apache.kafka.streams.KeyValue
import org.apache.kafka.streams.StreamsBuilder
import org.apache.kafka.streams.Topology
import org.apache.kafka.streams.kstream.*
import org.springframework.kafka.support.serializer.JsonSerde
import org.springframework.stereotype.Component
import java.time.Duration

@Component
class WindowingProcessor {
    
    fun buildTumblingWindowTopology(): Topology {
        val builder = StreamsBuilder()
        
        // Create source stream for stock prices
        val stockPrices: KStream<String, StockPrice> = builder.stream(
            "stock-prices",
            Consumed.with(Serdes.String(), JsonSerde(StockPrice::class.java))
        )
        
        // Group by stock symbol for windowed aggregation
        val stocksBySymbol = stockPrices
            .selectKey { _, price -> price.symbol }
            .groupByKey(Grouped.with(Serdes.String(), JsonSerde(StockPrice::class.java)))
        
        // Create 1-minute tumbling windows
        val minutelyAggregates = stocksBySymbol
            .windowedBy(TimeWindows.of(Duration.ofMinutes(1)))
            .aggregate(
                { MarketDataAggregate() },
                aggregateStockData(),
                Named.`as`("minutely-aggregates"),
                Materialized.`as`<String, MarketDataAggregate, WindowStore<org.apache.kafka.common.utils.Bytes, ByteArray>>("minutely-stock-store")
                    .withKeySerde(Serdes.String())
                    .withValueSerde(JsonSerde(MarketDataAggregate::class.java))
                    .withRetention(Duration.ofHours(1))
            )
        
        // Convert windowed KTable to stream and output
        minutelyAggregates.toStream()
            .map { windowedKey, aggregate ->
                KeyValue(
                    "${windowedKey.key()}-${windowedKey.window().start()}",
                    aggregate.copy(
                        windowStart = windowedKey.window().start(),
                        windowEnd = windowedKey.window().end()
                    )
                )
            }
            .to("minutely-aggregates", Produced.with(Serdes.String(), JsonSerde(MarketDataAggregate::class.java)))
        
        return builder.build()
    }
    
    fun buildHoppingWindowTopology(): Topology {
        val builder = StreamsBuilder()
        
        // Create source stream for stock prices
        val stockPrices: KStream<String, StockPrice> = builder.stream(
            "stock-prices",
            Consumed.with(Serdes.String(), JsonSerde(StockPrice::class.java))
        )
        
        // Group by stock symbol
        val stocksBySymbol = stockPrices
            .selectKey { _, price -> price.symbol }
            .groupByKey()
        
        // Create 5-minute hopping windows that advance every 1 minute
        val hoppingAggregates = stocksBySymbol
            .windowedBy(
                TimeWindows.of(Duration.ofMinutes(5))
                    .advanceBy(Duration.ofMinutes(1))
            )
            .aggregate(
                { MarketDataAggregate() },
                aggregateStockData(),
                Named.`as`("hopping-aggregates"),
                Materialized.`as`<String, MarketDataAggregate, WindowStore<org.apache.kafka.common.utils.Bytes, ByteArray>>("hopping-stock-store")
                    .withRetention(Duration.ofHours(2))
            )
        
        // Output hopping window results
        hoppingAggregates.toStream()
            .map { windowedKey, aggregate ->
                KeyValue(
                    "${windowedKey.key()}-${windowedKey.window().start()}-${windowedKey.window().end()}",
                    aggregate.copy(
                        windowStart = windowedKey.window().start(),
                        windowEnd = windowedKey.window().end()
                    )
                )
            }
            .to("hopping-aggregates", Produced.with(Serdes.String(), JsonSerde(MarketDataAggregate::class.java)))
        
        return builder.build()
    }
    
    fun buildSessionWindowTopology(): Topology {
        val builder = StreamsBuilder()
        
        // Create source stream for trading events
        val tradingEvents: KStream<String, TradingEvent> = builder.stream(
            "trading-events",
            Consumed.with(Serdes.String(), JsonSerde(TradingEvent::class.java))
        )
        
        // Group by symbol for session windowing
        val tradingBySymbol = tradingEvents
            .selectKey { _, trade -> trade.symbol }
            .groupByKey()
        
        // Create session windows with 10-minute inactivity gap
        val sessionAggregates = tradingBySymbol
            .windowedBy(SessionWindows.with(Duration.ofMinutes(10)))
            .aggregate(
                { MarketDataAggregate() },
                aggregateTradingData(),
                { agg1, agg2 -> // Session merger
                    MarketDataAggregate(
                        symbol = agg1.symbol,
                        count = agg1.count + agg2.count,
                        totalVolume = agg1.totalVolume + agg2.totalVolume,
                        averagePrice = ((agg1.averagePrice * agg1.count) + (agg2.averagePrice * agg2.count)) / (agg1.count + agg2.count),
                        minPrice = minOf(agg1.minPrice, agg2.minPrice),
                        maxPrice = maxOf(agg1.maxPrice, agg2.maxPrice)
                    )
                },
                Named.`as`("session-aggregates"),
                Materialized.with(Serdes.String(), JsonSerde(MarketDataAggregate::class.java))
            )
        
        // Output session results
        sessionAggregates.toStream()
            .map { windowedKey, aggregate ->
                KeyValue(
                    "${windowedKey.key()}-session-${windowedKey.window().start()}",
                    aggregate.copy(
                        windowStart = windowedKey.window().start(),
                        windowEnd = windowedKey.window().end()
                    )
                )
            }
            .to("session-aggregates", Produced.with(Serdes.String(), JsonSerde(MarketDataAggregate::class.java)))
        
        return builder.build()
    }
    
    private fun aggregateStockData(): Aggregator<String, StockPrice, MarketDataAggregate> {
        return Aggregator { symbol, price, aggregate ->
            val newCount = aggregate.count + 1
            val newTotalVolume = aggregate.totalVolume + price.volume
            val newAveragePrice = ((aggregate.averagePrice * aggregate.count) + price.price) / newCount
            
            MarketDataAggregate(
                symbol = symbol,
                count = newCount,
                totalVolume = newTotalVolume,
                averagePrice = newAveragePrice,
                minPrice = if (aggregate.minPrice == Double.MAX_VALUE) price.price else minOf(aggregate.minPrice, price.price),
                maxPrice = if (aggregate.maxPrice == Double.MIN_VALUE) price.price else maxOf(aggregate.maxPrice, price.price)
            )
        }
    }
    
    private fun aggregateTradingData(): Aggregator<String, TradingEvent, MarketDataAggregate> {
        return Aggregator { symbol, trade, aggregate ->
            val newCount = aggregate.count + 1
            val newTotalVolume = aggregate.totalVolume + trade.quantity
            val newAveragePrice = ((aggregate.averagePrice * aggregate.count) + trade.price) / newCount
            
            MarketDataAggregate(
                symbol = symbol,
                count = newCount,
                totalVolume = newTotalVolume,
                averagePrice = newAveragePrice,
                minPrice = if (aggregate.minPrice == Double.MAX_VALUE) trade.price else minOf(aggregate.minPrice, trade.price),
                maxPrice = if (aggregate.maxPrice == Double.MIN_VALUE) trade.price else maxOf(aggregate.maxPrice, trade.price)
            )
        }
    }
}