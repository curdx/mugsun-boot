package com.mugsun.boot.gen;

import com.mugsun.boot.gen.entity.GenColumn;
import com.mugsun.boot.gen.entity.GenTable;
import com.mugsun.boot.gen.mapper.GenColumnMapper;
import com.mugsun.boot.gen.mapper.GenTableMapper;
import com.mugsun.core.tool.exception.ServiceException;
import com.mybatisflex.core.query.QueryWrapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 动态建表引擎：以 gen_table/gen_column 元数据为源，向真实数据库生成并执行 DDL。
 * <ul><li>正向建表：config → CREATE TABLE（雪花主键 + 审计列，平台规约）</li>
 * <li>增量同步：diff 物理表结构 → ADD COLUMN（新增字段）/ RENAME COLUMN（改名保数据），不 DROP</li>
 * <li>强制重建：DROP + CREATE（毁数据，需显式确认，且禁作用于系统表）</li></ul>
 * 安全：标识符白名单校验、仅作用于 gen 托管表、系统表前缀保护、DDL 事务化执行。
 */
@Service
public class DdlService {

	/** 受保护的系统表前缀（不可重建/删除，且新建不得占用） */
	private static final Set<String> PROTECTED = Set.of("sys_", "gen_", "flyway_", "flow_", "qrtz_", "blade_", "act_");
	/** 审计列由引擎统一维护，建表时显式补齐，不随业务列重复渲染 */
	private static final Set<String> AUDIT = Set.of("id", "create_time", "update_time", "is_deleted");

	private final DataSource dataSource;
	private final GenTableMapper tableMapper;
	private final GenColumnMapper columnMapper;

	public DdlService(DataSource dataSource, GenTableMapper tableMapper, GenColumnMapper columnMapper) {
		this.dataSource = dataSource;
		this.tableMapper = tableMapper;
		this.columnMapper = columnMapper;
	}

	/** 预览 DDL（不执行）：物理表不存在→建表；存在→同步（force 走 DROP+CREATE，否则增量 ADD/RENAME） */
	public List<String> preview(Long tableId, boolean force) {
		GenTable t = table(tableId);
		boolean pg = isPg();
		if (!force && !physicalExists(validName(t.getTableName()))) {
			return List.of(buildCreate(t, columns(tableId), pg));
		}
		return buildSync(t, columns(tableId), force, pg);
	}

	/** 校验表名合法且非系统保护表（确认建模前置校验，避免落入无法建表的无效/越权配置） */
	public void validateTableName(String tableName) {
		guardNotProtected(validName(tableName));
	}

	/** 正向建表：物理表不存在时按元数据 CREATE */
	@Transactional(rollbackFor = Exception.class)
	public void createTable(Long tableId) {
		GenTable t = table(tableId);
		String tn = validName(t.getTableName());
		guardNotProtected(tn);
		if (physicalExists(tn)) {
			throw new ServiceException("物理表已存在，请改用同步：" + tn);
		}
		execute(List.of(buildCreate(t, columns(tableId), isPg())));
	}

	/** 增量/强制同步：执行 DDL，并在改名完成后清空 column_name_old */
	@Transactional(rollbackFor = Exception.class)
	public void syncTable(Long tableId, boolean force) {
		GenTable t = table(tableId);
		String tn = validName(t.getTableName());
		guardNotProtected(tn);
		List<GenColumn> cols = columns(tableId);
		boolean pg = isPg();
		if (force) {
			execute(buildSync(t, cols, true, pg));
		} else {
			if (!physicalExists(tn)) {
				throw new ServiceException("物理表不存在，请先建表：" + tn);
			}
			List<String> stmts = buildSync(t, cols, false, pg);
			if (stmts.isEmpty()) {
				throw new ServiceException("无结构变更");
			}
			execute(stmts);
			// 改名已落库，清除追踪标记避免二次 RENAME
			for (GenColumn c : cols) {
				if (c.getColumnNameOld() != null && !c.getColumnNameOld().isBlank()) {
					c.setColumnNameOld(null);
					columnMapper.update(c, false);
				}
			}
		}
	}

	// ==================== DDL 构建 ====================

	private String buildCreate(GenTable t, List<GenColumn> cols, boolean pg) {
		String tn = validName(t.getTableName());
		List<String> lines = new ArrayList<>();
		lines.add("id " + (pg ? "BIGINT" : "BIGINT") + " PRIMARY KEY");
		for (GenColumn c : cols) {
			String cn = c.getColumnName();
			if (cn == null || AUDIT.contains(cn)) {
				continue;
			}
			validName(cn);
			String nn = isOne(c.getIsRequired()) ? " NOT NULL" : "";
			lines.add(cn + " " + sqlType(c, pg) + nn);
		}
		lines.add("create_time " + tsType(pg));
		lines.add("update_time " + tsType(pg));
		lines.add("is_deleted " + intType(pg) + " NOT NULL DEFAULT 0");
		return "CREATE TABLE " + tn + " (\n\t" + String.join(",\n\t", lines) + "\n)";
	}

