package uz.coder.order_service.client

import io.grpc.StatusRuntimeException
import net.devh.boot.grpc.client.inject.GrpcClient
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import uz.coder.inventory.grpc.CheckStockRequest
import uz.coder.inventory.grpc.InventoryServiceGrpc
import uz.coder.order_service.dto.StockCheckResponse

@Component
class InventoryGrpcClient {

    private val log = LoggerFactory.getLogger(javaClass)

    @GrpcClient("inventory-service")
    lateinit var stub: InventoryServiceGrpc.InventoryServiceBlockingStub

    fun checkStock(productId: String): StockCheckResponse {
        return try {
            log.info("[gRPC] Checking stock for product=$productId")
            val res = stub.checkStock(
                CheckStockRequest.newBuilder().setProductId(productId).build()
            )
            StockCheckResponse(
                productId = res.productId,
                productName = res.productName,
                availableQuantity = res.availableQuantity,
                available = res.available,
                fallback = false
            )
        } catch (e: StatusRuntimeException) {
            log.warn("[gRPC] Call failed: ${e.status}")
            StockCheckResponse(
                productId = productId,
                productName = "Unknown (gRPC error)",
                availableQuantity = 0,
                available = false,
                fallback = true,
                fallbackReason = "gRPC ${e.status.code}: ${e.status.description}"
            )
        }
    }
}