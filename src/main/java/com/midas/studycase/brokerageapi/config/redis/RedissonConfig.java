package com.midas.studycase.brokerageapi.config.redis;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.api.RedissonReactiveClient;
import org.redisson.codec.JsonJacksonCodec;
import org.redisson.config.Config;
import org.redisson.spring.data.connection.RedissonConnectionFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
@Profile("dev")
public class RedissonConfig {

    @Value("${spring.data.redis.host}")
    private String redisHost;

    @Value("${spring.data.redis.port}")
    private int redisPort;

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
        config.useSingleServer().setAddress("redis://" + redisHost +":" + redisPort);
        return Redisson.create(config);
    }

    @Bean(destroyMethod = "shutdown")
    public RedissonReactiveClient redissonReactiveClient() {
        Config config = new Config();


        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        JsonJacksonCodec jsonJacksonCodec = new JsonJacksonCodec(objectMapper);

        config.setCodec(jsonJacksonCodec);
        config.useSingleServer().setAddress("redis://" + redisHost +":" + redisPort);
        return Redisson.create(config).reactive();
    }
}

