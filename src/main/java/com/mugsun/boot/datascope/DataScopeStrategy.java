package com.mugsun.boot.datascope;

import com.mybatisflex.core.query.QueryWrapper;

import java.util.function.Consumer;

/**
 * 数据权限策略：一种数据范围对应一个策略，构建应用到 QueryWrapper 的过滤条件。
 * 返回 {@code null} 表示不限制（全部数据）。
 */
public interface DataScopeStrategy {

	/** 适配的数据范围值（1全部/2本部门/3本部门及子/4仅本人/5自定义部门） */
	int scope();

	/**
	 * 构建过滤条件应用器。
	 *
	 * @param ctx        当前用户数据权限上下文
	 * @param deptColumn 部门归属列
	 * @param userColumn 本人归属列
	 * @return 应用到 QueryWrapper 的条件；null 表示不过滤
	 */
	Consumer<QueryWrapper> build(DataScopeContext.Scope ctx, String deptColumn, String userColumn);
}
