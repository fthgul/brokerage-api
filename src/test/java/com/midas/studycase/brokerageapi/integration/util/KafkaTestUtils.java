package com.midas.studycase.brokerageapi.integration.util;


import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;

import java.time.Duration;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Utility class for Kafka related testing operations.
 */
public class KafkaTestUtils {

    private static final Duration POLL_DURATION = Duration.ofSeconds(5);

    /**
     * Creates a Kafka consumer with the given configurations.
     *
     * @param bootstrapServers Kafka bootstrap servers.
     * @param groupId          Consumer group ID.
     * @param topic            Kafka topic to subscribe to.
     * @return A Kafka consumer.
     */
    public static Consumer<String, String> createConsumer(String bootstrapServers, String groupId, String topic) {
        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");

        Consumer<String, String> consumer = new KafkaConsumer<>(props);
        consumer.subscribe(Collections.singletonList(topic));
        return consumer;
    }

    /**
     * Polls messages from the Kafka topic using the provided consumer.
     *
     * @param consumer Kafka consumer.
     * @return Consumed records.
     */
    public static ConsumerRecords<String, String> getRecords(Consumer<String, String> consumer) {
        return consumer.poll(POLL_DURATION);
    }
}

