package com.midas.studycase.brokerageapi.integration.config;

import com.midas.studycase.brokerageapi.config.kafka.KafkaConfig;
import com.midas.studycase.brokerageapi.config.kafka.KafkaProperties;
import jakarta.annotation.PostConstruct;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.testcontainers.containers.KafkaContainer;

@Getter
@Configuration
@Profile("test")
public class TestConfigKafka extends KafkaConfig {

    @Autowired
    private KafkaProperties kafkaProperties;

    @Autowired
    private KafkaContainer kafkaContainer;

    public TestConfigKafka(KafkaProperties properties) {
        super(properties);
    }

    @PostConstruct
    public void setBootstrapServers() {
        kafkaProperties.setBootstrapServers(kafkaContainer.getBootstrapServers());
    }
}
