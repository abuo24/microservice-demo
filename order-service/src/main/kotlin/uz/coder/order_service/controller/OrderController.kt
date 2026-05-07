package uz.coder.order_service.controller

import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import uz.coder.order_service.client.AnnotationInventoryClient
import uz.coder.order_service.client.InventoryClient
import uz.coder.order_service.client.InventoryGrpcClient
import uz.coder.order_service.dto.OrderRequest
import uz.coder.order_service.dto.OrderResponse
import uz.coder.order_service.dto.StockCheckResponse
import uz.coder.order_service.enumuration.OrderStatus
import uz.coder.order_service.service.OrderService
import java.util.UUID

@RestController
@RequestMapping
class OrderController(
    private val orderService: OrderService,
    private val inventoryClient: InventoryClient,
    private val annotationInventoryClient: AnnotationInventoryClient,
    private val inventoryGrpcClient: InventoryGrpcClient
) {

    @GetMapping("/{id}")
    fun getOrder(@PathVariable id: UUID): ResponseEntity<OrderResponse> =
        ResponseEntity.ok(orderService.findById(id))

    @GetMapping("/customer/{customerId}")
    fun getOrdersByCustomer(@PathVariable customerId: String): ResponseEntity<List<OrderResponse>> =
        ResponseEntity.ok(orderService.findByCustomerId(customerId))

    @PostMapping
    fun createOrder(@RequestBody request: OrderRequest): ResponseEntity<OrderResponse> =
        ResponseEntity.status(HttpStatus.CREATED).body(orderService.createOrder(request))

    @PatchMapping("/{id}/status")
    fun updateStatus(
        @PathVariable id: UUID,
        @RequestParam status: OrderStatus
    ): ResponseEntity<OrderResponse> =
        ResponseEntity.ok(orderService.updateStatus(id, status))

    @DeleteMapping("/{id}")
    fun cancelOrder(@PathVariable id: UUID): ResponseEntity<OrderResponse> =
        ResponseEntity.ok(orderService.cancelOrder(id))

    // Circuit breaker CASE 1: calls real inventory-service — succeeds when inventory is UP
    @GetMapping("/stock/{productId}")
    fun checkStock(@PathVariable productId: String): ResponseEntity<StockCheckResponse> =
        ResponseEntity.ok(inventoryClient.checkStock(productId))

    // Circuit breaker CASE 2: calls non-existent host — always fails, circuit opens, fallback returns
    @GetMapping("/stock-broken/{productId}")
    fun checkStockBroken(@PathVariable productId: String): ResponseEntity<StockCheckResponse> =
        ResponseEntity.ok(inventoryClient.checkStockBroken(productId))

    // Circuit breaker CASE 3: @CircuitBreaker annotation, real inventory — circuit CLOSED, live data
    @GetMapping("/stock-annotation/{productId}")
    fun checkStockAnnotation(@PathVariable productId: String): ResponseEntity<StockCheckResponse> =
        ResponseEntity.ok(annotationInventoryClient.checkStock(productId))

    // Circuit breaker CASE 4: @CircuitBreaker annotation, broken host — circuit OPEN, fallback
    @GetMapping("/stock-annotation-broken/{productId}")
    fun checkStockAnnotationBroken(@PathVariable productId: String): ResponseEntity<StockCheckResponse> =
        ResponseEntity.ok(annotationInventoryClient.checkStockBroken(productId))

    // gRPC: calls inventory-service via gRPC on port 9090 (binary protocol, low latency)
    @GetMapping("/stock-grpc/{productId}")
    fun checkStockGrpc(@PathVariable productId: String): ResponseEntity<StockCheckResponse> =
        ResponseEntity.ok(inventoryGrpcClient.checkStock(productId))
}