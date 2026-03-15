package acp.cw2.controller;

import acp.cw2.service.KafkaService;
import acp.cw2.service.RabbitMqService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import acp.cw2.dto.SortedMessage;
import java.util.List;

@RestController
@RequestMapping("/api/v1/acp")
public class AcpController {

    private final RabbitMqService rabbitMqService;
    private final KafkaService kafkaService;

    public AcpController(RabbitMqService rabbitMqService, KafkaService kafkaService) {
        this.rabbitMqService = rabbitMqService;
        this.kafkaService = kafkaService;
    }

    @PutMapping("/messages/rabbitmq/{queueName}/{messageCount}")
    public ResponseEntity<Void> writeMessagesToRabbitMq(
            @PathVariable String queueName,
            @PathVariable int messageCount) {
        rabbitMqService.sendCounterMessages(queueName, messageCount);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/messages/rabbitmq/{queueName}/{timeoutInMsec}")
    public ResponseEntity<List<String>> readMessagesFromRabbitMq(
            @PathVariable String queueName,
            @PathVariable long timeoutInMsec) {
        List<String> messages = rabbitMqService.readMessagesForDuration(queueName, timeoutInMsec);
        return ResponseEntity.ok(messages);
    }

    @GetMapping("/messages/sorted/rabbitmq/{queueName}/{messagesToConsider}")
    public ResponseEntity<List<SortedMessage>> readSortedMessagesFromRabbitMq(
            @PathVariable String queueName,
            @PathVariable int messagesToConsider) {

        List<SortedMessage> messages = rabbitMqService.readAndSortMessages(queueName, messagesToConsider);
        return ResponseEntity.ok(messages);
    }

    @PutMapping("/messages/kafka/{writeTopic}/{messageCount}")
    public ResponseEntity<Void> writeMessagesToKafka(
            @PathVariable String writeTopic,
            @PathVariable int messageCount) {
        kafkaService.sendCounterMessages(writeTopic, messageCount);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/messages/kafka/{readTopic}/{timeoutInMsec}")
    public ResponseEntity<List<String>> readMessagesFromKafka(
            @PathVariable String readTopic,
            @PathVariable long timeoutInMsec) {

        List<String> messages = kafkaService.readMessagesForDuration(readTopic, timeoutInMsec);
        return ResponseEntity.ok(messages);
    }


}