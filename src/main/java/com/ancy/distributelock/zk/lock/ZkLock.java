package com.ancy.distributelock.zk.lock;


import com.ancy.distributelock.Lock;
import lombok.extern.slf4j.Slf4j;
import org.apache.zookeeper.*;
import org.apache.zookeeper.data.Stat;
import org.springframework.util.CollectionUtils;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

@Slf4j
public class ZkLock implements AutoCloseable, Watcher {
    private ZooKeeper zk;

    String zNode;

    public ZkLock() throws IOException {
        this.zk = new ZooKeeper("localhost:2181", 10000, this);
    }

    public boolean getLock(String lockName) throws IOException {
        try {
            // 创建业务根节点? 这里有并发问题，可能同时创建多个持久节点
            Stat exists = zk.exists("/" + lockName, false);
            if (exists == null) {
                zk.create("/" + lockName, lockName.getBytes(),
                        ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
            }
            // 创建瞬时有序节点
            zNode = zk.create("/" + lockName + "/" + lockName + "_", lockName.getBytes(),
                    ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.EPHEMERAL_SEQUENTIAL);
            // 获取所有的临时有序节点
            List<String> children = zk.getChildren("/" + lockName, false);
            if (!CollectionUtils.isEmpty(children)) {
                // 子节点排序
                Collections.sort(children);
                String firstNode = children.get(0);
                // 如果创建的节点是第一个子节点，则获取锁
                if (zNode.endsWith(firstNode)) {
                    return true;
                }
                // 不是 则需要监听前一个节点
                String miniNode = firstNode;
                // 遍历所有的临时有序节点，从小到大
                for (String child : children) {
                    if (zNode.endsWith(child)) {
                        zk.exists("/" + lockName + "/" + miniNode, true);
                        break;
                    } else {
                        // 更新miniNode
                        miniNode = child;
                    }
                }
            } else {
                return false;
            }
            synchronized (this) {
                // 等待时释放锁
                wait();
            }
            return true;
        } catch (KeeperException e) {
            throw new RuntimeException(e);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void close() throws Exception {
        zk.delete(zNode, -1);
        zk.close();
        log.info("锁已释放");
    }

    @Override
    public void process(WatchedEvent watchedEvent) {
        // 被监听节点发生变化
        if (watchedEvent.getType() == Event.EventType.NodeDeleted) {
            synchronized (this) {
                notify();
            }
        }
    }
}
