package uz.coder.api_gateway.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ServerWebExchange
import reactor.core.publisher.Mono
import uz.coder.api_gateway.model.OrderWithInventoryComposition
import uz.coder.api_gateway.service.OrderCompositionService
import java.util.UUID

@RestController
@RequestMapping("/api/gateway/v1/composition")
@Tag(name = "API Composition", description = "API Composition Pattern - Compose data from multiple services")
class CompositionController(private val compositionService: OrderCompositionService) {

    @GetMapping("/orders/{orderId}")
    @Operation(
        summary = "Get order with inventory",
        description = "Demonstrates API Composition pattern: fetches order from order-service and inventory from inventory-service"
    )
    fun getOrderWithInventory(
        @PathVariable orderId: UUID,
        @RequestParam productId: UUID,
        exchange: ServerWebExchange
    ): Mono<ResponseEntity<OrderWithInventoryComposition>> {
        return compositionService.getOrderWithInventory(orderId, productId, exchange)
            .map { ResponseEntity.ok(it) }
            .onErrorResume { ex ->
                Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build())
            }
    }
}