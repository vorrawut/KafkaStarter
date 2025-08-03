package com.demo.orderservice.service

import com.demo.orderservice.model.*
import com.demo.orderservice.repository.OrderRepository
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.*

@Service
@Transactional
class OrderService {
    
    @Autowired
    private lateinit var orderRepository: OrderRepository
    
    @Autowired
    private lateinit var orderEventPublisher: OrderEventPublisher
    
    @Autowired
    private lateinit var orderSagaOrchestrator: OrderSagaOrchestrator
    
    private val logger = org.slf4j.LoggerFactory.getLogger(OrderService::class.java)
    
    fun createOrder(request: CreateOrderRequest): OrderResponse {
        logger.info("Creating new order for user: ${request.userId}")
        
        // Generate order number
        val orderNumber = generateOrderNumber()
        
        // Calculate total amount
        val totalAmount = request.items.sumOf { it.unitPrice * it.quantity.toBigDecimal() }
        
        // Create order
        val order = Order(
            userId = request.userId,
            status = OrderStatus.PENDING,
            totalAmount = totalAmount,
            shippingAddress = request.shippingAddress,
            billingAddress = request.billingAddress,
            paymentMethod = request.paymentMethod,
            orderNumber = orderNumber,
            estimatedDelivery = Instant.now().plus(7, ChronoUnit.DAYS)
        )
        
        val savedOrder = orderRepository.save(order)
        
        // Create order items
        val orderItems = request.items.map { item ->
            OrderItem(
                orderId = savedOrder.id,
                productId = item.productId,
                productName = item.productName,
                quantity = item.quantity,
                unitPrice = item.unitPrice,
                totalPrice = item.unitPrice * item.quantity.toBigDecimal(),
                sku = item.sku
            )
        }
        
        // Save with items (in real system, would save items separately)
        val completeOrder = savedOrder.copy(items = orderItems)
        
        // Publish order created event
        val orderCreatedEvent = OrderCreatedEvent(
            eventId = "order-created-${UUID.randomUUID()}",
            orderId = savedOrder.id,
            orderNumber = savedOrder.orderNumber,
            userId = savedOrder.userId,
            items = orderItems.map { item ->
                OrderItemEvent(
                    productId = item.productId,
                    productName = item.productName,
                    quantity = item.quantity,
                    unitPrice = item.unitPrice,
                    totalPrice = item.totalPrice,
                    sku = item.sku
                )
            },
            totalAmount = savedOrder.totalAmount,
            currency = savedOrder.currency,
            shippingAddress = savedOrder.shippingAddress,
            billingAddress = savedOrder.billingAddress,
            paymentMethod = savedOrder.paymentMethod
        )
        
        orderEventPublisher.publishOrderCreated(orderCreatedEvent)
        
        // Publish general order event
        val orderEvent = OrderEvent(
            eventId = "order-event-${UUID.randomUUID()}",
            eventType = "CREATED",
            orderId = savedOrder.id,
            orderNumber = savedOrder.orderNumber,
            userId = savedOrder.userId,
            status = savedOrder.status.name,
            totalAmount = savedOrder.totalAmount,
            currency = savedOrder.currency,
            metadata = mapOf(
                "itemCount" to orderItems.size,
                "paymentMethod" to savedOrder.paymentMethod
            )
        )
        
        orderEventPublisher.publishOrderEvent(orderEvent)
        
        // Start order processing saga
        orderSagaOrchestrator.startOrderProcessingSaga(savedOrder, orderItems)
        
        logger.info("Order created successfully: orderNumber=${savedOrder.orderNumber}, " +
            "orderId=${savedOrder.id}, totalAmount=${savedOrder.totalAmount}")
        
        return OrderResponse(
            id = savedOrder.id,
            orderNumber = savedOrder.orderNumber,
            userId = savedOrder.userId,
            status = savedOrder.status,
            totalAmount = savedOrder.totalAmount,
            currency = savedOrder.currency,
            shippingAddress = savedOrder.shippingAddress,
            billingAddress = savedOrder.billingAddress,
            paymentMethod = savedOrder.paymentMethod,
            items = orderItems.map { item ->
                OrderItemResponse(
                    productId = item.productId,
                    productName = item.productName,
                    quantity = item.quantity,
                    unitPrice = item.unitPrice,
                    totalPrice = item.totalPrice,
                    sku = item.sku
                )
            },
            createdAt = savedOrder.createdAt,
            updatedAt = savedOrder.updatedAt,
            estimatedDelivery = savedOrder.estimatedDelivery
        )
    }
    
    fun updateOrderStatus(orderId: Long, newStatus: OrderStatus, reason: String? = null): OrderResponse {
        logger.info("Updating order status: orderId=$orderId, newStatus=$newStatus")
        
        val order = orderRepository.findById(orderId).orElse(null)
            ?: throw IllegalArgumentException("Order not found: $orderId")
        
        val previousStatus = order.status
        
        // Validate status transition
        if (!isValidStatusTransition(previousStatus, newStatus)) {
            throw IllegalArgumentException("Invalid status transition: $previousStatus -> $newStatus")
        }
        
        val updatedOrder = order.copy(
            status = newStatus,
            updatedAt = Instant.now()
        )
        
        orderRepository.save(updatedOrder)
        
        // Publish status changed event
        val statusChangedEvent = OrderStatusChangedEvent(
            eventId = "order-status-${UUID.randomUUID()}",
            orderId = updatedOrder.id,
            orderNumber = updatedOrder.orderNumber,
            userId = updatedOrder.userId,
            previousStatus = previousStatus.name,
            newStatus = newStatus.name,
            reason = reason
        )
        
        orderEventPublisher.publishOrderStatusChanged(statusChangedEvent)
        
        // Publish general order event
        val orderEvent = OrderEvent(
            eventId = "order-event-${UUID.randomUUID()}",
            eventType = "STATUS_CHANGED",
            orderId = updatedOrder.id,
            orderNumber = updatedOrder.orderNumber,
            userId = updatedOrder.userId,
            status = newStatus.name,
            totalAmount = updatedOrder.totalAmount,
            currency = updatedOrder.currency,
            metadata = mapOf(
                "previousStatus" to previousStatus.name,
                "reason" to (reason ?: "")
            )
        )
        
        orderEventPublisher.publishOrderEvent(orderEvent)
        
        logger.info("Order status updated successfully: orderId=$orderId, " +
            "previousStatus=$previousStatus, newStatus=$newStatus")
        
        return getOrderById(orderId)!!
    }
    
