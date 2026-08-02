package com.mugsun.boot.system.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import com.mugsun.boot.system.entity.SysLoginLog;
import com.mugsun.boot.system.mapper.SysLoginLogMapper;
import com.mugsun.core.tool.api.R;
import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.query.QueryWrapper;
import com.mugsun.boot.tenant.TenantContext;
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
	@cn.dev33.satoken.annotation.SaCheckPermission("sys:login-log:list")
	public R<Page<SysLoginLog>> page(@RequestParam(defaultValue = "1") long pageNum,
									 @RequestParam(defaultValue = "10") long pageSize,
									 @RequestParam(required = false) Integer status) {
		QueryWrapper query = QueryWrapper.create().orderBy("id", false);
		if (status != null) {
			query.and("status = ?", status);
		}
		// 平台超管看全平台留痕（ignore）；其余租户仅见本租户登录日志（tenant_id 列自动过滤）
		Page<SysLoginLog> page = TenantContext.isPlatformSuperAdmin()
			? TenantContext.ignore(() -> loginLogMapper.paginate(pageNum, Math.min(pageSize, 500), query))
			: loginLogMapper.paginate(pageNum, Math.min(pageSize, 500), query);
		return R.data(page);
	}
}
