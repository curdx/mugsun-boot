package com.mugsun.boot.datascope.strategy;

import com.mugsun.boot.datascope.DataScopeContext;
import com.mugsun.boot.datascope.DataScopeStrategy;
import com.mybatisflex.core.query.QueryWrapper;
import org.springframework.stereotype.Component;

import java.util.function.Consumer;

/** 全部数据：不施加任何过滤 */
@Component
public class AllDataScopeStrategy implements DataScopeStrategy {

	@Override
	public int scope() {
		return 1;
	}

	@Override
	public Consumer<QueryWrapper> build(DataScopeContext.Scope ctx, String deptColumn, String userColumn) {
		return null;
	}
}
