package com.mugsun.boot.datascope;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

/**
 * 数据权限切面：在标注 {@link DataScope} 的方法执行前，按当前用户数据范围（{@link DataScopeContext}）
 * 经策略注册器构建过滤条件写入 {@link DataScopeHolder}，方法内以 {@code DataScopeHolder.apply} 应用到查询，方法结束清理。
 */
@Aspect
@Component
public class DataScopeAspect {

	private final DataScopeStrategyRegistry registry;

	public DataScopeAspect(DataScopeStrategyRegistry registry) {
		this.registry = registry;
	}

	@Around("@annotation(dataScope)")
	public Object around(ProceedingJoinPoint joinPoint, DataScope dataScope) throws Throwable {
		DataScopeHolder.set(registry.build(DataScopeContext.get(), dataScope.deptColumn(), dataScope.userColumn()));
		try {
			return joinPoint.proceed();
		} finally {
			DataScopeHolder.clear();
		}
	}
}
