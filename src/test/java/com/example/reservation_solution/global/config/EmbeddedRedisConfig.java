package com.example.reservation_solution.global.config;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import redis.embedded.RedisServer;

import java.io.IOException;

@Configuration
@Profile("test")
public class EmbeddedRedisConfig {

    private static final int REDIS_PORT = 6370;
    private RedisServer redisServer;

    @PostConstruct
    public void startRedis() throws IOException {
        redisServer = new RedisServer(REDIS_PORT);
        try {
            redisServer.start();
        } catch (Exception e) {
            // 이미 실행 중인 경우 무시
        }
    }

    @PreDestroy
    public void stopRedis() throws IOException {
        if (redisServer != null && redisServer.isActive()) {
            redisServer.stop();
        }
    }
}
