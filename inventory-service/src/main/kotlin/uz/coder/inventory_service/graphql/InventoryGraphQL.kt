package uz.coder.inventory_service.graphql

import org.springframework.graphql.data.method.annotation.Argument
import org.springframework.graphql.data.method.annotation.QueryMapping
import org.springframework.stereotype.Controller
import uz.coder.inventory_service.dto.InventoryResponse
import uz.coder.inventory_service.service.InventoryService
import java.util.UUID

@Controller
class InventoryGraphQL(private val inventoryService: InventoryService) {

    @QueryMapping
    fun inventoryByProductId(@Argument productId: String): InventoryResponse =
        inventoryService.findByProductId(productId)

    @QueryMapping
    fun inventoryById(@Argument id: String): InventoryResponse =
        inventoryService.findById(UUID.fromString(id))

    @QueryMapping
    fun allInventory(): List<InventoryResponse> =
        inventoryService.findAll()

    @QueryMapping
    fun allInStock(): List<InventoryResponse> =
        inventoryService.findAllInStock()
}