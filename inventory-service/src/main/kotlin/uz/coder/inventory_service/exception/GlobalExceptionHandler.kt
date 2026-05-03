package uz.coder.inventory_service.exception

import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ProblemDetail
import org.springframework.validation.FieldError
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import java.time.Instant

@RestControllerAdvice
class GlobalExceptionHandler {

    private val log = LoggerFactory.getLogger(javaClass)

    @ExceptionHandler(InventoryNotFoundException::class)
    fun handleNotFound(ex: InventoryNotFoundException): ProblemDetail {
        log.warn("Inventory not found: {}", ex.message)
        return ProblemDetail.forStatus(HttpStatus.NOT_FOUND).apply {
            title = "Inventory Item Not Found"
            detail = ex.message
            setProperty("timestamp", Instant.now())
        }
    }

    @ExceptionHandler(InsufficientStockException::class)
    fun handleInsufficientStock(ex: InsufficientStockException): ProblemDetail {
        log.warn("Insufficient stock: {}", ex.message)
        return ProblemDetail.forStatus(HttpStatus.CONFLICT).apply {
            title = "Insufficient Stock"
            detail = ex.message
            setProperty("timestamp", Instant.now())
        }
    }

    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleValidation(ex: MethodArgumentNotValidException): ProblemDetail {
        val errors = ex.bindingResult.allErrors.associate { error ->
            val field = if (error is FieldError) error.field else "global"
            field to (error.defaultMessage ?: "Invalid value")
        }
        log.warn("Validation failed: {}", errors)
        return ProblemDetail.forStatus(HttpStatus.BAD_REQUEST).apply {
            title = "Validation Failed"
            detail = "Request validation failed"
            setProperty("errors", errors)
            setProperty("timestamp", Instant.now())
        }
    }

    @ExceptionHandler(Exception::class)
    fun handleGeneric(ex: Exception): ProblemDetail {
        log.error("Unexpected error", ex)
        return ProblemDetail.forStatus(HttpStatus.INTERNAL_SERVER_ERROR).apply {
            title = "Internal Server Error"
            detail = "An unexpected error occurred"
            setProperty("timestamp", Instant.now())
        }
    }
}