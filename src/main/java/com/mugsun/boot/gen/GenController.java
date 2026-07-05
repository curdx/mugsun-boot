package com.mugsun.boot.gen;

import cn.dev33.satoken.annotation.SaCheckLogin;
import com.mugsun.core.tool.api.R;
import org.springframework.core.env.Environment;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 在线代码生成：数据源信息 / 表与列 / 生成预览
 */
@RestController
@RequestMapping("/system/gen")
@SaCheckLogin
public class GenController {

	private final GenService genService;
	private final Environment env;

	public GenController(GenService genService, Environment env) {
		this.genService = genService;
		this.env = env;
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

	/** 生成参数：表名、基础包、表前缀 */
	public record GenParam(String tableName, String basePackage, String tablePrefix) {
	}
}
