package uz.coder.order_service.audit

@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class Auditable(
    val action: String,
    val resourceType: String
)