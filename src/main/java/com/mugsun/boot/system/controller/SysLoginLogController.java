package com.mugsun.boot.system.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import com.mugsun.boot.system.entity.SysLoginLog;
import com.mugsun.boot.system.mapper.SysLoginLogMapper;
import com.mugsun.core.tool.api.R;
import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.core.tenant.TenantManager;
import org.springframework.web.bind.annotation.*;

/**
 * 登录日志查询
 */
@RestController
@RequestMapping("/system/login-log")
@SaCheckLogin
public class SysLoginLogController {

	private final SysLoginLogMapper loginLogMapper;

	public SysLoginLogController(SysLoginLogMapper loginLogMapper) {
		this.loginLogMapper = loginLogMapper;
	}

	@GetMapping("/page")
	public R<Page<SysLoginLog>> page(@RequestParam(defaultValue = "1") long pageNum,
									 @RequestParam(defaultValue = "10") long pageSize,
									 @RequestParam(required = false) Integer status) {
		QueryWrapper query = QueryWrapper.create().orderBy("id", false);
		if (status != null) {
			query.and("status = ?", status);
		}
		// 登录日志为平台级留痕，不做租户过滤
		Page<SysLoginLog> page = TenantManager.withoutTenantCondition(() ->
			loginLogMapper.paginate(pageNum, pageSize, query));
		return R.data(page);
	}
}
