package uz.coder.order_service.audit

import net.logstash.logback.argument.StructuredArguments.entries
import org.slf4j.LoggerFactory
import org.slf4j.MDC
import org.springframework.context.event.EventListener
import org.springframework.security.authorization.event.AuthorizationDeniedEvent
import org.springframework.stereotype.Component

@Component
class SecurityAuditListener {

    private val log = LoggerFactory.getLogger("AUDIT")

    @EventListener
    fun onAccessDenied(event: AuthorizationDeniedEvent<*>) {
        val userId = event.authentication.get()?.name ?: "anonymous"
        log.warn("audit", entries(mapOf(
            "audit_action" to "ACCESS_DENIED",
            "audit_outcome" to "FAILURE",
            "audit_resource_type" to "ENDPOINT",
            "audit_user" to userId,
            "audit_failure_reason" to "INSUFFICIENT_PRIVILEGES",
            "audit_trace_id" to (MDC.get("traceId") ?: "")
        )))
    }
}