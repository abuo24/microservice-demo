package uz.coder.order_service.controller

import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import uz.coder.order_service.dto.OrderRequest
import uz.coder.order_service.dto.OrderResponse
import uz.coder.order_service.enumuration.OrderStatus
import uz.coder.order_service.service.OrderService
import java.util.UUID

@RestController
@RequestMapping("/api/orders")
class OrderController(private val orderService: OrderService) {

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
}