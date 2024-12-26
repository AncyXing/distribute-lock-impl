package com.ancy.distributelock.curator;

import com.ancy.distributelock.Lock;
import lombok.extern.slf4j.Slf4j;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.recipes.locks.InterProcessMutex;

import java.io.Closeable;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

@Slf4j
public class CuratorLock implements Lock , Closeable {
    private final InterProcessMutex lockMutex;
    private final long expireTime;

    public CuratorLock(CuratorFramework client, String lockPath, long expireTime) {
        this.expireTime = expireTime;
        this.lockMutex = new InterProcessMutex(client, lockPath);
    }

    @Override
    public boolean getLock() {
        try {
            if (lockMutex.acquire(expireTime, TimeUnit.SECONDS)) {
                return true;
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return false;
    }

    @Override
    public boolean releaseLock() {
        try {
            lockMutex.release();
            log.info("CuratorLock release lock success");
            return true;
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return false;
        }
    }

    @Override
    public void close() throws IOException {
        releaseLock();
    }
}
