package com.mugsun.boot.datascope;

import com.mybatisflex.core.query.QueryWrapper;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * 数据权限策略注册器：按数据范围值分发到对应策略，构建 QueryWrapper 过滤条件。
 */
@Component
public class DataScopeStrategyRegistry {

	private final Map<Integer, DataScopeStrategy> strategies;

	public DataScopeStrategyRegistry(List<DataScopeStrategy> strategyList) {
		this.strategies = strategyList.stream()
			.collect(Collectors.toMap(DataScopeStrategy::scope, s -> s));
	}

	/**
	 * 按上下文数据范围构建过滤条件应用器。
	 *
	 * @return 应用到 QueryWrapper 的条件；null 表示不过滤（全部数据或无上下文）
	 */
	public Consumer<QueryWrapper> build(DataScopeContext.Scope ctx, String deptColumn, String userColumn) {
		if (ctx == null || ctx.dataScope() == null) {
			return null;
		}
		DataScopeStrategy strategy = strategies.get(ctx.dataScope());
		return strategy == null ? null : strategy.build(ctx, deptColumn, userColumn);
	}
}
