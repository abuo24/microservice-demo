package uz.coder.inventory_service.controller

import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import uz.coder.inventory_service.dto.InventoryRequest
import uz.coder.inventory_service.dto.InventoryResponse
import uz.coder.inventory_service.dto.ReserveRequest
import uz.coder.inventory_service.service.InventoryService
import java.util.UUID

@RestController
@RequestMapping
class InventoryController(private val inventoryService: InventoryService) {
    private val log = LoggerFactory.getLogger(javaClass)

    @GetMapping
    fun getAll(): ResponseEntity<List<InventoryResponse>> {
        log.debug("GET all inventory items")
        return ResponseEntity.ok(inventoryService.findAll())
    }

    @GetMapping("/in-stock")
    fun getAllInStock(): ResponseEntity<List<InventoryResponse>> {
        log.debug("GET all in-stock inventory items")
        return ResponseEntity.ok(inventoryService.findAllInStock())
    }

    @GetMapping("/{id}")
    fun getById(@PathVariable id: UUID): ResponseEntity<InventoryResponse> {
        log.debug("GET inventory id={}", id)
        return ResponseEntity.ok(inventoryService.findById(id))
    }

    @GetMapping("/product/{productId}")
    fun getByProductId(@PathVariable productId: String): ResponseEntity<InventoryResponse> {
        log.debug("GET inventory productId={}", productId)
        return ResponseEntity.ok(inventoryService.findByProductId(productId))
    }

    @PostMapping
    fun createOrUpdate(@RequestBody request: InventoryRequest): ResponseEntity<InventoryResponse> {
        log.info("POST createOrUpdate inventory productId={} quantity={}", request.productId, request.quantity)
        return ResponseEntity.status(HttpStatus.CREATED).body(inventoryService.createOrUpdate(request))
    }

    @PostMapping("/reserve")
    fun reserve(@RequestBody request: ReserveRequest): ResponseEntity<InventoryResponse> {
        log.info("POST reserve productId={} quantity={}", request.productId, request.quantity)
        return ResponseEntity.ok(inventoryService.reserveStock(request.productId, request.quantity))
    }

    @PostMapping("/release")
    fun release(@RequestBody request: ReserveRequest): ResponseEntity<InventoryResponse> {
        log.info("POST release productId={} quantity={}", request.productId, request.quantity)
        return ResponseEntity.ok(inventoryService.releaseReservation(request.productId, request.quantity))
    }
}