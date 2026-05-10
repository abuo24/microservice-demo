package uz.coder.order_service.grpc

import io.grpc.Status
import io.grpc.stub.StreamObserver
import net.devh.boot.grpc.server.service.GrpcService
import uz.coder.order.grpc.CreateOrderRequest
import uz.coder.order.grpc.GetOrderRequest
import uz.coder.order.grpc.GetOrdersByCustomerRequest
import uz.coder.order.grpc.OrderItem as GrpcOrderItem
import uz.coder.order.grpc.OrderListResponse
import uz.coder.order.grpc.OrderResponse as GrpcOrderResponse
import uz.coder.order.grpc.OrderServiceGrpc
import uz.coder.order.grpc.OrderStatus as GrpcOrderStatus
import uz.coder.order.grpc.UpdateStatusRequest
import uz.coder.order_service.dto.OrderItemRequest
import uz.coder.order_service.dto.OrderRequest
import uz.coder.order_service.dto.OrderResponse
import uz.coder.order_service.enumuration.OrderStatus
import uz.coder.order_service.exception.OrderNotFoundException
import uz.coder.order_service.service.OrderService
import java.math.BigDecimal
import java.util.UUID

@GrpcService
class OrderGrpcServiceImpl(
    private val orderService: OrderService
) : OrderServiceGrpc.OrderServiceImplBase() {

    override fun getOrder(request: GetOrderRequest, responseObserver: StreamObserver<GrpcOrderResponse>) {
        try {
            val order = orderService.findById(UUID.fromString(request.id))
            responseObserver.onNext(order.toProto())
            responseObserver.onCompleted()
        } catch (e: OrderNotFoundException) {
            responseObserver.onError(
                Status.NOT_FOUND.withDescription("Order not found: ${request.id}").asRuntimeException()
            )
        }
    }

    override fun getOrdersByCustomer(
        request: GetOrdersByCustomerRequest,
        responseObserver: StreamObserver<OrderListResponse>
    ) {
        val orders = orderService.findByCustomerId(request.customerId)
        responseObserver.onNext(
            OrderListResponse.newBuilder()
                .addAllOrders(orders.map { it.toProto() })
                .build()
        )
        responseObserver.onCompleted()
    }

    override fun createOrder(request: CreateOrderRequest, responseObserver: StreamObserver<GrpcOrderResponse>) {
        try {
            val dto = OrderRequest(
                customerId = request.customerId,
                items = request.itemsList.map {
                    OrderItemRequest(
                        productId = it.productId,
                        productName = it.productName,
                        quantity = it.quantity,
                        unitPrice = BigDecimal(it.unitPrice)
                    )
                }
            )
            val order = orderService.createOrder(dto)
            responseObserver.onNext(order.toProto())
            responseObserver.onCompleted()
        } catch (e: Exception) {
            responseObserver.onError(
                Status.INTERNAL.withDescription(e.message).asRuntimeException()
            )
        }
    }

    override fun updateStatus(request: UpdateStatusRequest, responseObserver: StreamObserver<GrpcOrderResponse>) {
        try {
            val status = OrderStatus.valueOf(request.status)
            val order = orderService.updateStatus(UUID.fromString(request.id), status)
            responseObserver.onNext(order.toProto())
            responseObserver.onCompleted()
        } catch (e: OrderNotFoundException) {
            responseObserver.onError(
                Status.NOT_FOUND.withDescription("Order not found: ${request.id}").asRuntimeException()
            )
        } catch (e: IllegalArgumentException) {
            responseObserver.onError(
                Status.INVALID_ARGUMENT.withDescription("Invalid status: ${request.status}").asRuntimeException()
            )
        }
    }
}

private fun OrderResponse.toProto(): GrpcOrderResponse =
    GrpcOrderResponse.newBuilder()
        .setId(id.toString())
        .setCustomerId(customerId)
        .setStatus(GrpcOrderStatus.valueOf(status.name))
        .addAllItems(items.map { item ->
            GrpcOrderItem.newBuilder()
                .setId(item.id.toString())
                .setProductId(item.productId)
                .setProductName(item.productName)
                .setQuantity(item.quantity)
                .setUnitPrice(item.unitPrice.toPlainString())
                .build()
        })
        .setTotalAmount(totalAmount.toPlainString())
        .setCreatedAt(createdAt.toString())
        .setUpdatedAt(updatedAt.toString())
        .build()