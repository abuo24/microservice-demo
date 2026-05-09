package uz.coder.inventory_service.service

import io.micrometer.core.instrument.Counter
import io.micrometer.core.instrument.MeterRegistry
import org.slf4j.LoggerFactory
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uz.coder.inventory_service.audit.Auditable
import uz.coder.inventory_service.domain.InventoryItem
import uz.coder.inventory_service.dto.InventoryRequest
import uz.coder.inventory_service.dto.InventoryResponse
import uz.coder.inventory_service.exception.InsufficientStockException
import uz.coder.inventory_service.exception.InventoryNotFoundException
import uz.coder.inventory_service.repository.InventoryRepository
import java.time.Instant
import java.util.UUID

@Service
@Transactional(readOnly = true)
class InventoryService(
    private val inventoryRepository: InventoryRepository,
    private val meterRegistry: MeterRegistry
) {
    private val log = LoggerFactory.getLogger(javaClass)

    private val reservationsTotal = Counter.builder("inventory.reservations.total")
        .description("Total stock reservations")
        .register(meterRegistry)

    private val releasesTotal = Counter.builder("inventory.releases.total")
        .description("Total stock reservation releases")
        .register(meterRegistry)

    private val insufficientStockTotal = Counter.builder("inventory.insufficient_stock.total")
        .description("Total insufficient stock rejections")
        .register(meterRegistry)

    @Cacheable(cacheNames = ["inventory"], key = "#productId")
    fun findByProductId(productId: String): InventoryResponse {
        log.info("Finding inventory productId={}", productId)
        return inventoryRepository.findByProductId(productId)
            .map { InventoryResponse.from(it) }
            .orElseThrow { InventoryNotFoundException("Inventory not found for productId: $productId") }
    }

    @Cacheable(cacheNames = ["inventory"], key = "#id")
    fun findById(id: UUID): InventoryResponse =
        inventoryRepository.findById(id)
            .map { InventoryResponse.from(it) }
            .orElseThrow { InventoryNotFoundException("Inventory not found: $id") }

    @Cacheable(cacheNames = ["inventory"], key = "'all'")
    fun findAll(): List<InventoryResponse> =
        inventoryRepository.findAll().map { InventoryResponse.from(it) }

    @Cacheable(cacheNames = ["inventory"], key = "'in-stock'")
    fun findAllInStock(): List<InventoryResponse> =
        inventoryRepository.findAllInStock().map { InventoryResponse.from(it) }

    @Auditable(action = "UPSERT_INVENTORY", resourceType = "INVENTORY")
    @Transactional
    fun createOrUpdate(request: InventoryRequest): InventoryResponse {
        log.info("Creating/updating inventory productId={}", request.productId)
        val item = inventoryRepository.findByProductId(request.productId)
            .map { existing ->
                existing.productName = request.productName
                existing.quantity = request.quantity
                existing.unitPrice = request.unitPrice
                existing.updatedAt = Instant.now()
                existing
            }
            .orElseGet {
                InventoryItem(
                    productId = request.productId,
                    productName = request.productName,
                    quantity = request.quantity,
                    unitPrice = request.unitPrice
                )
            }
        return InventoryResponse.from(inventoryRepository.save(item))
    }

    @Auditable(action = "RESERVE_STOCK", resourceType = "INVENTORY")
    @Transactional
    fun reserveStock(productId: String, quantity: Int): InventoryResponse {
        log.info("Reserving stock productId={} quantity={}", productId, quantity)
        val item = inventoryRepository.findByProductIdForUpdate(productId)
            .orElseThrow { InventoryNotFoundException("Inventory not found for productId: $productId") }
        if (item.availableQuantity < quantity) {
            insufficientStockTotal.increment()
            throw InsufficientStockException(
                "Insufficient stock for productId=$productId: available=${item.availableQuantity}, requested=$quantity"
            )
        }
        item.reservedQuantity += quantity
        item.updatedAt = Instant.now()
        reservationsTotal.increment()
        return InventoryResponse.from(inventoryRepository.save(item))
    }

    @Auditable(action = "RELEASE_STOCK", resourceType = "INVENTORY")
    @Transactional
    fun releaseReservation(productId: String, quantity: Int): InventoryResponse {
        log.info("Releasing reservation productId={} quantity={}", productId, quantity)
        val item = inventoryRepository.findByProductIdForUpdate(productId)
            .orElseThrow { InventoryNotFoundException("Inventory not found for productId: $productId") }
        item.reservedQuantity = maxOf(0, item.reservedQuantity - quantity)
        item.updatedAt = Instant.now()
        releasesTotal.increment()
        return InventoryResponse.from(inventoryRepository.save(item))
    }

    @Auditable(action = "DEDUCT_STOCK", resourceType = "INVENTORY")
    @Transactional
    fun confirmDeduction(productId: String, quantity: Int): InventoryResponse {
        log.info("Confirming deduction productId={} quantity={}", productId, quantity)
        val item = inventoryRepository.findByProductIdForUpdate(productId)
            .orElseThrow { InventoryNotFoundException("Inventory not found for productId: $productId") }
        item.quantity = maxOf(0, item.quantity - quantity)
        item.reservedQuantity = maxOf(0, item.reservedQuantity - quantity)
        item.updatedAt = Instant.now()
        return InventoryResponse.from(inventoryRepository.save(item))
    }
}