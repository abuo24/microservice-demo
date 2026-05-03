package uz.coder.order_service.exception

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

    @ExceptionHandler(OrderNotFoundException::class)
    fun handleOrderNotFound(ex: OrderNotFoundException): ProblemDetail {
        log.warn("Order not found: {}", ex.message)
        return ProblemDetail.forStatus(HttpStatus.NOT_FOUND).apply {
            title = "Order Not Found"
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

    @ExceptionHandler(IllegalArgumentException::class)
    fun handleIllegalArgument(ex: IllegalArgumentException): ProblemDetail {
        log.warn("Illegal argument: {}", ex.message)
        return ProblemDetail.forStatus(HttpStatus.BAD_REQUEST).apply {
            title = "Bad Request"
            detail = ex.message
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