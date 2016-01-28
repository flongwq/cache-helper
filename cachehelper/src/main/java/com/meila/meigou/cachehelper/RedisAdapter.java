/**
 * 
 */
package com.meila.meigou.cachehelper;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.exceptions.JedisConnectionException;
import redis.clients.jedis.exceptions.JedisException;

import com.meila.meigou.cachehelper.jedis.JedisAction;

/**
 * 封装redis相关操作
 * 
 * @author flong
 *
 */
public class RedisAdapter {
    private static final Logger log = LoggerFactory.getLogger(RedisAdapter.class);

    private JedisPool jedisPool;// 默认使用jedis实现

    private RedisAdapter() {

    }

    public RedisAdapter(JedisPool pool) {
        this.jedisPool = pool;
    }

    public <T> T execute(JedisAction<T> jedisAction) throws JedisException {
        Jedis jedis = null;
        boolean broken = false;
        try {
            jedis = jedisPool.getResource();
            return jedisAction.action(jedis);
        } catch (JedisConnectionException e) {
            log.error("Redis connection lost.", e);
            broken = true;
            throw e;
        } finally {
            closeResource(jedis, broken);
        }
    }

    /**
     * @param jedis
     * @param broken
     */
    private void closeResource(Jedis jedis, boolean broken) {
        if (jedis != null) {
            try {
                if (broken) {
                    jedisPool.returnBrokenResource(jedis);
                } else {
                    jedisPool.returnResource(jedis);
                }
            } catch (Exception e) {
                log.error("Error happen when return jedis to pool, try to close it directly.", e);
                jedis.close();
            }
        }
    }

    public Long del(final String key) {
        return execute(new JedisAction<Long>() {

            @Override
            public Long action(Jedis jedis) {
                return jedis.del(key);
            }
        });
    }

    public Long del(final String... keys) {
        return execute(new JedisAction<Long>() {

            @Override
            public Long action(Jedis jedis) {
                return jedis.del(keys);
            }
        });
    }

    public String get(final String key) {
        return execute(new JedisAction<String>() {

            @Override
            public String action(Jedis jedis) {
                return jedis.get(key);
            }
        });
    }

    public byte[] get(final byte[] key) {
        return execute(new JedisAction<byte[]>() {
            @Override
            public byte[] action(Jedis jedis) {
                return jedis.get(key);
            }
        });
    }

    public <T> T get(final String key, Class<T> clazz) {
        return execute(new JedisAction<T>() {
            @Override
            public T action(Jedis jedis) {
                return (T) unserialize(jedis.get(key.getBytes()));
            }
        });
    }

    public String set(final String key, final String value) {
        return execute(new JedisAction<String>() {

            @Override
            public String action(Jedis jedis) {
                return jedis.set(key, value);
            }
        });

    }

    public String set(final byte[] key, final byte[] value) {
        return execute(new JedisAction<String>() {

            @Override
            public String action(Jedis jedis) {
                return jedis.set(key, value);
            }
        });

    }

    public String set(final String key, final Object value) {
        return execute(new JedisAction<String>() {
            @Override
            public String action(Jedis jedis) {
                return jedis.set(key.getBytes(), serialize(value));
            }
        });

    }

    /**
     * @param key
     * @param seconds
     * @param value
     */
    public String setex(final String key, final int seconds, final String value) {
        return execute(new JedisAction<String>() {

            @Override
            public String action(Jedis jedis) {
                return jedis.setex(key, seconds, value);
            }
        });

    }

    public Long rpush(final String key, final String value) {
        return execute(new JedisAction<Long>() {

            @Override
            public Long action(Jedis jedis) {
                return jedis.rpush(key, value);
            }
        });

    }

    public Long zadd(final String key, final double score, final String member) {
        return execute(new JedisAction<Long>() {

            @Override
            public Long action(Jedis jedis) {
                return jedis.zadd(key, score, member);
            }
        });
    }

    public Long hset(final String key, final String field, final String value) {
        return execute(new JedisAction<Long>() {

            @Override
            public Long action(Jedis jedis) {
                return jedis.hset(key, field, value);
            }
        });
    }

