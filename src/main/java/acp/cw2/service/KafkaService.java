package acp.cw2.service;

import acp.cw2.configuration.AcpConfig;
import acp.cw2.dto.CounterMessage;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.UUID;

@Service
public class KafkaService {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final AcpConfig acpConfig;
    private final ObjectMapper objectMapper;
    private final String bootstrapServers;

    public KafkaService(KafkaTemplate<String, String> kafkaTemplate,
                        AcpConfig acpConfig,
                        ObjectMapper objectMapper,
                        @Value("${spring.kafka.bootstrap-servers}") String bootstrapServers) {
        this.kafkaTemplate = kafkaTemplate;
        this.acpConfig = acpConfig;
        this.objectMapper = objectMapper;
        this.bootstrapServers = bootstrapServers;
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
    public List<String> readMessagesForDuration(String topicName, long timeoutInMsec) {
        Properties props = new Properties();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, "acp-cw2-read-" + UUID.randomUUID());
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "false");

        List<String> messages = new ArrayList<>();

        try (KafkaConsumer<String, String> consumer = new KafkaConsumer<>(props)) {
            consumer.subscribe(List.of(topicName));

            long subscribeDeadline = System.currentTimeMillis() + 3000;
            while (consumer.assignment().isEmpty() && System.currentTimeMillis() < subscribeDeadline) {
                consumer.poll(Duration.ofMillis(100));
            }

            if (consumer.assignment().isEmpty()) {
                return messages;
            }

            consumer.seekToBeginning(consumer.assignment());

            long deadline = System.currentTimeMillis() + timeoutInMsec;
            while (System.currentTimeMillis() < deadline) {
                long remaining = deadline - System.currentTimeMillis();
                long pollMillis = Math.min(200, Math.max(1, remaining));

                ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(pollMillis));

                for (ConsumerRecord<String, String> record : records) {
                    messages.add(record.value());
                }
            }
        }

        return messages;
    }
}