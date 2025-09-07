package com.learning.KafkaStarter.model

import com.fasterxml.jackson.annotation.JsonProperty
import java.time.Instant

data class StockPrice(
    @JsonProperty("symbol")
    val symbol: String,
    
    @JsonProperty("price")
    val price: Double,
    
    @JsonProperty("volume")
    val volume: Long,
    
    @JsonProperty("timestamp")
    val timestamp: Long = Instant.now().toEpochMilli()
)

data class TradingEvent(
    @JsonProperty("symbol")
    val symbol: String,
    
    @JsonProperty("tradeType")
    val tradeType: String, // BUY, SELL
    
    @JsonProperty("quantity")
    val quantity: Long,
    
    @JsonProperty("price")
    val price: Double,
    
    @JsonProperty("timestamp")
    val timestamp: Long = Instant.now().toEpochMilli()
)

data class MarketDataAggregate(
    @JsonProperty("symbol")
    val symbol: String = "",
    
    @JsonProperty("count")
    val count: Long = 0,
    
    @JsonProperty("totalVolume")
    val totalVolume: Long = 0,
    
    @JsonProperty("averagePrice")
    val averagePrice: Double = 0.0,
    
    @JsonProperty("minPrice")
    val minPrice: Double = Double.MAX_VALUE,
    
    @JsonProperty("maxPrice")
    val maxPrice: Double = Double.MIN_VALUE,
    
    @JsonProperty("windowStart")
    val windowStart: Long = 0,
    
    @JsonProperty("windowEnd")
    val windowEnd: Long = 0
)