package contracts.order

import org.springframework.cloud.contract.spec.Contract

Contract.make {
    description 'order-service publishes OrderStatusChangedEvent to order-status-changed topic'
    label 'order-status-changed'
    input {
        triggeredBy('updateOrderStatusAndPublishEvent()')
    }
    outputMessage {
        sentTo 'order-status-changed'
        body([
            eventId: $(consumer(regex(uuid())), producer('00000000-0000-0000-0000-000000000000')),
            timestamp: $(consumer(regex('.+')), producer('2025-01-01T00:00:00Z')),
            orderId: $(consumer(regex(uuid())), producer('00000000-0000-0000-0000-000000000000')),
            newStatus: 'SHIPPED',
            previousStatus: 'PENDING'
        ])
        headers {
            messagingContentType('application/json')
        }
    }
}