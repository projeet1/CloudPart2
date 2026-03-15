package acp.cw2.service;

import acp.cw2.dto.TombstoneSummary;
import acp.cw2.dto.TransformMessage;
import acp.cw2.dto.TransformRequest;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;
import acp.cw2.dto.SplitterMessage;
import acp.cw2.dto.SplitterRequest;
import java.nio.charset.StandardCharsets;

@Service
public class ProcessingService {

    private final RabbitMqService rabbitMqService;
    private final RabbitTemplate rabbitTemplate;
    private final RedisStateService redisStateService;
    private final ObjectMapper objectMapper;

    public ProcessingService(RabbitMqService rabbitMqService,
                             RabbitTemplate rabbitTemplate,
                             RedisStateService redisStateService,
                             ObjectMapper objectMapper) {
        this.rabbitMqService = rabbitMqService;
        this.rabbitTemplate = rabbitTemplate;
        this.redisStateService = redisStateService;
        this.objectMapper = objectMapper;
    }

    public void transformMessages(TransformRequest request) {
        rabbitMqService.ensureQueueForExternalUse(request.getReadQueue());
        rabbitMqService.ensureQueueForExternalUse(request.getWriteQueue());

        int totalMessagesWritten = 0;
        int totalMessagesProcessed = 0;
        int totalRedisUpdates = 0;
        double totalValueWritten = 0.0;
        double totalAdded = 0.0;

        for (int i = 0; i < request.getMessageCount(); i++) {
            Message rawMessage = rabbitTemplate.receive(request.getReadQueue());

            if (rawMessage == null) {
                throw new RuntimeException("Expected " + request.getMessageCount()
                        + " messages but queue ran empty after " + i);
            }

            totalMessagesProcessed++;

            try {
                String body = new String(rawMessage.getBody(), StandardCharsets.UTF_8);
                TransformMessage incoming = objectMapper.readValue(body, TransformMessage.class);

                if ("TOMBSTONE".equals(incoming.getKey())) {
                    TombstoneSummary summary = new TombstoneSummary(
                            totalMessagesWritten + 1,
                            totalMessagesProcessed,
                            totalRedisUpdates,
                            totalValueWritten,
                            totalAdded
                    );

                    rabbitTemplate.convertAndSend(
                            request.getWriteQueue(),
                            objectMapper.writeValueAsString(summary)
                    );

                    redisStateService.clearAllTransformVersions();

                    totalMessagesWritten = 0;
                    totalMessagesProcessed = 0;
                    totalRedisUpdates = 0;
                    totalValueWritten = 0.0;
                    totalAdded = 0.0;

                    continue;
                }

                Integer existingVersion = redisStateService.getVersion(incoming.getKey());
                TransformMessage outgoing;

                if (existingVersion == null || incoming.getVersion() > existingVersion) {
                    redisStateService.setVersion(incoming.getKey(), incoming.getVersion());
                    totalRedisUpdates++;

                    outgoing = new TransformMessage(
                            incoming.getKey(),
                            incoming.getVersion(),
                            incoming.getValue() + 10.5
                    );
                    totalAdded += 10.5;
                } else {
                    outgoing = incoming;
                }

                rabbitTemplate.convertAndSend(
                        request.getWriteQueue(),
                        objectMapper.writeValueAsString(outgoing)
                );
                totalMessagesWritten++;
                totalValueWritten += outgoing.getValue();

            } catch (JsonProcessingException e) {
                throw new RuntimeException("Failed to process transformMessages payload", e);
            }
        }
    }

    public void splitter(SplitterRequest request) {
        rabbitMqService.ensureQueueForExternalUse(request.getReadQueue());

        for (int i = 0; i < request.getMessageCount(); i++) {
            Message rawMessage = rabbitTemplate.receive(request.getReadQueue());

            if (rawMessage == null) {
                throw new RuntimeException("Expected " + request.getMessageCount()
                        + " messages but queue ran empty after " + i);
            }

            try {
                String body = new String(rawMessage.getBody(), StandardCharsets.UTF_8);
                SplitterMessage message = objectMapper.readValue(body, SplitterMessage.class);

                boolean isEven = message.getId() % 2 == 0;

                String redisHash = isEven ? request.getRedisHashEven() : request.getRedisHashOdd();
                String countKey = isEven ? "count_even" : "count_odd";
                String sumKey = isEven ? "sum_even" : "sum_odd";
                String avgKey = isEven ? "average_even" : "average_odd";

                redisStateService.putJsonInHash(redisHash, message.getId(), body);

                long count = redisStateService.getLongValue(countKey);
                double sum = redisStateService.getDoubleValue(sumKey);

                count += 1;
                sum += message.getValue();

                double average = sum / count;
                double roundedAverage = Math.round(average * 100.0) / 100.0;

                redisStateService.setLongValue(countKey, count);
                redisStateService.setDoubleValue(sumKey, sum);
                redisStateService.setDoubleValue(avgKey, roundedAverage);

            } catch (JsonProcessingException e) {
                throw new RuntimeException("Failed to process splitter payload", e);
            }
        }
    }
}