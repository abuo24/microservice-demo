package uz.coder.api_gateway.filter

import org.slf4j.LoggerFactory
import org.springframework.cloud.gateway.filter.GatewayFilterChain
import org.springframework.cloud.gateway.filter.GlobalFilter
import org.springframework.core.Ordered
import org.springframework.stereotype.Component
import org.springframework.web.server.ServerWebExchange
import reactor.core.publisher.Mono
import java.time.Instant

@Component
class LoggingGlobalFilter : GlobalFilter, Ordered {

    private val log = LoggerFactory.getLogger(javaClass)

    override fun filter(exchange: ServerWebExchange, chain: GatewayFilterChain): Mono<Void> {
        val request = exchange.request
        val startTime = Instant.now().toEpochMilli()

        log.info(
            "Request: method={} uri={} remoteAddr={}",
            request.method,
            request.uri,
            request.remoteAddress?.address?.hostAddress
        )

        return chain.filter(exchange).then(Mono.fromRunnable {
            val elapsed = Instant.now().toEpochMilli() - startTime
            log.info(
                "Response: status={} uri={} durationMs={}",
                exchange.response.statusCode,
                request.uri,
                elapsed
            )
        })
    }

    override fun getOrder(): Int = Ordered.LOWEST_PRECEDENCE
}