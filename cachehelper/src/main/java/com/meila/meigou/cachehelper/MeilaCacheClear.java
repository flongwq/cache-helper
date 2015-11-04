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
public @interface MeilaCacheClear {
	String table() default "";// 根据数据库table name清空缓存

	MeilaCacheType type() default MeilaCacheType.None;// 缓存类型，当不为None时table失效
}
