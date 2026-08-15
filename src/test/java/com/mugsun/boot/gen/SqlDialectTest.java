package com.mugsun.boot.gen;

import com.mybatisflex.core.dialect.DbType;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 方言归族：信创 PG 系（金仓/openGauss）→ POSTGRES；达梦 → ORACLE。
 */
class SqlDialectTest {

	@Test
	void kingbaseAndOpenGaussMapToPostgresFamily() {
		assertThat(SqlDialect.of(DbType.KINGBASE_ES)).isEqualTo(SqlDialect.POSTGRES);
		assertThat(SqlDialect.of(DbType.OPENGAUSS)).isEqualTo(SqlDialect.POSTGRES);
		assertThat(SqlDialect.of(DbType.POSTGRE_SQL)).isEqualTo(SqlDialect.POSTGRES);
	}

	@Test
	void damengMapsToOracleFamily() {
		assertThat(SqlDialect.of(DbType.DM)).isEqualTo(SqlDialect.ORACLE);
		assertThat(SqlDialect.of(DbType.DM).bigintType()).isEqualTo("NUMBER(19)");
		assertThat(SqlDialect.of(DbType.DM).columnType("String", true)).isEqualTo("CLOB");
	}

	@Test
	void nullFallsBackToPostgres() {
		assertThat(SqlDialect.of(null)).isEqualTo(SqlDialect.POSTGRES);
	}
}
