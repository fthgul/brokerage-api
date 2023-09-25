package com.midas.studycase.brokerageapi.integration.config;


import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.midas.studycase.brokerageapi.integration.config.property.RedisProperties;
import jakarta.annotation.PostConstruct;
import lombok.Getter;
import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.api.RedissonReactiveClient;
import org.redisson.codec.JsonJacksonCodec;
import org.redisson.config.Config;
import org.redisson.spring.data.connection.RedissonConnectionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.testcontainers.containers.GenericContainer;

@Getter
@Configuration
@Profile("test")
public class TestConfigRedis {

    @Autowired
    private RedisProperties redisProperties;

    @Autowired
    private GenericContainer<?> redisContainer;

    @PostConstruct
    public void setRedisHostAndPort() {
        redisProperties.setHost(redisContainer.getHost());
        redisProperties.setPort(redisContainer.getFirstMappedPort());
    }

    @Bean
    public RedissonConnectionFactory redissonConnectionFactory(RedissonClient redissonClient) {
        return new RedissonConnectionFactory(redissonClient);
    }

    @Bean(destroyMethod = "shutdown")
    public RedissonClient redissonClient() {
        Config config = new Config();
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        JsonJacksonCodec jsonJacksonCodec = new JsonJacksonCodec(objectMapper);
        config.setCodec(jsonJacksonCodec);
        config.useSingleServer().setAddress("redis://" + getRedisProperties().getHost() +":" + getRedisProperties().getPort());
        return Redisson.create(config);
    }

    @Bean(destroyMethod = "shutdown")
    public RedissonReactiveClient redissonReactiveClient() {
        Config config = new Config();


        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        JsonJacksonCodec jsonJacksonCodec = new JsonJacksonCodec(objectMapper);

        config.setCodec(jsonJacksonCodec);
        config.useSingleServer().setAddress("redis://" + getRedisProperties().getHost() +":" + getRedisProperties().getPort());
        return Redisson.create(config).reactive();
    }
}
