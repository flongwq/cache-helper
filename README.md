# cache-helper
spring缓存插件
==============
当多个系统同时调用同一个DB时，增加缓存易造成数据一致性问题，而通知所有子系统同步更新缓存也是较麻烦的事情。
cache-helper的目的是封装缓存的使用，各系统间只需要简易配置就实现了缓存的操作。

#使用方式：
##spring容器支持aop：
	在配置文件中beans节点下增加  xmlns:aop="http://www.springframework.org/schema/aop"
	xsi:schemaLocation中增加 http://www.springframework.org/schema/aop  		http://www.springframework.org/schema/aop/spring-aop-3.0.xsd
	增加 <aop:aspectj-autoproxy />

##导入cachehelper包
	已经在私库nexus上发布 com.meila.meigou.cachehelper 
	在spring中注入 <context:component-scan base-package="com.meila.meigou.cachehelper" />

##注入redis
```	
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
```
##在service方法上增加annotation来实现缓存控制
	为了适应特殊场景，除默认方式外提供了两种特殊方式
	
	* 注意事项
	缓存对象必须可序列化
	所有方式都支持过期时间参数expireTime，单位秒，默认值1小时。可以在系统配置中增加meila.meigou.cachehelper.expiretime配置项来修改默认时间。
	* 默认方式
	默认方式会自动生成缓存Key，适用于不关心数据一致性的场景，例如有一些全天推送的固定新闻列表需要放入缓存。
	@MeilaCached
	public NewsVO getNews(String code) {
	    return new NewsVO();
	}
	
	* 指定操作类型（包括商品Product、卖家Seller、评论Comment）
	商品详情页需要将商品、卖家等信息的数据缓存，并在DML操作时进行缓存更新，同时考虑到其它系统同时维护缓存，需要将Key固定格式。
	增加缓存时指定类型为Product，@MeilaCacheParam用于指定缓存key，不加@MeilaCacheParam时会自动使用method中的第一个param。
	@MeilaCached(type = MeilaCacheType.Product)
	public ProductVO loadByProductCode(@MeilaCacheParam String productCode, SkuDisplayEnum skuDisplay) {
		Product product = productSlaveDao.selectByCode(productCode);
		return productToVo(product, skuDisplay);
	}
	清除缓存时使用同样的type，不加@MeilaCacheParam时会自动使用method中的第一个param。
	@MeilaCacheClear(type = MeilaCacheType.Product)
	public void updateProduct(@MeilaCacheParam String productCode) {
		update(productCode);
	}

	* 以数据库表名关联
	对于缓存key没有特定要求，大家只需要一张数据库表相关DML操作都自动更新缓存时，使用指定table的方式。
	增加缓存时自行指定table，需要自定义key可以自行输入，但是清除缓存时同一table下的所有key都被清除。
	table填写数据库中表名，如果该操作不需要同步更新缓存则table参数不填或为空。
	@MeilaCached(table = "product_info",key="iamkey",expireTime=300)
	public ProductVO loadByProductCode(String productCode,SkuDisplayEnum skuDisplay) {
	    Product product = productSlaveDao.selectByCode(productCode);
	    return productToVo(product, skuDisplay);
	}
	清除缓存，指定与@MeilaCached相同的table。
	@CacheClear的作用是当需要增加、修改、删除数据库中数据时，同步清空缓存数据。唯一的参数是table，输入受影响的数据库表名，当存在多个表需要更新时，使用逗号分隔     @MeilaCacheClear(table = "product_info,user_info")
    	@MeilaCacheClear(table = "product_info")
    	public ProductVO loadByProductCode(String productCode,SkuDisplayEnum skuDisplay) {
	    Product product = productSlaveDao.selectByCode(productCode);
	    return productToVo(product, skuDisplay);
	}

##不使用注解直接清空缓存	
	@Autowired
	private MeilaCacheUtils cacheUtils;
	cacheUtils.del(MeilaCacheType.Product, "productcode");
	
