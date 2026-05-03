package uz.coder.inventory_service.exception

class InventoryNotFoundException(message: String) : RuntimeException(message)
class InsufficientStockException(message: String) : RuntimeException(message)