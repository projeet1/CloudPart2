package acp.cw2.service;

import acp.cw2.configuration.AcpConfig;
import acp.cw2.dto.CounterMessage;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
public class KafkaService {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final AcpConfig acpConfig;
    private final ObjectMapper objectMapper;

    public KafkaService(KafkaTemplate<String, String> kafkaTemplate,
                        AcpConfig acpConfig,
                        ObjectMapper objectMapper) {
        this.kafkaTemplate = kafkaTemplate;
        this.acpConfig = acpConfig;
        this.objectMapper = objectMapper;
    }

    public void sendCounterMessages(String topicName, int messageCount) {
        for (int i = 0; i < messageCount; i++) {
            CounterMessage message = new CounterMessage(acpConfig.getSid(), i);
            try {
                String json = objectMapper.writeValueAsString(message);
                kafkaTemplate.send(topicName, json);
            } catch (JsonProcessingException e) {
                throw new RuntimeException("Failed to serialize Kafka message", e);
            }
        }
    }
}