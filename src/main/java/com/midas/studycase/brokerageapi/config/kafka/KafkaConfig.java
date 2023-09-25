package com.midas.studycase.brokerageapi.config.kafka;

import com.midas.studycase.brokerageapi.model.event.OrderEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.*;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.support.serializer.ErrorHandlingDeserializer;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.kafka.support.serializer.JsonSerializer;

import java.util.HashMap;
import java.util.Map;

/**
 * Kafka configuration class for setting up producer and consumer properties.
 */
@Configuration
@RequiredArgsConstructor
@EnableKafka
@Slf4j
@Profile("dev")
public class KafkaConfig {

    private final KafkaProperties properties;
    public static final String BUY_ORDERS_TOPIC = "buy_intent_orders";
    public static final String SELL_ORDERS_TOPIC = "sell_intent_orders";
    public static final String CANCELLED_ORDERS_TOPIC = "cancelled_intent_orders";
    public static final String USER_NOTIFICATION_TOPIC = "user-notifications";
    public static final String STOCK_ACTION_CONSUMER_GROUP_ID = "stock-action-handler-group";

    /**
     * Configures the producer factory for sending OrderEvent objects.
     *
     * @return ProducerFactory for OrderEvent
     */
    @Bean
    public ProducerFactory<String, OrderEvent> producerFactory() {
        Map<String, Object> configProps = new HashMap<>();
        configProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, properties.getBootstrapServers());
        configProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        configProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);
        return new DefaultKafkaProducerFactory<>(configProps);
    }


    /**
     * Configures the producer factory for sending OrderEvent objects.
     *
     * @return ProducerFactory for OrderEvent
     */
    @Bean
    public ProducerFactory<String, String> producerNotifyFactory() {
        Map<String, Object> configProps = new HashMap<>();
        configProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, properties.getBootstrapServers());
        configProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        configProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        return new DefaultKafkaProducerFactory<>(configProps);
    }

    /**
     * Provides a Kafka template bean for sending OrderEvent objects.
     *
     * @return KafkaTemplate for OrderEvent
     */
    @Bean
    public KafkaTemplate<String, OrderEvent> kafkaTemplate() {
        return new KafkaTemplate<>(producerFactory());
    }

    /**
     * Provides a Kafka template bean for sending OrderEvent objects.
     *
     * @return KafkaTemplate for OrderEvent
     */
    @Bean
    @Qualifier("notifyKafkaTemplate")
    public KafkaTemplate<String, String> notifyKafkaTemplate() {
        return new KafkaTemplate<>(producerNotifyFactory());
    }

    /**
     * Configures the Kafka listener container factory for consuming OrderEvent objects.
     *
     * @param consumerFactory ConsumerFactory for OrderEvent
     * @return KafkaListenerContainerFactory for OrderEvent
     */
    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, OrderEvent> kafkaListenerContainerFactory(ConsumerFactory<String, OrderEvent> consumerFactory) {
        ConcurrentKafkaListenerContainerFactory<String, OrderEvent> factory = new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory);
        factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.MANUAL_IMMEDIATE);
        DefaultErrorHandler commonErrorHandler = new DefaultErrorHandler((record, exception) ->
                log.error(String.format("There is an exception occurred while consuming order from kafka topic:%s, partition:%s, recordOffset:%s"
                        , record.topic(), record.partition(), record.offset()), exception));
        commonErrorHandler.setCommitRecovered(true);
        factory.setCommonErrorHandler(commonErrorHandler);
        return factory;
    }

    /**
     * Provides a consumer factory bean for consuming OrderEvent objects.
     *
     * @return ConsumerFactory for OrderEvent
     */
    @Bean
    public ConsumerFactory<String, OrderEvent> consumerFactory() {
        return new DefaultKafkaConsumerFactory<>(consumerProps(), new StringDeserializer(),
                new ErrorHandlingDeserializer<>(new JsonDeserializer<>(OrderEvent.class, false)));
    }

    /**
     * Sets up consumer properties for Kafka.
     *
     * @return Map of consumer properties
     */
    private Map<String, Object> consumerProps() {
        Map<String, Object> configuration = new HashMap<>();
        configuration.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, properties.getBootstrapServers());
        configuration.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        configuration.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class);
        return configuration;
    }
}
