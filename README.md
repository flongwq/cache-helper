# cache-helper
spring缓存插件

当多个系统同时调用同一个DB时，增加缓存易造成数据一致性问题，而通知所有子系统同步更新缓存也是较麻烦的事情。
cache-helper的目的是封装缓存的使用，各系统间只需要简易配置就实现了缓存的操作。

使用方式：
1、spring容器支持aop：
	在配置文件中beans节点下增加  xmlns:aop="http://www.springframework.org/schema/aop"
	xsi:schemaLocation中增加 http://www.springframework.org/schema/aop  http://www.springframework.org/schema/aop/spring-aop-3.0.xsd
	增加 <aop:aspectj-autoproxy />

2、导入cachehelper包
已经在私库nexus上发布 com.meila.meigou.cachehelper 
在spring中注入 <context:component-scan base-package="com.meila.meigou.cachehelper" />

3、注入redis
    <bean id="jedisPool" class="com.meila.meigou.cachehelper.JedisPoolHelper" autowire="byType">
        <constructor-arg name="poolConfig">
            <bean class="org.apache.commons.pool2.impl.GenericObjectPoolConfig">
                <property name="testOnBorrow" value="true"/>
                <property name="maxIdle" value="20"/>
                <property name="minIdle" value="10"/>
                <property name="maxTotal" value="50"/>
            </bean>
        </constructor-arg>
        <constructor-arg name="host" value="${redis.host}"/>
        <constructor-arg name="port" value="${redis.port}"/>
        <constructor-arg name="password" value="${redis.pass}"/>
        <constructor-arg name="timeout" value="${redis.timeout}"/>
        <constructor-arg name="database" value="${redis.default.db}"/>
    </bean>
    
    <bean id="redisAdapter" class="com.meila.meigou.cachehelper.RedisAdapter" autowire="byType">
        <constructor-arg name="pool" ref="jedisPool"/>
    </bean>

4、在service方法上增加annotation来实现缓存控制
    增加缓存
    @Cached(table = "product_info",key="iamkey",expireTime=300)
    public ProductVO loadByProductCode(String productCode,SkuDisplayEnum skuDisplay) {
	Product product = productSlaveDao.selectByCode(productCode);
	return productToVo(product, skuDisplay);
    }
    这里要注意，返回的ProductVO必须可序列化
    table填写数据库中表名，如果该操作不需要同步更新缓存则table参数不填或为空。
    key可不填由cachehelper自动生成
    expireTime单位秒，为缓存时间，默认值1小时。可以在系统配置中增加meila.meigou.cachehelper.expiretime配置项来修改默认时间。

    @CacheClear(table = "product_info")
    public ProductVO loadByProductCode(String productCode,SkuDisplayEnum skuDisplay) {
	Product product = productSlaveDao.selectByCode(productCode);
	return productToVo(product, skuDisplay);
    }
    @CacheClear的作用是当需要增加、修改、删除数据库中数据时，同步清空缓存数据。
    唯一的参数是table，输入受影响的数据库表名，当存在多个表需要更新时，使用逗号分隔     @CacheClear(table = "product_info,user_info")
	
