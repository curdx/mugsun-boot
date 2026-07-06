package com.mugsun.boot.tablecolumn;

import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.util.IdUtil;
import com.mugsun.boot.tablecolumn.entity.SysTableColumn;
import com.mugsun.boot.tablecolumn.mapper.SysTableColumnMapper;
import com.mugsun.core.tool.api.R;
import com.mugsun.core.tool.exception.ServiceException;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.core.row.Db;
import org.springframework.web.bind.annotation.*;

/**
 * 表格自定义列持久化：按当前用户 + 表格标识存取列配置（顺序/显隐/宽），支持恢复默认。
 */
@RestController
@RequestMapping("/system/table-column")
@SaCheckLogin
public class TableColumnController {

	private final SysTableColumnMapper columnMapper;

	public TableColumnController(SysTableColumnMapper columnMapper) {
		this.columnMapper = columnMapper;
	}

	/** 取当前用户某表的列配置，无则返回 null（前端用内置默认） */
	@GetMapping("/get")
	public R<SysTableColumn> get(@RequestParam String tableKey) {
		return R.data(columnMapper.selectOneByQuery(query(tableKey)));
	}

	/** 保存（原子 upsert）：按 user_id + table_key 归并，存在则更新否则新增 */
	@PostMapping("/save")
	public R<Void> save(@RequestBody SysTableColumn body) {
		String tableKey = body.getTableKey();
		if (tableKey == null || tableKey.isBlank()) {
			throw new ServiceException("表格标识不能为空");
		}
		// 单条 upsert 原子化：并发首配同抢时靠 (user_id, table_key) 部分唯一键归并，杜绝抢插冲突
		Db.updateBySql(
			"insert into sys_table_column (id, user_id, table_key, config_json, create_time, update_time, is_deleted) "
				+ "values (?, ?, ?, ?, now(), now(), 0) "
				+ "on conflict (user_id, table_key) where is_deleted = 0 "
				+ "do update set config_json = excluded.config_json, update_time = now()",
			IdUtil.getSnowflakeNextId(), StpUtil.getLoginIdAsLong(), tableKey, body.getConfigJson());
		return R.success("保存成功");
	}

	/** 恢复默认：删除当前用户该表的配置行 */
	@PostMapping("/reset/{tableKey}")
	public R<Void> reset(@PathVariable String tableKey) {
		columnMapper.deleteByQuery(query(tableKey));
		return R.success("已恢复默认");
	}

	private QueryWrapper query(String tableKey) {
		return QueryWrapper.create()
				.eq("user_id", StpUtil.getLoginIdAsLong())
				.eq("table_key", tableKey);
	}
}
