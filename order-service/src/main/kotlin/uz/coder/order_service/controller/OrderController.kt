package uz.coder.order_service.controller

import org.slf4j.LoggerFactory
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
    private val log = LoggerFactory.getLogger(javaClass)

    @GetMapping("/{id}")
    fun getOrder(@PathVariable id: UUID): ResponseEntity<OrderResponse> {
        log.debug("GET order id={}", id)
        return ResponseEntity.ok(orderService.findById(id))
    }

    @GetMapping("/customer/{customerId}")
    fun getOrdersByCustomer(@PathVariable customerId: String): ResponseEntity<List<OrderResponse>> {
        log.debug("GET orders customerId={}", customerId)
        return ResponseEntity.ok(orderService.findByCustomerId(customerId))
    }

    @PostMapping
    fun createOrder(@RequestBody request: OrderRequest): ResponseEntity<OrderResponse> {
        log.info("POST createOrder customerId={} items={}", request.customerId, request.items.size)
        val response = orderService.createOrder(request)
        log.info("Order created id={} totalAmount={}", response.id, response.totalAmount)
        return ResponseEntity.status(HttpStatus.CREATED).body(response)
    }

    @PatchMapping("/{id}/status")
    fun updateStatus(
        @PathVariable id: UUID,
        @RequestParam status: OrderStatus
    ): ResponseEntity<OrderResponse> {
        log.info("PATCH order status id={} status={}", id, status)
        return ResponseEntity.ok(orderService.updateStatus(id, status))
    }

    @DeleteMapping("/{id}")
    fun cancelOrder(@PathVariable id: UUID): ResponseEntity<OrderResponse> {
        log.info("DELETE (cancel) order id={}", id)
        return ResponseEntity.ok(orderService.cancelOrder(id))
    }

    @GetMapping("/stock/{productId}")
    fun checkStock(@PathVariable productId: String): ResponseEntity<StockCheckResponse> {
        log.debug("GET stock productId={} via HTTP circuit-breaker", productId)
        return ResponseEntity.ok(inventoryClient.checkStock(productId))
    }

    @GetMapping("/stock-broken/{productId}")
    fun checkStockBroken(@PathVariable productId: String): ResponseEntity<StockCheckResponse> {
        log.debug("GET stock-broken productId={} (broken host test)", productId)
        return ResponseEntity.ok(inventoryClient.checkStockBroken(productId))
    }

    @GetMapping("/stock-annotation/{productId}")
    fun checkStockAnnotation(@PathVariable productId: String): ResponseEntity<StockCheckResponse> {
        log.debug("GET stock-annotation productId={}", productId)
        return ResponseEntity.ok(annotationInventoryClient.checkStock(productId))
    }

    @GetMapping("/stock-annotation-broken/{productId}")
    fun checkStockAnnotationBroken(@PathVariable productId: String): ResponseEntity<StockCheckResponse> {
        log.debug("GET stock-annotation-broken productId={} (broken host test)", productId)
        return ResponseEntity.ok(annotationInventoryClient.checkStockBroken(productId))
    }

    @GetMapping("/stock-grpc/{productId}")
    fun checkStockGrpc(@PathVariable productId: String): ResponseEntity<StockCheckResponse> {
        log.debug("GET stock-grpc productId={} via gRPC", productId)
        return ResponseEntity.ok(inventoryGrpcClient.checkStock(productId))
    }
}