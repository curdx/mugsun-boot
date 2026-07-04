package com.mugsun.boot.system.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
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
	public R<List<SysParam>> list() {
		return R.data(paramMapper.selectListByQuery(QueryWrapper.create().orderBy("id", false)));
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

	@PostMapping("/remove")
	public R<Void> remove(@RequestParam Long id) {
		SysParam param = paramMapper.selectOneById(id);
		paramMapper.deleteById(id);
		if (param != null) {
			paramService.evict(param.getParamKey());
		}
		return R.success("删除成功");
	}
}
