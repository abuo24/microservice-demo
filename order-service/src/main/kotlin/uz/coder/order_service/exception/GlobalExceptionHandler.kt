package uz.coder.order_service.exception

import io.micrometer.core.instrument.MeterRegistry
import net.logstash.logback.argument.StructuredArguments.entries
import org.slf4j.LoggerFactory
import org.slf4j.MDC
import org.springframework.http.HttpStatus
import org.springframework.http.ProblemDetail
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.validation.FieldError
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import java.time.Instant

@RestControllerAdvice
class GlobalExceptionHandler(private val meterRegistry: MeterRegistry) {

    private val log = LoggerFactory.getLogger("EXCEPTION_TRACKER")

    // --- 4xx client errors: expected, track but don't alarm ---

    @ExceptionHandler(OrderNotFoundException::class)
    fun handleOrderNotFound(ex: OrderNotFoundException): ProblemDetail {
        track(ex, "client_error")
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
        track(ex, "client_error")
        return ProblemDetail.forStatus(HttpStatus.BAD_REQUEST).apply {
            title = "Validation Failed"
            detail = "Request validation failed"
            setProperty("errors", errors)
            setProperty("timestamp", Instant.now())
        }
    }

    @ExceptionHandler(IllegalArgumentException::class)
    fun handleIllegalArgument(ex: IllegalArgumentException): ProblemDetail {
        track(ex, "client_error")
        return ProblemDetail.forStatus(HttpStatus.BAD_REQUEST).apply {
            title = "Bad Request"
            detail = ex.message
            setProperty("timestamp", Instant.now())
        }
    }

    // --- 5xx server errors: unexpected, alert on these ---

    @ExceptionHandler(Exception::class)
    fun handleGeneric(ex: Exception): ProblemDetail {
        track(ex, "server_error")
        return ProblemDetail.forStatus(HttpStatus.INTERNAL_SERVER_ERROR).apply {
            title = "Internal Server Error"
            detail = "An unexpected error occurred"
            setProperty("timestamp", Instant.now())
        }
    }

    private fun track(ex: Exception, category: String) {
        val exType = ex::class.simpleName ?: "Unknown"
        val user = SecurityContextHolder.getContext().authentication?.name ?: "anonymous"

        meterRegistry.counter(
            "exceptions.total",
            "type", exType,
            "category", category
        ).increment()

        val level = if (category == "server_error") "ERROR" else "WARN"
        if (level == "ERROR") {
            log.error("exception", ex, entries(mapOf(
                "exception_type" to exType,
                "exception_category" to category,
                "exception_user" to user,
                "exception_trace_id" to (MDC.get("traceId") ?: "")
            )))
        } else {
            log.warn("exception", entries(mapOf(
                "exception_type" to exType,
                "exception_category" to category,
                "exception_message" to (ex.message ?: ""),
                "exception_user" to user,
                "exception_trace_id" to (MDC.get("traceId") ?: "")
            )))
        }
    }
}