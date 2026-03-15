package acp.cw2.service;

import acp.cw2.configuration.AcpConfig;
import acp.cw2.dto.CounterMessage;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import org.springframework.amqp.core.Message;

@Service
public class RabbitMqService {

    private final RabbitTemplate rabbitTemplate;
    private final AcpConfig acpConfig;
    private final ObjectMapper objectMapper;

    public RabbitMqService(RabbitTemplate rabbitTemplate, AcpConfig acpConfig, ObjectMapper objectMapper) {
        this.rabbitTemplate = rabbitTemplate;
        this.acpConfig = acpConfig;
        this.objectMapper = objectMapper;
    }

    public void sendCounterMessages(String queueName, int messageCount) {
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
        long startTime = System.currentTimeMillis();
        List<String> messages = new ArrayList<>();

        while (System.currentTimeMillis() - startTime < timeoutInMsec) {
            Message message = rabbitTemplate.receive(queueName);

            if (message != null) {
                String body = new String(message.getBody(), StandardCharsets.UTF_8);
                messages.add(body);
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
}