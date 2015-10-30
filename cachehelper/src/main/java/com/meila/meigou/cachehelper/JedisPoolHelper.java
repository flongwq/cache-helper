/**
 * 
 */
package com.meila.meigou.cachehelper;

import org.apache.commons.pool2.impl.GenericObjectPoolConfig;

import redis.clients.jedis.JedisPool;

/**
 * 增加一个适配器主要是为了蛋疼的redis密码为空问题
 * 
 * @author flong
 *
 */
public class JedisPoolHelper extends JedisPool {
	public JedisPoolHelper(final GenericObjectPoolConfig poolConfig, final String host, int port, int timeout,
			final String password, final int database) {
		super(poolConfig, host, port, timeout, (password == null || "".equals(password)) ? null : password, database,
				null);
	}
}
