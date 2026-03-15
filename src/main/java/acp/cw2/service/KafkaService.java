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
import java.util.Comparator;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.UUID;
import acp.cw2.dto.SortedMessage;
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
                kafkaTemplate.send(topicName, json).get();
            } catch (JsonProcessingException e) {
                throw new RuntimeException("Failed to serialize Kafka message", e);
            } catch (Exception e) {
                throw new RuntimeException("Failed to send Kafka message", e);
            }
        }
    }
    public List<String> readMessagesForDuration(String topicName, long timeoutInMsec) {
        Properties props = new Properties();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "false");

        List<String> messages = new ArrayList<>();

        try (KafkaConsumer<String, String> consumer = new KafkaConsumer<>(props)) {
            List<org.apache.kafka.common.PartitionInfo> partitionInfos = consumer.partitionsFor(topicName);

            if (partitionInfos == null || partitionInfos.isEmpty()) {
                return messages;
            }

            List<org.apache.kafka.common.TopicPartition> partitions = new ArrayList<>();
            for (org.apache.kafka.common.PartitionInfo partitionInfo : partitionInfos) {
                partitions.add(new org.apache.kafka.common.TopicPartition(topicName, partitionInfo.partition()));
            }

            consumer.assign(partitions);
            consumer.seekToBeginning(partitions);

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
    public List<SortedMessage> readAndSortMessages(String topicName, int messagesToConsider) {
        Properties props = new Properties();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "false");

        List<SortedMessage> messages = new ArrayList<>();

        try (KafkaConsumer<String, String> consumer = new KafkaConsumer<>(props)) {
            List<org.apache.kafka.common.PartitionInfo> partitionInfos = consumer.partitionsFor(topicName);

            if (partitionInfos == null || partitionInfos.isEmpty()) {
                throw new RuntimeException("Topic has no partitions or does not exist: " + topicName);
            }

            List<org.apache.kafka.common.TopicPartition> partitions = new ArrayList<>();
            for (org.apache.kafka.common.PartitionInfo partitionInfo : partitionInfos) {
                partitions.add(new org.apache.kafka.common.TopicPartition(topicName, partitionInfo.partition()));
            }

            consumer.assign(partitions);
            consumer.seekToBeginning(partitions);

            while (messages.size() < messagesToConsider) {
                ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(200));

                if (records.isEmpty()) {
                    throw new RuntimeException("Expected " + messagesToConsider
                            + " messages but topic ran out after " + messages.size());
                }

                for (ConsumerRecord<String, String> record : records) {
                    if (messages.size() >= messagesToConsider) {
                        break;
                    }

                    try {
                        SortedMessage message = objectMapper.readValue(record.value(), SortedMessage.class);
                        messages.add(message);
                    } catch (JsonProcessingException e) {
                        throw new RuntimeException("Failed to parse sorted Kafka message", e);
                    }
                }
            }
        }

        messages.sort(Comparator.comparingInt(SortedMessage::getId));
        return messages;
    }
}