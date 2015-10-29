/**
 * 
 */
package com.meila.meigou.cachehelper;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * @author flong
 *
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.METHOD })
@Documented
public @interface Cached {
	String table() default "";// 对应数据库中的table name

	String key() default "";// 不指定时默认生成key

	int expireTime() default 3600;
}
