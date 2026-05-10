package uz.coder.order_service.event

import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import uz.coder.order_service.repository.EventStoreRepository
import uz.coder.order_service.repository.OrderProjectionRepository

@Component
class OrderProjectionSubscriber(
    private val eventStoreRepository: EventStoreRepository,
    private val projectionHandler: OrderProjectionHandler,
    private val projectionRepository: OrderProjectionRepository
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @Scheduled(fixedDelay = 500)
    @Transactional
    fun projectEvents() {
        val allEvents = eventStoreRepository.findAll()
        for (record in allEvents) {
            val projection = projectionRepository.findByAggregateId(record.aggregateId)
            if (projection == null || projection.lastEventSequence < record.sequenceNumber) {
                projectionHandler.handleEvent(record.aggregateId, record)
            }
        }
    }
}