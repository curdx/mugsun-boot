package com.mugsun.boot.datascope.strategy;

import com.mugsun.boot.datascope.DataScopeContext;
import com.mugsun.boot.datascope.DataScopeStrategy;
import com.mybatisflex.core.query.QueryWrapper;
import org.springframework.stereotype.Component;

import java.util.function.Consumer;

/** 仅本人数据：按本人列等于当前用户过滤 */
@Component
public class SelfDataScopeStrategy implements DataScopeStrategy {

	@Override
	public int scope() {
		return 4;
	}

	@Override
	public Consumer<QueryWrapper> build(DataScopeContext.Scope ctx, String deptColumn, String userColumn) {
		long userId = ctx.userId() == null ? -1L : ctx.userId();
		return query -> query.and(userColumn + " = ?", userId);
	}
}
