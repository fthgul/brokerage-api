package com.midas.studycase.brokerageapi.integration.config.property;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "spring.data.redis")
@Component
@Profile("test")
public class RedisProperties {
    private String host;
    private Integer port;
}
