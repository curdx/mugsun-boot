package com.mugsun.boot.datascope;

import java.util.Map;

/**
 * 受控表注册表：声明哪些表参与数据权限过滤及其部门/本人归属列。
 * {@link MugsunDataScopeDialect} 仅对注册表内的表注入条件，字典/系统等表不受影响。
 */
public final class DataScopeTables {

	/** 受控表的部门列与本人列 */
	public record Columns(String deptColumn, String userColumn) {
	}

	/** 表名（小写）→ 归属列。业务表按需在此登记。 */
	private static final Map<String, Columns> TABLES = Map.of(
		"sys_user", new Columns("dept_id", "id")
	);

	private DataScopeTables() {
	}

	/** 取受控表列；{@code null} 表示该表不受控（不注入） */
	public static Columns of(String tableName) {
		return tableName == null ? null : TABLES.get(tableName.toLowerCase());
	}
}
