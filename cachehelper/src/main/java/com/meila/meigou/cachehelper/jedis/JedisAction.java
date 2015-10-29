/**
 * 
 */
package com.meila.meigou.cachehelper.jedis;

import redis.clients.jedis.Jedis;

/**
 * @author flong
 *
 */
public interface JedisAction<T> {
	public T action(Jedis jedis);
}