	private List<String> buildSync(GenTable t, List<GenColumn> cols, boolean force, boolean pg) {
		String tn = validName(t.getTableName());
		if (force) {
			return List.of("DROP TABLE IF EXISTS " + tn, buildCreate(t, cols, pg));
		}
		Map<String, String> physical = physicalColumns(tn);
		Set<String> physSet = physical.keySet();
		List<String> stmts = new ArrayList<>();
		for (GenColumn c : cols) {
			String cn = c.getColumnName();
			if (cn == null || "id".equals(cn)) {
				continue;
			}
			validName(cn);
			String old = c.getColumnNameOld();
			boolean hasNew = physSet.contains(cn.toLowerCase());
			if (old != null && !old.isBlank() && physSet.contains(old.toLowerCase()) && !hasNew) {
				stmts.add("ALTER TABLE " + tn + " RENAME COLUMN " + validName(old) + " TO " + cn);
			} else if (!hasNew) {
				// 新增列须可空（存量行无值），is_deleted 例外给默认值
				String def = "is_deleted".equals(cn) ? " NOT NULL DEFAULT 0" : "";
				stmts.add("ALTER TABLE " + tn + " ADD COLUMN " + cn + " " + sqlType(c, pg) + def);
			}
		}
		return stmts;
	}

	/** Java 类型 → 方言 SQL 类型（长文本控件落 text） */
	private String sqlType(GenColumn c, boolean pg) {
		String jt = c.getJavaType() == null ? "String" : c.getJavaType();
		return switch (jt) {
			case "Integer", "Short", "Boolean" -> intType(pg);
			case "Long" -> "BIGINT";
			case "BigDecimal" -> pg ? "NUMERIC(18,4)" : "DECIMAL(18,4)";
			case "Double", "Float" -> pg ? "FLOAT8" : "DOUBLE";
			case "LocalDateTime", "Date", "Timestamp" -> tsType(pg);
			case "LocalDate" -> "DATE";
			default -> "textarea".equals(c.getHtmlType()) ? "TEXT" : "VARCHAR(255)";
		};
	}

	private String intType(boolean pg) {
		return pg ? "INT4" : "INT";
	}

	private String tsType(boolean pg) {
		return pg ? "TIMESTAMP" : "DATETIME";
	}

	// ==================== 执行 / 内省 ====================

	/** DDL 事务化执行（PG DDL 事务安全，失败整体回滚） */
	private void execute(List<String> stmts) {
		try (Connection c = dataSource.getConnection()) {
			boolean auto = c.getAutoCommit();
			c.setAutoCommit(false);
			try (Statement st = c.createStatement()) {
				for (String s : stmts) {
					st.execute(s);
				}
				c.commit();
			} catch (SQLException e) {
				c.rollback();
				throw new ServiceException("DDL 执行失败：" + e.getMessage());
			} finally {
				c.setAutoCommit(auto);
			}
		} catch (SQLException e) {
			throw new ServiceException("获取连接失败：" + e.getMessage());
		}
	}

	private boolean physicalExists(String tn) {
		return !physicalColumns(tn).isEmpty();
	}

	/** 物理表现有列（JDBC 元数据，跨方言）：列名（小写）→ 类型名 */
	private Map<String, String> physicalColumns(String tn) {
		Map<String, String> m = new LinkedHashMap<>();
		try (Connection c = dataSource.getConnection()) {
			DatabaseMetaData md = c.getMetaData();
			try (ResultSet rs = md.getColumns(c.getCatalog(), c.getSchema(), tn.toLowerCase(), null)) {
				while (rs.next()) {
					m.put(rs.getString("COLUMN_NAME").toLowerCase(), rs.getString("TYPE_NAME"));
				}
			}
		} catch (SQLException e) {
			throw new ServiceException("读取表结构失败：" + e.getMessage());
		}
		return m;
	}

	private boolean isPg() {
		try (Connection c = dataSource.getConnection()) {
			return c.getMetaData().getDatabaseProductName().toLowerCase().contains("postgre");
		} catch (SQLException e) {
			return true;
		}
	}

	// ==================== 安全 / 取数 ====================

	private String validName(String name) {
		if (!GenNaming.isIdentifier(name)) {
			throw new ServiceException("非法标识符：" + name);
		}
		return name;
	}

	private void guardNotProtected(String tn) {
		String low = tn.toLowerCase();
		for (String p : PROTECTED) {
			if (low.startsWith(p)) {
				throw new ServiceException("受保护的系统表不可建/重建：" + tn);
			}
		}
	}

	private GenTable table(Long tableId) {
		GenTable t = tableMapper.selectOneById(tableId);
		if (t == null) {
			throw new ServiceException("表配置不存在：" + tableId);
		}
		return t;
	}

	private List<GenColumn> columns(Long tableId) {
		return columnMapper.selectListByQuery(
			QueryWrapper.create().eq("table_id", tableId).orderBy("sort", true));
	}

	private boolean isOne(Integer v) {
		return v != null && v == 1;
	}
}
