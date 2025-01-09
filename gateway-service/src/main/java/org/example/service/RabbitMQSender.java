package org.example.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class RabbitMQSender {
    private static final Logger log = LoggerFactory.getLogger(RabbitMQSender.class);

    private final RabbitTemplate rabbitTemplate;

    @Value("${rabbitmq.exchange}")
    private String exchange;

    @Value("${rabbitmq.routing.key}")
    private String routingKey;

    public RabbitMQSender(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    public void sendMessage(Object message) {
        log.info("Отправка сообщения в RabbitMQ: {}", message);
        try {
            rabbitTemplate.convertAndSend(exchange, routingKey, message);
            log.debug("Сообщение успешно отправлено");
        } catch (Exception e) {
            log.error("Ошибка при отправке сообщения в RabbitMQ: {}", e.getMessage(), e);
        }
    }
}
