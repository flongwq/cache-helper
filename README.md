# cache-helper
spring缓存插件

当多个系统同时调用同一个DB时，增加缓存易造成数据一致性问题，而通知所有子系统同步更新缓存也是较麻烦的事情。
cache-helper的目的是封装缓存的使用，各系统间只需要简易配置就实现了缓存的操作。

使用方式：
1、spring容器支持aop：
在配置文件中beans节点下增加  xmlns:aop="http://www.springframework.org/schema/aop"
xsi:schemaLocation中增加 http://www.springframework.org/schema/aop  http://www.springframework.org/schema/aop/spring-aop-3.0.xsd
通过 <aop:aspectj-autoproxy /> 实现aop支持
2、在service方法上增加annotation
	@Cached(key = "123")
	public ProductVO loadByProductCode(String productCode,
			SkuDisplayEnum skuDisplay) {
		Product product = productSlaveDao.selectByCode(productCode);
		return productToVo(product, skuDisplay);
	}
	这里要注意，返回的ProductVO必须可序列化
3、