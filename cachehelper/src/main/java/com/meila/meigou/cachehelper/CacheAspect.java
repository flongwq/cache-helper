/**
 * 
 */
package com.meila.meigou.cachehelper;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.lang.reflect.Method;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.alibaba.fastjson.JSON;

/**
 * @author flong
 *
 */
@Component
@Aspect
public class CacheAspect {
	private static final Logger log = LoggerFactory.getLogger(CacheAspect.class);

	@Autowired
	private RedisAdapter redisAdapter;
	@Value("${meila.meigou.cachehelper.expiretime:3600}")
	private Integer expireTime;

	private final static String DEFAULT_TABLE = "MEILACACHE";

	@Pointcut("@annotation(com.meila.meigou.cachehelper.Cached)")
	public void cachedPoint() {
	}

	@Pointcut("@annotation(com.meila.meigou.cachehelper.CacheClear)")
	public void cacheClearPoint() {
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Around("cachedPoint()")
	public Object cached(ProceedingJoinPoint pjp) throws Throwable {
		String targetName = pjp.getTarget().getClass().toString();
		String methodName = pjp.getSignature().getName();
		Object[] arguments = pjp.getArgs();

		// 试图得到标注的Cached类
		Method method = getMethod(pjp);
		if (method == null) {
			return pjp.proceed();
		}
		Cached anno = method.getAnnotation(Cached.class);
		if (anno == null) {
			return pjp.proceed();
		}

		Object result = null;
		String cacheKey = null;
		String hashKey = null;
		// 使用table当做redis key，key参数当做hset的key
		if (anno.table() == null || "".equals(anno.table())) {
			cacheKey = DEFAULT_TABLE;
		} else {
			cacheKey = anno.table();
		}
		if (anno.key() == null || "".equals(anno.key())) {
			hashKey = getCacheKey(targetName, methodName, arguments);
		} else {
			hashKey = anno.key();
		}

		Class returnType = ((MethodSignature) pjp.getSignature()).getReturnType();
		// 试图获取cache中的值
		result = get(cacheKey, hashKey, returnType);
		if (result == null) {
			if ((arguments != null) && (arguments.length != 0)) {
				result = pjp.proceed(arguments);
			} else {
				result = pjp.proceed();
			}
			int expire = expireTime;

			if (anno.expireTime() > 0) {// 当注解中存在配置时，替换当前值
				expire = anno.expireTime();
			}
			put(cacheKey, hashKey, result, expire);
		}
		return result;
	}

	@AfterReturning("cacheClearPoint()")
	public void cacheClear(JoinPoint joinPoint) throws Throwable {
		// 试图得到标注的Cached类
		Method method = getMethod(joinPoint);
		if (method == null) {
			return;
		}
		CacheClear anno = method.getAnnotation(CacheClear.class);
		if (anno == null) {
			return;
		}

		if (anno.table() == null || "".equals(anno.table())) {
			return;
		}
		if (anno.table().indexOf(",") >= 0) {
			String[] tables = anno.table().split(",");
			for (String table : tables) {
				redisAdapter.del(table);
			}
		} else {
			redisAdapter.del(anno.table());
		}
	}

	/**
	 * 获取被拦截方法对象
	 * 
	 * MethodSignature.getMethod() 获取的是顶层接口或者父类的方法对象 而缓存的注解在实现类的方法上
	 * 所以应该使用反射获取当前对象的方法对象
	 */
	@SuppressWarnings("rawtypes")
	public Method getMethod(ProceedingJoinPoint pjp) {
		// 获取参数的类型
		Object[] args = pjp.getArgs();
		Class[] argTypes = new Class[pjp.getArgs().length];
		for (int i = 0; i < args.length; i++) {
			argTypes[i] = args[i].getClass();
		}
		Method method = null;
		try {
			method = pjp.getTarget().getClass().getMethod(pjp.getSignature().getName(), argTypes);
		} catch (NoSuchMethodException | SecurityException e) {
			log.error("CacheAspect cannot get method with expect annotation", e);
		}
		return method;

	}

	@SuppressWarnings("rawtypes")
	public Method getMethod(JoinPoint jp) {
		// 获取参数的类型
		Object[] args = jp.getArgs();
		Class[] argTypes = new Class[jp.getArgs().length];
		for (int i = 0; i < args.length; i++) {
			argTypes[i] = args[i].getClass();
		}
		Method method = null;
		try {
			method = jp.getTarget().getClass().getMethod(jp.getSignature().getName(), argTypes);
		} catch (NoSuchMethodException | SecurityException e) {
			log.error("CacheAspect cannot get method with expect annotation", e);
		}
		return method;

	}

	/**
	 * 获得cache key的方法，cache key是Cache中一个Element的唯一标识 cache key包括
	 * 包名+类名+方法名，如com.meila.service.UserServiceImpl.getAllUser
	 */
	private String getCacheKey(String targetName, String methodName, Object[] arguments) {
		StringBuffer sb = new StringBuffer();
		sb.append(targetName).append(".").append(methodName);
		if ((arguments != null) && (arguments.length != 0)) {
			for (int i = 0; i < arguments.length; i++) {
				sb.append(".").append(JSON.toJSONString(arguments[i]));
			}
		}
		return sb.toString();
	}

	public void put(final String key, final String field, Object value, int expireTime) {
		redisAdapter.hset(key.getBytes(), field.getBytes(), serialize(value));
		redisAdapter.expire(key, expireTime);
	}

	public <T> T get(final String key, final String hashKey, Class<T> elementType) {
		if (redisAdapter.hexists(key, hashKey)) {
			byte[] cacheValue = redisAdapter.hget(key.getBytes(), hashKey.getBytes());
			@SuppressWarnings("unchecked")
			T value = (T) unserialize(cacheValue);
			return value;
		}
		return null;
	}

	private static byte[] serialize(Object object) {
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

	private static Object unserialize(byte[] bytes) {
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
