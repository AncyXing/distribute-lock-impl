package com.ancy.distributelock;

import com.ancy.distributelock.curator.CuratorLock;
import com.ancy.distributelock.redis.lock.RedisLock;
import com.ancy.distributelock.redisson.RedissonLock;
import com.ancy.distributelock.zk.lock.ZkLock;
import lombok.extern.slf4j.Slf4j;
import org.apache.curator.RetryPolicy;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.junit.jupiter.api.Test;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@SpringBootTest
class DistributeLockApplicationTests {
    @Autowired
    private RedisTemplate redisTemplate;

    @Autowired
    private RedissonClient redissonClient;

    @Test
    public void contextLoads() {
    }

    @Test
    void testRedissonLock() throws Exception {
        int size = 10;
        List<Thread> threads = new ArrayList<>();
        for (int i = 0; i < size; i++) {
            String lockName = "test_redisson_lock";
            Thread t = new Thread(() -> {
                try (RedissonLock redissonLock = new RedissonLock(redissonClient, lockName, 3)) {
                    log.info("准备获取锁");
                    boolean result = redissonLock.getLock();
                    log.info("获取锁结果：:{}", result);
                    if (result) {
                        Thread.sleep(2000);
                        log.info("准备释放锁");
                    }
                } catch (Exception e) {
                    log.error(e.getMessage(), e);
                }
            });
            threads.add(t);
        }

        for (Thread thread : threads) {
            thread.start();
        }

        for (Thread thread : threads) {
            thread.join();
        }
    }

    @Test
    void testCuratorLock() throws Exception {
        RetryPolicy retryPolicy = new ExponentialBackoffRetry(1000, 3);
        CuratorFramework client = CuratorFrameworkFactory.newClient("localhost:2181", retryPolicy);
        client.start();
        String lockPath = "/test/curator/lock";
        try (CuratorLock curatorLock = new CuratorLock(client, lockPath, 3)) {
            boolean lock1 = curatorLock.getLock();
            log.info("获取锁结果：:{}", lock1);
            Thread.sleep(30000);
            log.info("准备释放锁");
        }
    }

    @Test
    void testZkLock() throws Exception {
        ZkLock zkLock = new ZkLock();
        boolean lock1 = zkLock.getLock("lock1");
        log.info("获取锁结果：:{}", lock1);
        zkLock.close();
    }

    @Test
    void testRedisLock() {
        try (RedisLock redisLock = new RedisLock(redisTemplate, "test", 5)) {
            if (redisLock.getLock()) {
                log.info("获取锁成功");
                try {
                    Thread.sleep(2000);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
