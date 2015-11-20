/**
 * 
 */
package com.meila.meigou.cachehelper;

import java.util.HashMap;
import java.util.Map;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 ************************************************************
 * @类名 : TestService.java
 *
 * @DESCRIPTION :
 * @AUTHOR : flong
 * @DATE : 2015年11月9日
 ************************************************************
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = "classpath:spring-context.xml")
public class TestService {
    private Logger log = LoggerFactory.getLogger(TestService.class);

    @Autowired
    private RedisAdapter redisAdapter;

    @Test
    public void test() {
        String key = "testMap";
        Map<String, Object> map = new HashMap<String, Object>();
        map.put("key1", 1);
        map.put("key2", 2);
        redisAdapter.set(key, map);
        Map<String, Object> map2 = redisAdapter.get(key, Map.class);
        System.out.println(map2);
    }
}
