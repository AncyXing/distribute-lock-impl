package com.ancy.distributelock.redisson;

import com.ancy.distributelock.Lock;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;

import java.io.Closeable;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

@Slf4j
public class RedissonLock implements Lock, Closeable {
    private RLock lock;
    private long expireTime;

    public RedissonLock(RedissonClient redissonClient, String lockName, long expireTime) {
        this.expireTime = expireTime;
        this.lock = redissonClient.getLock(lockName);
    }

    @Override
    public boolean getLock() {
        try {
            // 尝试获取，没获取到可以直接返回
//            if (lock.tryLock(expireTime, TimeUnit.SECONDS)) {
//                return true;
//            }
            // 可以阻塞，leaseTime为-1才会触发看门狗机制，不设置默认为-1
            // 到了租赁时间，锁会被自动释放，其他线程可以获取到锁
            lock.lock(expireTime, TimeUnit.SECONDS);
            return true;

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public boolean releaseLock() {
        lock.unlock();
        log.info("redissonLock release lock success");
        return true;
    }

    @Override
    public void close() throws IOException {
        releaseLock();
    }
}
