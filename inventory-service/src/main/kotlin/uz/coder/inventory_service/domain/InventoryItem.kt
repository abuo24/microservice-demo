package uz.coder.inventory_service.domain

import jakarta.persistence.*
import java.math.BigDecimal
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "inventory_items")
class InventoryItem(
    @Id
    val id: UUID = UUID.randomUUID(),

    @Column(nullable = false, unique = true)
    val productId: String,

    @Column(nullable = false)
    var productName: String,

    @Column(nullable = false)
    var quantity: Int = 0,

    @Column(nullable = false)
    var reservedQuantity: Int = 0,

    @Column(nullable = false, precision = 10, scale = 2)
    var unitPrice: BigDecimal,

    @Column(nullable = false, updatable = false)
    val createdAt: Instant = Instant.now(),

    @Column(nullable = false)
    var updatedAt: Instant = Instant.now()
) {
    val availableQuantity: Int
        get() = quantity - reservedQuantity
}