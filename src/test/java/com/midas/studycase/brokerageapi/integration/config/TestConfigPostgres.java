package com.midas.studycase.brokerageapi.integration.config;

import com.midas.studycase.brokerageapi.integration.config.property.PostgresProperties;
import jakarta.annotation.PostConstruct;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.testcontainers.containers.PostgreSQLContainer;

@Getter
@Configuration
@Profile("test")
public class TestConfigPostgres {

    @Autowired
    private PostgresProperties dbProperties;

    @Autowired
    private PostgreSQLContainer<?> postgreSQLContainer;


    @PostConstruct
    public void setDbUrl() {
        dbProperties.setUrl(postgreSQLContainer.getJdbcUrl());
    }
}
