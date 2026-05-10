package uz.coder.inventory_service.grpc

import io.grpc.Status
import io.grpc.stub.StreamObserver
import net.devh.boot.grpc.server.service.GrpcService
import uz.coder.inventory.grpc.CheckStockRequest
import uz.coder.inventory.grpc.CheckStockResponse
import uz.coder.inventory.grpc.InventoryServiceGrpc
import uz.coder.inventory_service.exception.InventoryNotFoundException
import uz.coder.inventory_service.service.InventoryService

@GrpcService
class InventoryGrpcServiceImpl(
    private val inventoryService: InventoryService
) : InventoryServiceGrpc.InventoryServiceImplBase() {

    override fun checkStock(
        request: CheckStockRequest,
        responseObserver: StreamObserver<CheckStockResponse>
    ) {
        try {
            val item = inventoryService.findByProductId(request.productId)
            responseObserver.onNext(
                CheckStockResponse.newBuilder()
                    .setProductId(item.productId)
                    .setProductName(item.productName)
                    .setAvailableQuantity(item.availableQuantity)
                    .setAvailable(item.availableQuantity > 0)
                    .build()
            )
            responseObserver.onCompleted()
        } catch (e: InventoryNotFoundException) {
            responseObserver.onError(
                Status.NOT_FOUND
                    .withDescription("Product not found: ${request.productId}")
                    .asRuntimeException()
            )
        } catch (e: Exception) {
            responseObserver.onError(
                Status.INTERNAL.withDescription(e.message).asRuntimeException()
            )
        }
    }
}