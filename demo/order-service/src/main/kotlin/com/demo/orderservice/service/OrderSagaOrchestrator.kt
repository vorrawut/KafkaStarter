package com.demo.orderservice.service

import com.demo.orderservice.model.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.util.*

@Service
class OrderSagaOrchestrator {
    
    @Autowired
    private lateinit var orderEventPublisher: OrderEventPublisher
    
    @Autowired
    private lateinit var orderService: OrderService
    
    private val logger = org.slf4j.LoggerFactory.getLogger(OrderSagaOrchestrator::class.java)
    
    // Saga state tracking (in real system, use persistent saga store)
    private val sagaStates = mutableMapOf<String, SagaState>()
    
    fun startOrderProcessingSaga(order: Order, items: List<OrderItem>) {
        val correlationId = "saga-${order.orderNumber}-${UUID.randomUUID()}"
        
        logger.info("Starting order processing saga: correlationId=$correlationId, orderId=${order.id}")
        
        val sagaState = SagaState(
            correlationId = correlationId,
            orderId = order.id,
            orderNumber = order.orderNumber,
            currentStep = SagaStep.INVENTORY_RESERVATION,
            status = SagaStatus.IN_PROGRESS,
            compensations = mutableListOf()
        )
        
        sagaStates[correlationId] = sagaState
        
        // Step 1: Reserve inventory
        reserveInventory(correlationId, order, items)
    }
    
    private fun reserveInventory(correlationId: String, order: Order, items: List<OrderItem>) {
        logger.info("Saga step - Reserve inventory: correlationId=$correlationId")
        
        val inventoryRequest = InventoryReservationRequest(
            correlationId = correlationId,
            orderId = order.id,
            orderNumber = order.orderNumber,
            items = items.map { item ->
                InventoryItemRequest(
                    productId = item.productId,
                    sku = item.sku,
                    quantity = item.quantity
                )
            }
        )
        
        orderEventPublisher.publishInventoryReservationRequest(inventoryRequest)
        
        // Add compensation action
        val sagaState = sagaStates[correlationId]!!
        sagaState.compensations.add(CompensationAction.RELEASE_INVENTORY)
    }
    
    fun handleInventoryReservationSuccess(correlationId: String, orderId: Long) {
        logger.info("Inventory reservation successful: correlationId=$correlationId, orderId=$orderId")
        
        val sagaState = sagaStates[correlationId] ?: return
        
        // Update order status
        orderService.updateOrderStatus(orderId, OrderStatus.INVENTORY_RESERVED, "Inventory reserved successfully")
        
        // Move to next step: Process payment
        sagaState.currentStep = SagaStep.PAYMENT_PROCESSING
        processPayment(correlationId, orderId)
    }
    
    fun handleInventoryReservationFailure(correlationId: String, orderId: Long, reason: String) {
        logger.warn("Inventory reservation failed: correlationId=$correlationId, orderId=$orderId, reason=$reason")
        
        val sagaState = sagaStates[correlationId] ?: return
        
        // Update order status and end saga
        orderService.updateOrderStatus(orderId, OrderStatus.CANCELLED, "Inventory reservation failed: $reason")
        
        sagaState.status = SagaStatus.FAILED
        sagaState.failureReason = reason
        
        logger.info("Order processing saga failed: correlationId=$correlationId, reason=$reason")
    }
    
    private fun processPayment(correlationId: String, orderId: Long) {
        logger.info("Saga step - Process payment: correlationId=$correlationId")
        
        val order = orderService.getOrderById(orderId) ?: return
        
        val paymentRequest = PaymentProcessingRequest(
            correlationId = correlationId,
            orderId = order.id,
            orderNumber = order.orderNumber,
            userId = order.userId,
            amount = order.totalAmount,
            currency = order.currency,
            paymentMethod = order.paymentMethod
        )
        
        orderEventPublisher.publishPaymentProcessingRequest(paymentRequest)
        
        // Update order status
        orderService.updateOrderStatus(orderId, OrderStatus.PAYMENT_PROCESSING, "Payment processing started")
        
        // Add compensation action
        val sagaState = sagaStates[correlationId]!!
        sagaState.compensations.add(CompensationAction.REFUND_PAYMENT)
    }
    
