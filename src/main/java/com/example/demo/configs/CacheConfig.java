package com.example.demo.configs;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.cache.CacheType;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;

@Configuration
@EnableCaching
public class CacheConfig {

    @Value("${spring.cache.type}")
    private CacheType cacheType;

    @Bean
    public CacheManager cacheManager(RedisConnectionFactory redisConnectionFactory) {
        RedisSerializationContext.SerializationPair<Object> jsonSerializer =
                RedisSerializationContext.SerializationPair.fromSerializer(new GenericJackson2JsonRedisSerializer());

        RedisCacheConfiguration cacheConfiguration = RedisCacheConfiguration.defaultCacheConfig()
                .serializeValuesWith(jsonSerializer);

        return RedisCacheManager.builder(redisConnectionFactory)
                .cacheDefaults(cacheConfiguration)
                .build();
    }

    @Bean
    public boolean isCacheEnabled() {
        return cacheType != CacheType.NONE;
    }
}
