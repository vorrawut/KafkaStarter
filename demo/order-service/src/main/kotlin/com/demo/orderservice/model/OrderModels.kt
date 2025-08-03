package com.demo.orderservice.model

import com.fasterxml.jackson.annotation.JsonProperty
import java.math.BigDecimal
import java.time.Instant
import javax.persistence.*

@Entity
@Table(name = "orders")
data class Order(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,
    
    val userId: Long,
    
    @Enumerated(EnumType.STRING)
    val status: OrderStatus = OrderStatus.PENDING,
    
    val totalAmount: BigDecimal,
    
    val currency: String = "USD",
    
    val shippingAddress: String,
    
    val billingAddress: String,
    
    val paymentMethod: String,
    
    val createdAt: Instant = Instant.now(),
    
    val updatedAt: Instant = Instant.now(),
    
    val estimatedDelivery: Instant? = null,
    
    val orderNumber: String,
    
    @OneToMany(mappedBy = "orderId", cascade = [CascadeType.ALL], fetch = FetchType.LAZY)
    val items: List<OrderItem> = emptyList()
)

@Entity
@Table(name = "order_items")
data class OrderItem(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,
    
    val orderId: Long,
    
    val productId: Long,
    
    val productName: String,
    
    val quantity: Int,
    
    val unitPrice: BigDecimal,
    
    val totalPrice: BigDecimal,
    
    val sku: String
)

enum class OrderStatus {
    PENDING,           // Order created, awaiting inventory check
    INVENTORY_RESERVED, // Inventory reserved, awaiting payment
    PAYMENT_PROCESSING, // Payment being processed
    PAID,              // Payment successful
    PAYMENT_FAILED,    // Payment failed
    CONFIRMED,         // Order confirmed, ready for fulfillment
    PROCESSING,        // Order being prepared
    SHIPPED,           // Order shipped
    DELIVERED,         // Order delivered
    CANCELLED,         // Order cancelled
    REFUNDED           // Order refunded
}

// Kafka Event Models
data class OrderEvent(
    @JsonProperty("eventId")
    val eventId: String,
    
    @JsonProperty("eventType")
    val eventType: String, // CREATED, STATUS_CHANGED, ITEM_ADDED, CANCELLED
    
    @JsonProperty("orderId")
    val orderId: Long,
    
    @JsonProperty("orderNumber")
    val orderNumber: String,
    
    @JsonProperty("userId")
    val userId: Long,
    
    @JsonProperty("status")
    val status: String,
    
    @JsonProperty("totalAmount")
    val totalAmount: BigDecimal,
    
    @JsonProperty("currency")
    val currency: String = "USD",
    
    @JsonProperty("metadata")
    val metadata: Map<String, Any> = emptyMap(),
    
    @JsonProperty("timestamp")
    val timestamp: Long = Instant.now().toEpochMilli(),
    
    @JsonProperty("version")
    val version: String = "1.0"
)

data class OrderCreatedEvent(
    @JsonProperty("eventId")
    val eventId: String,
    
    @JsonProperty("orderId")
    val orderId: Long,
    
    @JsonProperty("orderNumber")
    val orderNumber: String,
    
    @JsonProperty("userId")
    val userId: Long,
    
    @JsonProperty("items")
    val items: List<OrderItemEvent>,
    
    @JsonProperty("totalAmount")
    val totalAmount: BigDecimal,
    
    @JsonProperty("currency")
    val currency: String = "USD",
    
    @JsonProperty("shippingAddress")
    val shippingAddress: String,
    
    @JsonProperty("billingAddress")
    val billingAddress: String,
    
    @JsonProperty("paymentMethod")
    val paymentMethod: String,
    
    @JsonProperty("timestamp")
    val timestamp: Long = Instant.now().toEpochMilli()
)

data class OrderItemEvent(
    @JsonProperty("productId")
    val productId: Long,
    
    @JsonProperty("productName")
    val productName: String,
    
    @JsonProperty("quantity")
    val quantity: Int,
    
    @JsonProperty("unitPrice")
    val unitPrice: BigDecimal,
    
    @JsonProperty("totalPrice")
    val totalPrice: BigDecimal,
    
    @JsonProperty("sku")
    val sku: String
)

data class OrderStatusChangedEvent(
    @JsonProperty("eventId")
    val eventId: String,
    
    @JsonProperty("orderId")
    val orderId: Long,
    
    @JsonProperty("orderNumber")
    val orderNumber: String,
    
    @JsonProperty("userId")
    val userId: Long,
    
    @JsonProperty("previousStatus")
    val previousStatus: String,
    
    @JsonProperty("newStatus")
    val newStatus: String,
    
    @JsonProperty("reason")
    val reason: String? = null,
    
    @JsonProperty("timestamp")
    val timestamp: Long = Instant.now().toEpochMilli()
)

data class OrderCancelledEvent(
    @JsonProperty("eventId")
    val eventId: String,
    
    @JsonProperty("orderId")
    val orderId: Long,
    
    @JsonProperty("orderNumber")
    val orderNumber: String,
    
    @JsonProperty("userId")
    val userId: Long,
    
    @JsonProperty("reason")
    val reason: String,
    
    @JsonProperty("refundAmount")
    val refundAmount: BigDecimal,
    
    @JsonProperty("timestamp")
    val timestamp: Long = Instant.now().toEpochMilli()
)

// Saga Coordination Events
data class InventoryReservationRequest(
    @JsonProperty("correlationId")
    val correlationId: String,
    
    @JsonProperty("orderId")
    val orderId: Long,
    
    @JsonProperty("orderNumber")
    val orderNumber: String,
    
    @JsonProperty("items")
    val items: List<InventoryItemRequest>,
    
    @JsonProperty("timestamp")
    val timestamp: Long = Instant.now().toEpochMilli()
)

data class InventoryItemRequest(
    @JsonProperty("productId")
    val productId: Long,
    
    @JsonProperty("sku")
    val sku: String,
    
    @JsonProperty("quantity")
    val quantity: Int
)

data class PaymentProcessingRequest(
    @JsonProperty("correlationId")
    val correlationId: String,
    
    @JsonProperty("orderId")
    val orderId: Long,
    
    @JsonProperty("orderNumber")
    val orderNumber: String,
    
    @JsonProperty("userId")
    val userId: Long,
    
    @JsonProperty("amount")
    val amount: BigDecimal,
    
    @JsonProperty("currency")
    val currency: String = "USD",
    
    @JsonProperty("paymentMethod")
    val paymentMethod: String,
    
    @JsonProperty("timestamp")
    val timestamp: Long = Instant.now().toEpochMilli()
)

// DTOs
data class CreateOrderRequest(
    val userId: Long,
    val items: List<CreateOrderItemRequest>,
    val shippingAddress: String,
    val billingAddress: String,
    val paymentMethod: String
)

data class CreateOrderItemRequest(
    val productId: Long,
    val productName: String,
    val quantity: Int,
    val unitPrice: BigDecimal,
    val sku: String
)

data class OrderResponse(
    val id: Long,
    val orderNumber: String,
    val userId: Long,
    val status: OrderStatus,
    val totalAmount: BigDecimal,
    val currency: String,
    val shippingAddress: String,
    val billingAddress: String,
    val paymentMethod: String,
    val items: List<OrderItemResponse>,
    val createdAt: Instant,
    val updatedAt: Instant,
    val estimatedDelivery: Instant?
)

data class OrderItemResponse(
    val productId: Long,
    val productName: String,
    val quantity: Int,
    val unitPrice: BigDecimal,
    val totalPrice: BigDecimal,
    val sku: String
)