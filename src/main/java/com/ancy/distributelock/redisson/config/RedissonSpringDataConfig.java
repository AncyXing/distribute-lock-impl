package com.ancy.distributelock.redisson.config;

import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.spring.data.connection.RedissonConnectionFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;

@Configuration
public class RedissonSpringDataConfig {

   @Bean
   public RedissonConnectionFactory redissonConnectionFactory(RedissonClient redisson) {
       return new RedissonConnectionFactory(redisson);
   }

   @Bean(destroyMethod = "shutdown")
   public RedissonClient redisson() throws IOException {
       return Redisson.create();
   }

}