package uz.coder.inventory_service.controller

import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import uz.coder.inventory_service.dto.InventoryRequest
import uz.coder.inventory_service.dto.InventoryResponse
import uz.coder.inventory_service.dto.ReserveRequest
import uz.coder.inventory_service.service.InventoryService
import java.util.UUID

@RestController
@RequestMapping("/api/inventory")
class InventoryController(private val inventoryService: InventoryService) {

    @GetMapping
    fun getAll(): ResponseEntity<List<InventoryResponse>> =
        ResponseEntity.ok(inventoryService.findAll())

    @GetMapping("/in-stock")
    fun getAllInStock(): ResponseEntity<List<InventoryResponse>> =
        ResponseEntity.ok(inventoryService.findAllInStock())

    @GetMapping("/{id}")
    fun getById(@PathVariable id: UUID): ResponseEntity<InventoryResponse> =
        ResponseEntity.ok(inventoryService.findById(id))

    @GetMapping("/product/{productId}")
    fun getByProductId(@PathVariable productId: String): ResponseEntity<InventoryResponse> =
        ResponseEntity.ok(inventoryService.findByProductId(productId))

    @PostMapping
    fun createOrUpdate(@RequestBody request: InventoryRequest): ResponseEntity<InventoryResponse> =
        ResponseEntity.status(HttpStatus.CREATED).body(inventoryService.createOrUpdate(request))

    @PostMapping("/reserve")
    fun reserve(@RequestBody request: ReserveRequest): ResponseEntity<InventoryResponse> =
        ResponseEntity.ok(inventoryService.reserveStock(request.productId, request.quantity))

    @PostMapping("/release")
    fun release(@RequestBody request: ReserveRequest): ResponseEntity<InventoryResponse> =
        ResponseEntity.ok(inventoryService.releaseReservation(request.productId, request.quantity))
}