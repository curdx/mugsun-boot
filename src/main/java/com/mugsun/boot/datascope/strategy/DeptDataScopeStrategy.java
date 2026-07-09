package com.mugsun.boot.datascope.strategy;

import com.mugsun.boot.datascope.DataScopeContext;
import com.mugsun.boot.datascope.DataScopeStrategy;
import com.mybatisflex.core.query.QueryWrapper;
import org.springframework.stereotype.Component;

import java.util.function.Consumer;

/** 本部门数据：按部门列等于当前用户部门过滤（无部门则不可见） */
@Component
public class DeptDataScopeStrategy implements DataScopeStrategy {

	@Override
	public int scope() {
		return 2;
	}

	@Override
	public Consumer<QueryWrapper> build(DataScopeContext.Scope ctx, String deptColumn, String userColumn) {
		long deptId = ctx.deptId() == null ? -1L : ctx.deptId();
		return query -> query.and(deptColumn + " = ?", deptId);
	}
}
