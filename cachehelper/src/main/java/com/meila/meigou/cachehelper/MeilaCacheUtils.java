/**
 * 
 */
package com.meila.meigou.cachehelper;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 ************************************************************
 * @类名 : MeilaCacheUtils.java
 *
 * @DESCRIPTION :
 * @AUTHOR : flong
 * @DATE : 2015年11月5日
 ************************************************************
 */
@Component
public class MeilaCacheUtils {
    private static final Logger log = LoggerFactory.getLogger(MeilaCacheUtils.class);

    @Autowired
    private RedisAdapter redisAdapter;
    @Value("${meila.meigou.cachehelper.expiretime:3600}")
    private Integer expireTime;

    @Value("${meila.meigou.cachehelper.projectName:unknown}")
    private String projectName;
    private Integer countExpireTime = 24 * 60 * 60;
    private String KEY_COUNT_TOTAL = "meila_meigou_cacheutil_count_total_";
    private String KEY_COUNT_MISS = "meila_meigou_cacheutil_count_miss_";

    public void del(MeilaCacheType cacheType, String key) throws Throwable {
        String cacheKey = cacheType.getPrefix() + key;
        redisAdapter.del(cacheKey);
    }

    public byte[] serialize(Object object) {
        ObjectOutputStream oos = null;
        ByteArrayOutputStream baos = null;
        try {
            // 序列化
            baos = new ByteArrayOutputStream();
            oos = new ObjectOutputStream(baos);
            oos.writeObject(object);
            byte[] bytes = baos.toByteArray();
            return bytes;
        } catch (Exception e) {
            log.error("object is not serializable", e);
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    public Object unserialize(byte[] bytes) {
        ByteArrayInputStream bais = null;
        try {
            // 反序列化
            bais = new ByteArrayInputStream(bytes);
            ObjectInputStream ois = new ObjectInputStream(bais);
            return ois.readObject();
        } catch (Exception e) {
            log.error("object fail to unserialize", e);
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    public long addTotal() {
        String keyTotal = KEY_COUNT_TOTAL + projectName;
        String keyMiss = KEY_COUNT_MISS + projectName;
        long total = redisAdapter.incr(keyTotal);
        if (total == Long.MAX_VALUE) {// 重置计数器
            redisAdapter.set(keyTotal, "1");
            redisAdapter.set(keyMiss, "0");
        }
        redisAdapter.expire(keyTotal, countExpireTime);
        return total;
    }

    public long addMiss() {
        String keyTotal = KEY_COUNT_TOTAL + projectName;
        String keyMiss = KEY_COUNT_MISS + projectName;
        long miss = redisAdapter.incr(keyMiss);
        if (miss == Long.MAX_VALUE) {// 重置计数器
            redisAdapter.set(keyTotal, "1");
            redisAdapter.set(keyMiss, "0");
        }
        redisAdapter.expire(keyMiss, countExpireTime);
        return miss;
    }

    public float getHitRate() {
        String keyTotal = KEY_COUNT_TOTAL + projectName;
        String keyMiss = KEY_COUNT_MISS + projectName;
        String total = redisAdapter.get(keyTotal);
        if (total == null) {
            return 0;
        }
        String miss = redisAdapter.get(keyMiss);
        if (miss == null) {
            return 0;
        }
        return (float) (Long.parseLong(total) - Long.parseLong(miss)) / Long.parseLong(total);
    }
}
