package com.midas.studycase.brokerageapi.integration.service.producer;

import com.midas.studycase.brokerageapi.integration.config.KafkaContainerConfig;
import com.midas.studycase.brokerageapi.config.kafka.KafkaConfig;
import com.midas.studycase.brokerageapi.config.kafka.KafkaProperties;
import com.midas.studycase.brokerageapi.integration.config.TestConfigKafka;
import com.midas.studycase.brokerageapi.integration.util.KafkaTestUtils;
import com.midas.studycase.brokerageapi.service.producer.NotifyProducerService;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

@ActiveProfiles("test")
@SpringBootTest(classes = {NotifyProducerService.class, KafkaContainerConfig.class, TestConfigKafka.class, KafkaProperties.class})
@Testcontainers
@TestPropertySource(locations = "classpath:application-test.properties")
class NotifyProducerIntegrationTest {

    private static final Logger logger = LoggerFactory.getLogger(NotifyProducerIntegrationTest.class);

    @Autowired
    private NotifyProducerService notifyProducerService;

    @Autowired
    private TestConfigKafka testConfigKafka;

    private Consumer<String, String> consumer;

    @BeforeEach
    void setUp() {
        consumer = KafkaTestUtils.createConsumer(
                testConfigKafka.getKafkaProperties().getBootstrapServers(),
                "testGroup", KafkaConfig.USER_NOTIFICATION_TOPIC);
    }

    @Test
    @DisplayName("Given a user ID and a message, when notifyUser is called, then it should send a notification message to the user")
    void testNotifyUser() {
        logger.info("Starting testNotifyUser test");
        Long userId = 1L;
        String message = "Test Notification Message";

        logger.info("Sending notification to user: {} with message: {}", userId, message);
        assertDoesNotThrow(() -> notifyProducerService.notifyUser(userId, message), "Sending notification failed");

        logger.info("Consuming the message from the topic");
        ConsumerRecords<String, String> records = KafkaTestUtils.getRecords(consumer);
        assertThat(records.count()).isEqualTo(1); // Ensure we have consumed one message

        String consumedMessage = records.iterator().next().value();
        logger.info("Verifying the consumed message: {}", consumedMessage);
        assertThat(consumedMessage).isEqualTo(message); // Verify the consumed message is the one we sent

        consumer.close();
        logger.info("testNotifyUser test completed");
    }
}
