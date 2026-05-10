package uz.coder.inventory_service.audit

import net.logstash.logback.argument.StructuredArguments.entries
import org.slf4j.LoggerFactory
import org.slf4j.MDC
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Component

@Component
class AuditLogger {

    private val log = LoggerFactory.getLogger("AUDIT")

    fun success(
        action: String,
        resourceType: String,
        resourceId: String? = null,
        details: Map<String, Any?> = emptyMap()
    ) = write("SUCCESS", action, resourceType, resourceId, null, details)

    fun failure(
        action: String,
        resourceType: String,
        resourceId: String? = null,
        reason: String? = null,
        details: Map<String, Any?> = emptyMap()
    ) = write("FAILURE", action, resourceType, resourceId, reason, details)

    private fun write(
        outcome: String,
        action: String,
        resourceType: String,
        resourceId: String?,
        reason: String?,
        details: Map<String, Any?>
    ) {
        val userId = SecurityContextHolder.getContext().authentication?.name ?: "anonymous"
        val fields = buildMap<String, Any?> {
            put("audit_action", action)
            put("audit_outcome", outcome)
            put("audit_resource_type", resourceType)
            put("audit_user", userId)
            MDC.get("traceId")?.let { put("audit_trace_id", it) }
            resourceId?.let { put("audit_resource_id", it) }
            reason?.let { put("audit_failure_reason", it) }
            details.forEach { (k, v) -> put("audit_$k", v) }
        }
        log.info("audit", entries(fields))
    }
}