package contracts.order

import org.springframework.cloud.contract.spec.Contract

Contract.make {
    description 'order-service publishes OrderCreatedEvent to order-created topic'
    label 'order-created'
    input {
        triggeredBy('createOrderAndPublishEvent()')
    }
    outputMessage {
        sentTo 'order-created'
        body([
            eventId: $(consumer(regex(uuid())), producer('00000000-0000-0000-0000-000000000000')),
            timestamp: $(consumer(regex('.+')), producer('2025-01-01T00:00:00Z')),
            orderId: $(consumer(regex(uuid())), producer('00000000-0000-0000-0000-000000000000')),
            customerId: 'cust-123',
            totalAmount: 100.00
        ])
        headers {
            messagingContentType('application/json')
        }
    }
}