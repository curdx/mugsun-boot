package com.mugsun.boot.gen;

import com.mybatisflex.core.dialect.DbType;

/**
 * 建表 DDL 方言族：将 Flex {@link DbType} 归并为三大兼容族，屏蔽国产库/主流库差异。
 * <ul><li>{@link #POSTGRES}——PG 系：PostgreSQL / 金仓 KingbaseES / openGauss / GaussDB / 瀚高 HighGo / Greenplum / 海量 VastBase 等（PG 线协议与语法兼容）；</li>
 * <li>{@link #ORACLE}——Oracle 系：Oracle / 达梦 DM / OceanBase(Oracle 模式)（NUMBER/VARCHAR2/CLOB，未加引号标识符按大写存储）；</li>
 * <li>{@link #MYSQL}——MySQL 系：MySQL / MariaDB / OceanBase(MySQL 模式) / GoldenDB。</li></ul>
 * 目标库由 {@code DbTypeUtil.getDbType(dataSource)} 探测后自动归族，建表类型按族出型，去除单一 PG 硬编码。
 */
public enum SqlDialect {

	POSTGRES("BIGINT", "INT4", "TIMESTAMP", "NUMERIC(18,4)", "FLOAT8", "TEXT", "VARCHAR(255)"),
	ORACLE("NUMBER(19)", "NUMBER(10)", "TIMESTAMP", "NUMBER(18,4)", "BINARY_DOUBLE", "CLOB", "VARCHAR2(255)"),
	MYSQL("BIGINT", "INT", "DATETIME", "DECIMAL(18,4)", "DOUBLE", "TEXT", "VARCHAR(255)");

	private final String bigintType;
	private final String intType;
	private final String timestampType;
	private final String decimalType;
	private final String doubleType;
	private final String textType;
	private final String varcharType;

	SqlDialect(String bigintType, String intType, String timestampType, String decimalType,
			String doubleType, String textType, String varcharType) {
		this.bigintType = bigintType;
		this.intType = intType;
		this.timestampType = timestampType;
		this.decimalType = decimalType;
		this.doubleType = doubleType;
		this.textType = textType;
		this.varcharType = varcharType;
	}

	/** Flex 探测的 DbType 归族；未识别库回退平台主库族 PG，保证不因新库型中断建表。 */
	public static SqlDialect of(DbType dbType) {
		if (dbType == null) {
			return POSTGRES;
		}
		return switch (dbType) {
			case ORACLE, ORACLE_12C, DM, OCEAN_BASE_ORACLE -> ORACLE;
			case MYSQL, MARIADB, OCEAN_BASE, GOLDENDB -> MYSQL;
			// PG 系(含金仓 KINGBASE_ES/openGauss/GaussDB/瀚高/Greenplum/海量 等)与未知库统一按 PG 出型
			default -> POSTGRES;
		};
	}

	/** 雪花主键/Long 列类型 */
	public String bigintType() {
		return bigintType;
	}

	/** 时间戳列类型（审计列 create_time/update_time 用） */
	public String timestampType() {
		return timestampType;
	}

	/** 整型列类型（is_deleted 逻辑删除列用） */
	public String intType() {
		return intType;
	}

	/** 业务列 Java 类型 → 目标库 SQL 类型（largeText：长文本控件落大字段） */
	public String columnType(String javaType, boolean largeText) {
		String jt = javaType == null ? "String" : javaType;
		return switch (jt) {
			case "Integer", "Short", "Boolean" -> intType;
			case "Long" -> bigintType;
			case "BigDecimal" -> decimalType;
			case "Double", "Float" -> doubleType;
			case "LocalDateTime", "Date", "Timestamp" -> timestampType;
			case "LocalDate" -> "DATE";
			default -> largeText ? textType : varcharType;
		};
	}
}