    fun cancelOrder(orderId: Long, reason: String): OrderResponse {
        logger.info("Cancelling order: orderId=$orderId, reason=$reason")
        
        val order = orderRepository.findById(orderId).orElse(null)
            ?: throw IllegalArgumentException("Order not found: $orderId")
        
        if (!canCancelOrder(order.status)) {
            throw IllegalArgumentException("Cannot cancel order in status: ${order.status}")
        }
        
        val cancelledOrder = order.copy(
            status = OrderStatus.CANCELLED,
            updatedAt = Instant.now()
        )
        
        orderRepository.save(cancelledOrder)
        
        // Publish order cancelled event
        val cancelledEvent = OrderCancelledEvent(
            eventId = "order-cancelled-${UUID.randomUUID()}",
            orderId = cancelledOrder.id,
            orderNumber = cancelledOrder.orderNumber,
            userId = cancelledOrder.userId,
            reason = reason,
            refundAmount = if (order.status in setOf(OrderStatus.PAID, OrderStatus.CONFIRMED)) {
                order.totalAmount
            } else {
                BigDecimal.ZERO
            }
        )
        
        orderEventPublisher.publishOrderCancelled(cancelledEvent)
        
        // Start cancellation saga to handle compensating transactions
        orderSagaOrchestrator.startOrderCancellationSaga(cancelledOrder, reason)
        
        logger.info("Order cancelled successfully: orderId=$orderId, reason=$reason")
        
        return getOrderById(orderId)!!
    }
    
    fun getOrderById(orderId: Long): OrderResponse? {
        val order = orderRepository.findById(orderId).orElse(null) ?: return null
        
        return OrderResponse(
            id = order.id,
            orderNumber = order.orderNumber,
            userId = order.userId,
            status = order.status,
            totalAmount = order.totalAmount,
            currency = order.currency,
            shippingAddress = order.shippingAddress,
            billingAddress = order.billingAddress,
            paymentMethod = order.paymentMethod,
            items = order.items.map { item ->
                OrderItemResponse(
                    productId = item.productId,
                    productName = item.productName,
                    quantity = item.quantity,
                    unitPrice = item.unitPrice,
                    totalPrice = item.totalPrice,
                    sku = item.sku
                )
            },
            createdAt = order.createdAt,
            updatedAt = order.updatedAt,
            estimatedDelivery = order.estimatedDelivery
        )
    }
    
    fun getOrderByNumber(orderNumber: String): OrderResponse? {
        val order = orderRepository.findByOrderNumber(orderNumber) ?: return null
        return getOrderById(order.id)
    }
    
    fun getOrdersByUserId(userId: Long): List<OrderResponse> {
        return orderRepository.findByUserIdOrderByCreatedAtDesc(userId).map { order ->
            getOrderById(order.id)!!
        }
    }
    
    fun getOrdersByStatus(status: OrderStatus): List<OrderResponse> {
        return orderRepository.findByStatusOrderByCreatedAtDesc(status).map { order ->
            getOrderById(order.id)!!
        }
    }
    
    // Utility methods
    private fun generateOrderNumber(): String {
        val timestamp = System.currentTimeMillis()
        val random = (1000..9999).random()
        return "ORD-$timestamp-$random"
    }
    
    private fun isValidStatusTransition(from: OrderStatus, to: OrderStatus): Boolean {
        val validTransitions = mapOf(
            OrderStatus.PENDING to setOf(OrderStatus.INVENTORY_RESERVED, OrderStatus.CANCELLED),
            OrderStatus.INVENTORY_RESERVED to setOf(OrderStatus.PAYMENT_PROCESSING, OrderStatus.CANCELLED),
            OrderStatus.PAYMENT_PROCESSING to setOf(OrderStatus.PAID, OrderStatus.PAYMENT_FAILED),
            OrderStatus.PAYMENT_FAILED to setOf(OrderStatus.PAYMENT_PROCESSING, OrderStatus.CANCELLED),
            OrderStatus.PAID to setOf(OrderStatus.CONFIRMED, OrderStatus.REFUNDED),
            OrderStatus.CONFIRMED to setOf(OrderStatus.PROCESSING, OrderStatus.CANCELLED),
            OrderStatus.PROCESSING to setOf(OrderStatus.SHIPPED, OrderStatus.CANCELLED),
            OrderStatus.SHIPPED to setOf(OrderStatus.DELIVERED),
            OrderStatus.DELIVERED to setOf(OrderStatus.REFUNDED),
            OrderStatus.CANCELLED to emptySet(),
            OrderStatus.REFUNDED to emptySet()
        )
        
        return validTransitions[from]?.contains(to) ?: false
    }
    
    private fun canCancelOrder(status: OrderStatus): Boolean {
        return status in setOf(
            OrderStatus.PENDING,
            OrderStatus.INVENTORY_RESERVED,
            OrderStatus.PAYMENT_FAILED,
            OrderStatus.PAID,
            OrderStatus.CONFIRMED,
            OrderStatus.PROCESSING
        )
    }
}