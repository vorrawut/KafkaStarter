package com.learning.KafkaStarter.streams

import com.learning.KafkaStarter.model.StockPrice
import com.learning.KafkaStarter.model.TradingEvent
import org.apache.kafka.common.serialization.Serdes
import org.apache.kafka.streams.StreamsBuilder
import org.apache.kafka.streams.Topology
import org.apache.kafka.streams.kstream.*
import org.springframework.kafka.support.serializer.JsonSerde
import org.springframework.stereotype.Component
import java.time.Duration
import java.time.Instant

@Component
class JoinProcessor {
    
    fun buildStreamStreamJoinTopology(): Topology {
        val builder = StreamsBuilder()
        
        // Create source streams
        val stockPrices: KStream<String, StockPrice> = builder.stream(
            "stock-prices",
            Consumed.with(Serdes.String(), JsonSerde(StockPrice::class.java))
        )
        
        val tradingEvents: KStream<String, TradingEvent> = builder.stream(
            "trading-events", 
            Consumed.with(Serdes.String(), JsonSerde(TradingEvent::class.java))
        )
        
        // Re-key both streams by symbol for joining
        val stockPricesBySymbol = stockPrices.selectKey { _, price -> price.symbol }
        val tradingEventsBySymbol = tradingEvents.selectKey { _, trade -> trade.symbol }
        
        // Perform inner join within 1-minute window
        val joinedEvents = stockPricesBySymbol.join(
            tradingEventsBySymbol,
            ::joinStockPriceWithTrade,
            JoinWindows.of(Duration.ofMinutes(1)),
            StreamJoined.with(
                Serdes.String(),
                JsonSerde(StockPrice::class.java),
                JsonSerde(TradingEvent::class.java)
            )
        )
        
        // Output joined results
        joinedEvents.to(
            "joined-stock-trades",
            Produced.with(Serdes.String(), JsonSerde(JoinedStockTrade::class.java))
        )
        
        return builder.build()
    }
    
    fun buildStreamTableJoinTopology(): Topology {
        val builder = StreamsBuilder()
        
        // Create stream and table sources
        val tradingEvents: KStream<String, TradingEvent> = builder.stream(
            "trading-events",
            Consumed.with(Serdes.String(), JsonSerde(TradingEvent::class.java))
        )
        
        val stockPricesTable: KTable<String, StockPrice> = builder.table(
            "stock-prices-table",
            Consumed.with(Serdes.String(), JsonSerde(StockPrice::class.java))
        )
        
        // Re-key trading events by symbol for joining with table
        val tradingEventsBySymbol = tradingEvents.selectKey { _, trade -> trade.symbol }
        
        // Join stream with table for enrichment
        val enrichedTrades = tradingEventsBySymbol.join(
            stockPricesTable,
            ::enrichTradeWithCurrentPrice
        )
        
        // Output enriched results
        enrichedTrades.to(
            "enriched-trades",
            Produced.with(Serdes.String(), JsonSerde(EnrichedTrade::class.java))
        )
        
        return builder.build()
    }
    
    fun buildGlobalTableJoinTopology(): Topology {
        val builder = StreamsBuilder()
        
        // Create stream and global table
        val tradingEvents: KStream<String, TradingEvent> = builder.stream(
            "trading-events",
            Consumed.with(Serdes.String(), JsonSerde(TradingEvent::class.java))
        )
        
        val stockInfoGlobalTable: GlobalKTable<String, StockInfo> = builder.globalTable(
            "stock-info",
            Consumed.with(Serdes.String(), JsonSerde(StockInfo::class.java))
        )
        
        // Join with global table
        val enrichedWithGlobalData = tradingEvents.join(
            stockInfoGlobalTable,
            { tradingEventKey, tradingEvent -> tradingEvent.symbol }, // Key selector
            { tradingEvent, stockInfo ->
                GloballyEnrichedTrade(
                    tradingEvent = tradingEvent,
                    stockInfo = stockInfo,
                    enrichmentTimestamp = Instant.now().toEpochMilli()
                )
            }
        )
        
        // Output globally enriched results
        enrichedWithGlobalData.to(
            "globally-enriched-trades",
            Produced.with(Serdes.String(), JsonSerde(GloballyEnrichedTrade::class.java))
        )
        
        return builder.build()
    }
    
    private fun joinStockPriceWithTrade(price: StockPrice, trade: TradingEvent): JoinedStockTrade {
        return JoinedStockTrade(
            symbol = price.symbol,
            stockPrice = price,
            tradingEvent = trade,
            joinTimestamp = Instant.now().toEpochMilli(),
            priceVolatility = calculateVolatility(price, trade),
            marketImpact = calculateMarketImpact(price, trade)
        )
    }
    
    private fun enrichTradeWithCurrentPrice(trade: TradingEvent, currentPrice: StockPrice): EnrichedTrade {
        val priceImpact = ((trade.price - currentPrice.price) / currentPrice.price) * 100
        
        return EnrichedTrade(
            tradingEvent = trade,
            currentPrice = currentPrice,
            priceImpact = priceImpact,
            enrichmentTimestamp = Instant.now().toEpochMilli(),
            tradeType = if (priceImpact > 0) "POSITIVE_IMPACT" else "NEGATIVE_IMPACT",
            riskLevel = calculateRiskLevel(priceImpact)
        )
    }
    
    private fun calculateVolatility(price: StockPrice, trade: TradingEvent): Double {
        // Simple volatility calculation based on price difference
        return kotlin.math.abs(trade.price - price.price) / price.price
    }
    
    private fun calculateMarketImpact(price: StockPrice, trade: TradingEvent): String {
        val volumeRatio = trade.quantity.toDouble() / price.volume.toDouble()
        return when {
            volumeRatio > 0.1 -> "HIGH"
            volumeRatio > 0.05 -> "MEDIUM"
            else -> "LOW"
        }
    }
    
    private fun calculateRiskLevel(priceImpact: Double): String {
        return when {
            kotlin.math.abs(priceImpact) > 5.0 -> "HIGH"
            kotlin.math.abs(priceImpact) > 2.0 -> "MEDIUM"
            else -> "LOW"
        }
    }
}

data class StockInfo(
    val symbol: String,
    val companyName: String,
    val sector: String,
    val marketCap: Long
)

data class JoinedStockTrade(
    val symbol: String,
    val stockPrice: StockPrice,
    val tradingEvent: TradingEvent,
    val joinTimestamp: Long,
    val priceVolatility: Double,
    val marketImpact: String
)

data class EnrichedTrade(
    val tradingEvent: TradingEvent,
    val currentPrice: StockPrice,
    val priceImpact: Double,
    val enrichmentTimestamp: Long,
    val tradeType: String,
    val riskLevel: String
)

data class GloballyEnrichedTrade(
    val tradingEvent: TradingEvent,
    val stockInfo: StockInfo,
    val enrichmentTimestamp: Long
)