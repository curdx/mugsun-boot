package com.mugsun.boot.system.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import com.mugsun.boot.system.entity.SysTenant;
import com.mugsun.boot.system.mapper.SysTenantMapper;
import com.mugsun.boot.system.service.TenantService;
import com.mugsun.core.tool.api.R;
import com.mybatisflex.core.query.QueryWrapper;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 租户管理（仅超管租户可操作）
 */
@RestController
@RequestMapping("/system/tenant")
@SaCheckLogin
public class SysTenantController {

	private final SysTenantMapper tenantMapper;
	private final TenantService tenantService;

	public SysTenantController(SysTenantMapper tenantMapper, TenantService tenantService) {
		this.tenantMapper = tenantMapper;
		this.tenantService = tenantService;
	}

	@GetMapping("/list")
	public R<List<SysTenant>> list() {
		return R.data(tenantMapper.selectListByQuery(QueryWrapper.create().orderBy("id", false)));
	}

	@GetMapping("/detail")
	public R<SysTenant> detail(@RequestParam Long id) {
		return R.data(tenantMapper.selectOneById(id));
	}

	/** 创建租户并初始化默认数据，返回租户编号 */
	@PostMapping("/create")
	public R<String> create(@RequestBody SysTenant tenant) {
		return R.data(tenantService.create(tenant));
	}

	/** 更新租户信息（含状态/期限/配额/套餐分配），不重新初始化默认数据 */
	@PostMapping("/update")
	public R<Void> update(@RequestBody SysTenant tenant) {
		if (tenant.getId() == null || tenantMapper.selectOneById(tenant.getId()) == null) {
			return R.fail("租户不存在");
		}
		tenantService.update(tenant);
		return R.success("更新成功");
	}

	@PostMapping("/remove")
	public R<Void> remove(@RequestBody List<Long> ids) {
		tenantService.remove(ids);
		return R.success("删除成功");
	}
}
