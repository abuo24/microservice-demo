package uz.coder.api_gateway.config

import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Mono

@RestController
@RequestMapping("/fallback")
class FallbackController {

    @GetMapping("/orders")
    @ResponseStatus(HttpStatus.SERVICE_UNAVAILABLE)
    fun ordersFallback(): Mono<Map<String, String>> =
        Mono.just(mapOf("error" to "Order service is temporarily unavailable. Please try again later."))

    @GetMapping("/inventory")
    @ResponseStatus(HttpStatus.SERVICE_UNAVAILABLE)
    fun inventoryFallback(): Mono<Map<String, String>> =
        Mono.just(mapOf("error" to "Inventory service is temporarily unavailable. Please try again later."))
}