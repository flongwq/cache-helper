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
}
