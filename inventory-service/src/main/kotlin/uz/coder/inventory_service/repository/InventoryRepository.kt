package uz.coder.inventory_service.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import uz.coder.inventory_service.domain.InventoryItem
import jakarta.persistence.LockModeType
import java.util.Optional
import java.util.UUID

@Repository
interface InventoryRepository : JpaRepository<InventoryItem, UUID> {
    fun findByProductId(productId: String): Optional<InventoryItem>

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT i FROM InventoryItem i WHERE i.productId = :productId")
    fun findByProductIdForUpdate(productId: String): Optional<InventoryItem>

    @Query("SELECT i FROM InventoryItem i WHERE i.quantity - i.reservedQuantity > 0")
    fun findAllInStock(): List<InventoryItem>
}