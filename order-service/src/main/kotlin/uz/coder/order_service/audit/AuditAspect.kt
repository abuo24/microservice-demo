package uz.coder.order_service.audit

import org.aspectj.lang.ProceedingJoinPoint
import org.aspectj.lang.annotation.Around
import org.aspectj.lang.annotation.Aspect
import org.springframework.stereotype.Component
import kotlin.reflect.full.memberProperties

@Aspect
@Component
class AuditAspect(private val auditLogger: AuditLogger) {

    @Around("@annotation(auditable)")
    fun audit(joinPoint: ProceedingJoinPoint, auditable: Auditable): Any? {
        return try {
            val result = joinPoint.proceed()
            auditLogger.success(auditable.action, auditable.resourceType, extractId(result))
            result
        } catch (ex: Exception) {
            auditLogger.failure(auditable.action, auditable.resourceType, reason = ex.message)
            throw ex
        }
    }

    private fun extractId(result: Any?): String? = try {
        result?.let {
            it::class.memberProperties
                .find { prop -> prop.name == "id" }
                ?.getter?.call(it)?.toString()
        }
    } catch (_: Exception) { null }
}