    fun handlePaymentSuccess(correlationId: String, orderId: Long) {
        logger.info("Payment successful: correlationId=$correlationId, orderId=$orderId")
        
        val sagaState = sagaStates[correlationId] ?: return
        
        // Update order status
        orderService.updateOrderStatus(orderId, OrderStatus.PAID, "Payment processed successfully")
        
        // Move to final step: Confirm order
        sagaState.currentStep = SagaStep.ORDER_CONFIRMATION
        confirmOrder(correlationId, orderId)
    }
    
    fun handlePaymentFailure(correlationId: String, orderId: Long, reason: String) {
        logger.warn("Payment failed: correlationId=$correlationId, orderId=$orderId, reason=$reason")
        
        val sagaState = sagaStates[correlationId] ?: return
        
        // Update order status
        orderService.updateOrderStatus(orderId, OrderStatus.PAYMENT_FAILED, "Payment failed: $reason")
        
        // Execute compensations
        executeCompensations(correlationId, orderId)
        
        sagaState.status = SagaStatus.COMPENSATED
        sagaState.failureReason = reason
        
        logger.info("Order processing saga compensated: correlationId=$correlationId, reason=$reason")
    }
    
    private fun confirmOrder(correlationId: String, orderId: Long) {
        logger.info("Saga step - Confirm order: correlationId=$correlationId")
        
        val sagaState = sagaStates[correlationId] ?: return
        
        // Update order status
        orderService.updateOrderStatus(orderId, OrderStatus.CONFIRMED, "Order confirmed and ready for processing")
        
        // Saga completed successfully
        sagaState.status = SagaStatus.COMPLETED
        sagaState.currentStep = SagaStep.COMPLETED
        
        logger.info("Order processing saga completed successfully: correlationId=$correlationId, orderId=$orderId")
    }
    
    fun startOrderCancellationSaga(order: Order, reason: String) {
        val correlationId = "cancel-saga-${order.orderNumber}-${UUID.randomUUID()}"
        
        logger.info("Starting order cancellation saga: correlationId=$correlationId, orderId=${order.id}")
        
        val sagaState = SagaState(
            correlationId = correlationId,
            orderId = order.id,
            orderNumber = order.orderNumber,
            currentStep = SagaStep.ORDER_CANCELLATION,
            status = SagaStatus.IN_PROGRESS,
            compensations = mutableListOf()
        )
        
        sagaStates[correlationId] = sagaState
        
        // Execute compensations based on order status
        executeCompensations(correlationId, order.id)
        
        sagaState.status = SagaStatus.COMPLETED
        logger.info("Order cancellation saga completed: correlationId=$correlationId")
    }
    
    private fun executeCompensations(correlationId: String, orderId: Long) {
        logger.info("Executing compensations: correlationId=$correlationId")
        
        val sagaState = sagaStates[correlationId] ?: return
        
        // Execute compensations in reverse order
        sagaState.compensations.reversed().forEach { compensation ->
            when (compensation) {
                CompensationAction.RELEASE_INVENTORY -> {
                    logger.info("Compensation: Releasing inventory for order $orderId")
                    // In real system, send inventory release event
                    orderEventPublisher.publishInventoryReleaseRequest(correlationId, orderId)
                }
                CompensationAction.REFUND_PAYMENT -> {
                    logger.info("Compensation: Refunding payment for order $orderId")
                    // In real system, send payment refund event
                    orderEventPublisher.publishPaymentRefundRequest(correlationId, orderId)
                }
            }
        }
    }
    
    fun getSagaState(correlationId: String): SagaState? {
        return sagaStates[correlationId]
    }
    
    fun getAllActiveSagas(): List<SagaState> {
        return sagaStates.values.filter { it.status == SagaStatus.IN_PROGRESS }
    }
}

data class SagaState(
    val correlationId: String,
    val orderId: Long,
    val orderNumber: String,
    var currentStep: SagaStep,
    var status: SagaStatus,
    val compensations: MutableList<CompensationAction>,
    var failureReason: String? = null
)

enum class SagaStep {
    INVENTORY_RESERVATION,
    PAYMENT_PROCESSING,
    ORDER_CONFIRMATION,
    ORDER_CANCELLATION,
    COMPLETED
}

enum class SagaStatus {
    IN_PROGRESS,
    COMPLETED,
    FAILED,
    COMPENSATED
}

enum class CompensationAction {
    RELEASE_INVENTORY,
    REFUND_PAYMENT
}