package com.mugsun.boot.gen;

import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.hutool.core.util.IdUtil;
import com.mugsun.boot.gen.entity.GenConfig;
import com.mugsun.boot.gen.mapper.GenConfigMapper;
import com.mugsun.core.tool.api.R;
import com.mugsun.core.tool.exception.ServiceException;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.core.row.Db;
import org.springframework.core.env.Environment;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 在线代码生成：数据源信息 / 表与列 / 生成预览 / 表配置持久化
 */
@RestController
@RequestMapping("/system/gen")
@SaCheckLogin
public class GenController {

	private final GenService genService;
	private final Environment env;
	private final GenConfigMapper configMapper;

	public GenController(GenService genService, Environment env, GenConfigMapper configMapper) {
		this.genService = genService;
		this.env = env;
		this.configMapper = configMapper;
	}

	/** 当前数据源信息（只读，密码不下发） */
	@GetMapping("/datasource")
	public R<Map<String, Object>> datasource() {
		Map<String, Object> ds = new LinkedHashMap<>();
		ds.put("name", "主数据源");
		ds.put("url", env.getProperty("spring.datasource.url"));
		ds.put("username", env.getProperty("spring.datasource.username"));
		ds.put("driver", env.getProperty("spring.datasource.driver-class-name"));
		return R.data(ds);
	}

	@GetMapping("/tables")
	public R<List<Map<String, Object>>> tables() {
		return R.data(genService.tables());
	}

	@GetMapping("/columns")
	public R<Map<String, Object>> columns(@RequestParam String table) {
		return R.data(genService.columns(table));
	}

	@PostMapping("/preview")
	public R<Map<String, String>> preview(@RequestBody GenParam param) {
		return R.data(genService.preview(param.tableName(), param.basePackage(), param.tablePrefix()));
	}

	/** 取某表已存生成配置，无则返回 null（前端用默认） */
	@GetMapping("/config")
	public R<GenConfig> config(@RequestParam String table) {
		return R.data(configMapper.selectOneByQuery(QueryWrapper.create().eq("table_name", table)));
	}

	/** 保存生成配置：按表名原子 upsert */
	@PostMapping("/config/save")
	public R<Void> saveConfig(@RequestBody GenConfig config) {
		if (config.getTableName() == null || config.getTableName().isBlank()) {
			throw new ServiceException("表名不能为空");
		}
		Db.updateBySql(
			"insert into gen_config (id, table_name, base_package, table_prefix, create_time, update_time, is_deleted) "
				+ "values (?, ?, ?, ?, now(), now(), 0) "
				+ "on conflict (table_name) where is_deleted = 0 "
				+ "do update set base_package = excluded.base_package, table_prefix = excluded.table_prefix, update_time = now()",
			IdUtil.getSnowflakeNextId(), config.getTableName(), config.getBasePackage(), config.getTablePrefix());
		return R.success("配置已保存");
	}

	/** 生成参数：表名、基础包、表前缀 */
	public record GenParam(String tableName, String basePackage, String tablePrefix) {
	}
}
