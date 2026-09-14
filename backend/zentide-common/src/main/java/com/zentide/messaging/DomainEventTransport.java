package com.zentide.messaging;

/** Infrastructure adapter. RabbitMQ is one implementation; business services never call it directly. */
public interface DomainEventTransport {
    void publish(String routingKey, DomainEventEnvelope envelope);
}
