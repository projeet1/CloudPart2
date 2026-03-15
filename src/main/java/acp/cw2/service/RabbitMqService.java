package acp.cw2.service;

import acp.cw2.configuration.AcpConfig;
import acp.cw2.dto.CounterMessage;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.amqp.core.AmqpAdmin;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;
import acp.cw2.dto.SortedMessage;
import java.util.Comparator;
import java.util.ArrayList;
import java.util.List;
import org.springframework.amqp.core.Message;
import java.nio.charset.StandardCharsets;

@Service
public class RabbitMqService {

    private final RabbitTemplate rabbitTemplate;
    private final AcpConfig acpConfig;
    private final ObjectMapper objectMapper;
    private final AmqpAdmin amqpAdmin;

    public RabbitMqService(RabbitTemplate rabbitTemplate,
                           AcpConfig acpConfig,
                           ObjectMapper objectMapper,
                           AmqpAdmin amqpAdmin) {
        this.rabbitTemplate = rabbitTemplate;
        this.acpConfig = acpConfig;
        this.objectMapper = objectMapper;
        this.amqpAdmin = amqpAdmin;
    }

    private void ensureQueueExists(String queueName) {
        amqpAdmin.declareQueue(new Queue(queueName, true));
    }

    public void ensureQueueForExternalUse(String queueName) {
        ensureQueueExists(queueName);
    }

    public void sendCounterMessages(String queueName, int messageCount) {
        ensureQueueExists(queueName);

        for (int i = 0; i < messageCount; i++) {
            CounterMessage message = new CounterMessage(acpConfig.getSid(), i);
            try {
                String json = objectMapper.writeValueAsString(message);
                rabbitTemplate.convertAndSend(queueName, json);
            } catch (JsonProcessingException e) {
                throw new RuntimeException("Failed to serialize RabbitMQ message", e);
            }
        }
    }

    public List<String> readMessagesForDuration(String queueName, long timeoutInMsec) {
        ensureQueueExists(queueName);

        long startTime = System.currentTimeMillis();
        List<String> messages = new ArrayList<>();

        while (System.currentTimeMillis() - startTime < timeoutInMsec) {
            Object payload = rabbitTemplate.receiveAndConvert(queueName);

            if (payload != null) {
                messages.add(payload.toString());
            } else {
                try {
                    Thread.sleep(10);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException("Interrupted while reading from RabbitMQ", e);
                }
            }
        }

        return messages;
    }
    public List<SortedMessage> readAndSortMessages(String queueName, int messagesToConsider) {
        ensureQueueExists(queueName);

        List<SortedMessage> messages = new ArrayList<>();

        for (int i = 0; i < messagesToConsider; i++) {
            Message rawMessage = rabbitTemplate.receive(queueName);

            if (rawMessage == null) {
                throw new RuntimeException("Expected " + messagesToConsider
                        + " messages but queue ran empty after " + i);
            }

            try {
                String body = new String(rawMessage.getBody(), StandardCharsets.UTF_8);
                System.out.println("RAW SORTED BODY = " + body);

                SortedMessage message = objectMapper.readValue(body, SortedMessage.class);
                messages.add(message);
            } catch (JsonProcessingException e) {
                throw new RuntimeException("Failed to parse sorted RabbitMQ message", e);
            }
        }

        messages.sort(Comparator.comparingInt(SortedMessage::getId));
        return messages;
    }
}