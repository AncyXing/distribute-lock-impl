package com.ancy.distributelock.redis.lock;

import com.ancy.distributelock.Lock;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.RedisStringCommands;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.data.redis.core.types.Expiration;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Slf4j
public class RedisLock implements AutoCloseable, Lock {
    private final RedisTemplate redisTemplate;
    private final String key;
    private final String value;
    private final int expire;

    public RedisLock(RedisTemplate redisTemplate, String key, int expire) {
        this.redisTemplate = redisTemplate;
        this.key = key;
        this.expire = expire;
        this.value = UUID.randomUUID().toString();
    }

    @Override
    public boolean getLock() {
        RedisCallback<Boolean> callback = con -> {
            RedisStringCommands.SetOption setOption = RedisStringCommands.SetOption.ifAbsent();
            Expiration expiration = Expiration.seconds(expire);
            byte[] keyBytes = redisTemplate.getKeySerializer().serialize(key);
            byte[] valueBytes = redisTemplate.getValueSerializer().serialize(value);
            return con.set(keyBytes, valueBytes, expiration, setOption);
        };
        return (boolean) redisTemplate.execute(callback);
    }

    public boolean getLock2() {
        return Boolean.TRUE.equals(redisTemplate.opsForValue().setIfAbsent(key, value, expire, TimeUnit.SECONDS));
    }

    @Override
    public boolean releaseLock() {
        // 保证原子性
        String script = "if redis.call(\"get\",KEYS[1]) == ARGV[1] then\n" +
                "return redis.call(\"del\",KEYS[1])\n" +
                "else\n" +
                " return 0\n" +
                "end";
        RedisScript<Boolean> redisScript = RedisScript.of(script, Boolean.class);
        List<String> keys = Arrays.asList(key);
        Boolean result = (Boolean) redisTemplate.execute(redisScript, keys, value);
        log.info("release lock result : {}", result);
        return result;
    }

    @Override
    public void close() throws Exception {
        releaseLock();
    }
}
