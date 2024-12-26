package com.ancy.distributelock;

public interface Lock {
    /**
     * 获取锁
     * @return
     */
    public boolean getLock();

    /**
     * 释放锁
     * @return
     */
    public boolean releaseLock();
}
