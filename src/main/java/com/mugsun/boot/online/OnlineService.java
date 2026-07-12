package com.mugsun.boot.online;

import cn.hutool.core.util.IdUtil;
import com.mugsun.boot.gen.entity.GenColumn;
import com.mugsun.boot.gen.entity.GenTable;
import com.mugsun.boot.gen.mapper.GenColumnMapper;
import com.mugsun.boot.gen.mapper.GenTableMapper;
import com.mugsun.boot.tenant.TenantContext;
import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.core.row.Db;
import com.mybatisflex.core.row.Row;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 在线（低代码）运行时引擎：复用 gen_table/gen_column 统一元数据，经 Flex Db + Row **无实体**对真实业务物理表
 * 做泛化增删改查（结构化列，非 JSON blob）。改元数据配置即时生效、零发布；数据可按字段查询。
 * <p>无实体路径不走 Flex 逻辑删除/租户插件，故此处按元数据手工附加 is_deleted=0 / tenant_id / 审计。
 */
@Service
public class OnlineService {

	private final GenTableMapper tableMapper;
	private final GenColumnMapper columnMapper;

	public OnlineService(GenTableMapper tableMapper, GenColumnMapper columnMapper) {
		this.tableMapper = tableMapper;
		this.columnMapper = columnMapper;
	}

	/** 表单元数据：表 + 字段配置，供前端运行时渲染 */
	public Map<String, Object> meta(Long tableId) {
		return Map.of("table", table(tableId), "columns", columns(tableId));
	}

	/** 运行时分页查询（is_deleted=0 + 租户 + is_query 字段动态条件） */
	public Page<Row> page(Long tableId, long pageNum, long pageSize, Map<String, String> query) {
		GenTable t = table(tableId);
		List<GenColumn> cols = columns(tableId);
		Set<String> names = columnNames(cols);
		String tableName = t.getTableName();
		QueryWrapper qw = QueryWrapper.create().from(tableName).orderBy("id", false);
		applyScope(qw, names);
		for (GenColumn c : cols) {
			if (isOne(c.getIsQuery()) && query != null) {
				String v = query.get(c.getColumnName());
				if (v != null && !v.isBlank()) {
					if ("LIKE".equalsIgnoreCase(c.getQueryType())) {
						qw.and(c.getColumnName() + " LIKE ?", "%" + v + "%");
					} else {
						qw.and(c.getColumnName() + " = ?", coerce(v, c.getJavaType()));
					}
				}
			}
		}
		return Db.paginate(tableName, pageNum, pageSize, qw);
	}

	/** 运行时详情（与 page 同口径附加租户 + 逻辑删除范围，防跨租户/越删读） */
	public Row detail(Long tableId, Object id) {
		GenTable t = table(tableId);
		Set<String> names = columnNames(columns(tableId));
		String tableName = t.getTableName();
		QueryWrapper qw = QueryWrapper.create().from(tableName).where("id = ?", Long.valueOf(id.toString()));
		applyScope(qw, names);
		List<Row> rows = Db.selectListByQuery(tableName, qw);
		return rows.isEmpty() ? null : rows.get(0);
	}

	/** 运行时保存（新增/编辑）：仅可编辑列入库，落真实物理表结构化列 */
	public void save(Long tableId, Map<String, Object> data) {
		GenTable t = table(tableId);
		List<GenColumn> cols = columns(tableId);
		Set<String> names = columnNames(cols);
		String tableName = t.getTableName();
		Object id = data.get("id");
		boolean isNew = id == null || id.toString().isBlank();
		Row row = isNew ? new Row() : Row.ofKey("id", Long.valueOf(id.toString()));
		for (GenColumn c : cols) {
			if (isOne(c.getIsEdit()) && data.containsKey(c.getColumnName())) {
				row.set(c.getColumnName(), data.get(c.getColumnName()));
			}
		}
		if (isNew) {
			row.set("id", IdUtil.getSnowflakeNextId());
			if (names.contains("create_time")) {
				row.set("create_time", LocalDateTime.now());
			}
			if (names.contains("is_deleted")) {
				row.set("is_deleted", 0);
			}
			if (names.contains("tenant_id") && TenantContext.current() != null) {
				row.set("tenant_id", TenantContext.current());
			}
			Db.insert(tableName, row);
		} else {
			// 编辑前校验目标行在当前租户 / 未删除范围内，防跨租户改
			QueryWrapper scope = QueryWrapper.create().from(tableName).where("id = ?", Long.valueOf(id.toString()));
			applyScope(scope, names);
			if (Db.selectListByQuery(tableName, scope).isEmpty()) {
				throw new IllegalArgumentException("记录不存在或无权限：" + id);
			}
			if (names.contains("update_time")) {
				row.set("update_time", LocalDateTime.now());
			}
			Db.updateById(tableName, row);
		}
	}

	/** 运行时删除：有 is_deleted 列走逻辑删除，否则物理删除（均附加租户范围，防跨租户删） */
	public void remove(Long tableId, List<Long> ids) {
		if (ids == null || ids.isEmpty()) {
			return;
		}
		GenTable t = table(tableId);
		Set<String> names = columnNames(columns(tableId));
		String tableName = t.getTableName();
		if (!names.contains("is_deleted")) {
			Db.deleteBatchByIds(tableName, "id", ids);
			return;
		}
		String placeholders = ids.stream().map(i -> "?").collect(Collectors.joining(","));
		String sql = "UPDATE " + tableName + " SET is_deleted = 1 WHERE id IN (" + placeholders + ")";
		List<Object> params = new ArrayList<>(ids);
		if (names.contains("tenant_id") && TenantContext.current() != null) {
			sql += " AND tenant_id = ?";
			params.add(TenantContext.current());
		}
		Db.updateBySql(sql, params.toArray());
	}

	/** 无实体路径手工附加租户 + 逻辑删除范围条件，与 page/detail/save/remove 统一口径 */
	private void applyScope(QueryWrapper qw, Set<String> names) {
		if (names.contains("is_deleted")) {
			qw.and("is_deleted = 0");
		}
		if (names.contains("tenant_id") && TenantContext.current() != null) {
			qw.and("tenant_id = ?", TenantContext.current());
		}
	}

	private GenTable table(Long tableId) {
		GenTable t = tableMapper.selectOneById(tableId);
		if (t == null) {
			throw new IllegalArgumentException("在线表单不存在：" + tableId);
		}
		return t;
	}

	private List<GenColumn> columns(Long tableId) {
		return columnMapper.selectListByQuery(
			QueryWrapper.create().eq("table_id", tableId).orderBy("sort", true));
	}

	private Set<String> columnNames(List<GenColumn> cols) {
		Set<String> s = new HashSet<>();
		for (GenColumn c : cols) {
			s.add(c.getColumnName());
		}
		return s;
	}

	private boolean isOne(Integer v) {
		return v != null && v == 1;
	}

	/** 查询值按字段 Java 类型转型：数值列需绑定数值（String 绑定数值列在部分数据库会类型报错） */
	private Object coerce(String v, String javaType) {
		if (javaType == null) {
			return v;
		}
		try {
			return switch (javaType) {
				case "Integer", "Long", "Short" -> Long.valueOf(v.trim());
				case "BigDecimal" -> new BigDecimal(v.trim());
				case "Double", "Float" -> Double.valueOf(v.trim());
				default -> v;
			};
		} catch (NumberFormatException e) {
			return v;
		}
	}
}
