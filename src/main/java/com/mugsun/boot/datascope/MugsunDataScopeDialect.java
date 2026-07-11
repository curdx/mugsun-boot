package com.mugsun.boot.datascope;

import com.mybatisflex.core.dialect.KeywordWrap;
import com.mybatisflex.core.dialect.LimitOffsetProcessor;
import com.mybatisflex.core.dialect.OperateType;
import com.mybatisflex.core.dialect.impl.CommonsDialectImpl;
import com.mybatisflex.core.query.CPI;
import com.mybatisflex.core.query.QueryTable;
import com.mybatisflex.core.query.QueryWrapper;

import java.util.List;

/**
 * 数据权限方言：覆写 Flex 官方零侵入行级授权钩子 {@code prepareAuth}，在 SQL 生成期对受控表自动注入数据权限条件。
 * <p>基类构造严格对齐 Flex 内置 PostgreSQL 方言（{@code DOUBLE_QUOTATION} + {@code POSTGRESQL}，字节码实证），保双引号标识符不破坏 PG。
 * <p>SELECT 覆盖读隔离（分页 count 与 data 两条 SQL 同过此钩子，条件一致）；仅 {@link DataScopeContext#isActive() 生效} 时注入。
 */
public class MugsunDataScopeDialect extends CommonsDialectImpl {

	private final DataScopeEngine engine;

	public MugsunDataScopeDialect(DataScopeEngine engine) {
		super(KeywordWrap.DOUBLE_QUOTATION, LimitOffsetProcessor.POSTGRESQL);
		this.engine = engine;
	}

	@Override
	public void prepareAuth(QueryWrapper queryWrapper, OperateType operateType) {
		if (operateType != OperateType.SELECT) {
			return;
		}
		List<QueryTable> tables = CPI.getQueryTables(queryWrapper);
		if (tables == null || tables.isEmpty()) {
			return;
		}
		for (QueryTable table : tables) {
			engine.apply(queryWrapper, table.getName());
		}
	}
}
