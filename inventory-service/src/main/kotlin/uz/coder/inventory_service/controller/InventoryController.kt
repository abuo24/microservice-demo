package uz.coder.inventory_service.controller

import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
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
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    fun getAll(): ResponseEntity<List<InventoryResponse>> =
        ResponseEntity.ok(inventoryService.findAll())

    @GetMapping("/in-stock")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    fun getAllInStock(): ResponseEntity<List<InventoryResponse>> =
        ResponseEntity.ok(inventoryService.findAllInStock())

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    fun getById(@PathVariable id: UUID): ResponseEntity<InventoryResponse> =
        ResponseEntity.ok(inventoryService.findById(id))

    @GetMapping("/product/{productId}")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    fun getByProductId(@PathVariable productId: String): ResponseEntity<InventoryResponse> =
        ResponseEntity.ok(inventoryService.findByProductId(productId))

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    fun createOrUpdate(@Valid @RequestBody request: InventoryRequest): ResponseEntity<InventoryResponse> =
        ResponseEntity.status(HttpStatus.CREATED).body(inventoryService.createOrUpdate(request))

    @PostMapping("/reserve")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    fun reserve(@Valid @RequestBody request: ReserveRequest): ResponseEntity<InventoryResponse> =
        ResponseEntity.ok(inventoryService.reserveStock(request.productId, request.quantity))

    @PostMapping("/release")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    fun release(@Valid @RequestBody request: ReserveRequest): ResponseEntity<InventoryResponse> =
        ResponseEntity.ok(inventoryService.releaseReservation(request.productId, request.quantity))
}