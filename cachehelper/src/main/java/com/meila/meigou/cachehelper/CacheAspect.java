/**
 * 
 */
package com.meila.meigou.cachehelper;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.lang.reflect.Method;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.After;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.alibaba.fastjson.JSON;

/**
 * @author flong
 *
 */
@Component
@Aspect
public class CacheAspect {
	@Autowired
	private RedisAdapter redisAdapter;

	private final static String DEFAULT_TABLE = "MEILACACHE";

	@Pointcut("@annotation(com.vdlm.aop.Cached)")
	public void cachedPoint() {
	}

	@Pointcut("@annotation(com.vdlm.aop.CacheClear)")
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
			put(cacheKey, hashKey, result);
		}
		return result;
	}

	@After("cacheClearPoint()")
	public void cacheClear(ProceedingJoinPoint pjp) throws Throwable {
		// 试图得到标注的Cached类
		Method method = getMethod(pjp);
		CacheClear anno = method.getAnnotation(CacheClear.class);
		if (anno == null) {
			return;
		}

		if (anno.table() == null || "".equals(anno.table())) {
			return;
		}

		redisAdapter.del(anno.table());
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
		} catch (NoSuchMethodException e) {
			e.printStackTrace();
		} catch (SecurityException e) {
			e.printStackTrace();
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

	public void put(final String key, final String field, Object value) {
		redisAdapter.hset(key.getBytes(), field.getBytes(), serialize(value));
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
			e.printStackTrace();
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
			throw new RuntimeException(e.getMessage(), e);
		}
	}
}
