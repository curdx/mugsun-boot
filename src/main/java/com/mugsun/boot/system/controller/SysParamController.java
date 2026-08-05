package com.mugsun.boot.system.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.annotation.SaCheckPermission;
import com.mugsun.boot.system.entity.SysParam;
import com.mugsun.boot.system.mapper.SysParamMapper;
import com.mugsun.boot.system.service.ParamService;
import com.mugsun.core.tool.api.R;
import com.mybatisflex.core.query.QueryWrapper;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 系统参数管理
 */
@RestController
@RequestMapping("/system/param")
@SaCheckLogin
public class SysParamController {

	private final SysParamMapper paramMapper;
	private final ParamService paramService;

	public SysParamController(SysParamMapper paramMapper, ParamService paramService) {
		this.paramMapper = paramMapper;
		this.paramService = paramService;
	}

	@GetMapping("/list")
	public R<List<SysParam>> list(@RequestParam(required = false) String paramName,
								  @RequestParam(required = false) String paramKey) {
		QueryWrapper query = QueryWrapper.create().orderBy("id", false);
		// 查询条件（值走参数化绑定，LIKE 前后模糊）
		if (paramName != null && !paramName.isBlank()) {
			query.like("param_name", paramName.trim());
		}
		if (paramKey != null && !paramKey.isBlank()) {
			query.like("param_key", paramKey.trim());
		}
		return R.data(paramMapper.selectListByQuery(query));
	}

	/** 按键取参数值（走缓存） */
	@GetMapping("/value")
	public R<String> value(@RequestParam String paramKey) {
		return R.data(paramService.getValue(paramKey));
	}

	@GetMapping("/detail")
	public R<SysParam> detail(@RequestParam Long id) {
		return R.data(paramMapper.selectOneById(id));
	}

	@SaCheckPermission("sys:param:save")
	@PostMapping("/submit")
	public R<Void> submit(@RequestBody SysParam param) {
		if (param.getId() == null) {
			paramMapper.insertSelective(param);
		} else {
			paramMapper.update(param);
		}
		paramService.evict(param.getParamKey());
		return R.success("操作成功");
	}

	@SaCheckPermission("sys:param:remove")
	@PostMapping("/remove")
	public R<Void> remove(@RequestBody List<Long> ids) {
		ids.forEach(id -> {
			SysParam param = paramMapper.selectOneById(id);
			paramMapper.deleteById(id);
			if (param != null) {
				paramService.evict(param.getParamKey());
			}
		});
		return R.success("删除成功");
	}
}