    public Long hset(final byte[] key, final byte[] field, final byte[] value) {
        return execute(new JedisAction<Long>() {

            @Override
            public Long action(Jedis jedis) {
                return jedis.hset(key, field, value);
            }
        });
    }

    public String hget(final String key, final String field) {
        return execute(new JedisAction<String>() {

            @Override
            public String action(Jedis jedis) {
                return jedis.hget(key, field);
            }
        });
    }

    public byte[] hget(final byte[] key, final byte[] field) {
        return execute(new JedisAction<byte[]>() {

            @Override
            public byte[] action(Jedis jedis) {
                return jedis.hget(key, field);
            }
        });
    }

    public String hmset(final String key, final Map<String, String> hash) {
        return execute(new JedisAction<String>() {

            @Override
            public String action(Jedis jedis) {
                return jedis.hmset(key, hash);
            }
        });
    }

    public List<String> hmget(final String key, final String... fields) {
        return execute(new JedisAction<List<String>>() {

            @Override
            public List<String> action(Jedis jedis) {
                return jedis.hmget(key, fields);
            }
        });
    }

    /**
     *  
     */
    public Set<String> zrange(final String key, final long start, final long end) {
        return execute(new JedisAction<Set<String>>() {

            @Override
            public Set<String> action(Jedis jedis) {
                return jedis.zrange(key, start, end);
            }
        });

    }

    /**
     *  
     */
    public Set<String> zrevrange(final String key, final long start, final long end) {
        return execute(new JedisAction<Set<String>>() {

            @Override
            public Set<String> action(Jedis jedis) {
                return jedis.zrevrange(key, start, end);
            }
        });

    }

    public Long zrem(final String key,final String... member) {
        return execute(new JedisAction<Long>() {
            @Override
            public Long action(Jedis jedis) {
                return jedis.zrem(key, member);
            }
        });
    }

    public Long zcard(final String key) {
        return execute(new JedisAction<Long>() {

            @Override
            public Long action(Jedis jedis) {
                return jedis.zcard(key);
            }
        });

    }

    public long scard(final String key) {
        return execute(new JedisAction<Long>() {

            @Override
            public Long action(Jedis jedis) {
                return jedis.scard(key);
            }
        });
    }

    public boolean sismember(final String key, final String member) {
        return execute(new JedisAction<Boolean>() {

            @Override
            public Boolean action(Jedis jedis) {
                return jedis.sismember(key, member);
            }
        });
    }

    public long sadd(final String key, final String member) {
        return execute(new JedisAction<Long>() {

            @Override
            public Long action(Jedis jedis) {
                return jedis.sadd(key, member);
            }
        });
    }

    public long srem(final String key, final String member) {
        return execute(new JedisAction<Long>() {

            @Override
            public Long action(Jedis jedis) {
                return jedis.srem(key, member);
            }
        });
    }

    public long expire(final String key, final int seconds) {
        return execute(new JedisAction<Long>() {

            @Override
            public Long action(Jedis jedis) {
                return jedis.expire(key, seconds);
            }
        });
    }

    public boolean exists(final String key) {
        return execute(new JedisAction<Boolean>() {

            @Override
            public Boolean action(Jedis jedis) {
                return jedis.exists(key);
            }
        });
    }

    public boolean hexists(final String key, final String field) {
        return execute(new JedisAction<Boolean>() {

            @Override
            public Boolean action(Jedis jedis) {
                return jedis.hexists(key, field);
            }
        });
    }

    public long incr(final String key) {
        return execute(new JedisAction<Long>() {
            @Override
            public Long action(Jedis jedis) {
                return jedis.incr(key);
            }
        });
    }

    public long decr(final String key) {
        return execute(new JedisAction<Long>() {
            @Override
            public Long action(Jedis jedis) {
                return jedis.decr(key);
            }
        });
    }

    private byte[] serialize(Object object) {
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

    private Object unserialize(byte[] bytes) {
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
