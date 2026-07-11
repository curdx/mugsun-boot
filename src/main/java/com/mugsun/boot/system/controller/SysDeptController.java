package com.mugsun.boot.system.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.annotation.SaCheckPermission;
import com.mugsun.boot.system.entity.SysDept;
import com.mugsun.boot.system.mapper.SysDeptMapper;
import com.mugsun.core.tool.api.R;
import com.mugsun.core.tool.tree.TreeUtil;
import com.mybatisflex.core.query.QueryWrapper;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 部门管理
 */
@RestController
@RequestMapping("/system/dept")
@SaCheckLogin
public class SysDeptController {

	private final SysDeptMapper deptMapper;

	public SysDeptController(SysDeptMapper deptMapper) {
		this.deptMapper = deptMapper;
	}

	@GetMapping("/tree")
	public R<List<SysDept>> tree() {
		List<SysDept> all = deptMapper.selectListByQuery(QueryWrapper.create().orderBy("sort", true));
		return R.data(TreeUtil.build(all, 0L));
	}

	/** 部门下拉选项（value/label 契约，供上级部门选择等场景） */
	@GetMapping("/select")
	public R<List<Map<String, Object>>> select() {
		List<Map<String, Object>> options = deptMapper
			.selectListByQuery(QueryWrapper.create().orderBy("sort", true))
			.stream()
			.map(dept -> {
				Map<String, Object> option = new HashMap<>();
				option.put("value", dept.getId());
				option.put("label", dept.getDeptName());
				return option;
			})
			.toList();
		return R.data(options);
	}

	@GetMapping("/detail")
	public R<SysDept> detail(@RequestParam Long id) {
		return R.data(deptMapper.selectOneById(id));
	}

	@SaCheckPermission("sys:dept:save")
	@PostMapping("/submit")
	public R<Void> submit(@RequestBody SysDept dept) {
		if (dept.getId() == null) {
			deptMapper.insert(dept);
		} else {
			deptMapper.update(dept);
		}
		return R.success("操作成功");
	}

	@SaCheckPermission("sys:dept:remove")
	@PostMapping("/remove")
	public R<Void> remove(@RequestBody List<Long> ids) {
		deptMapper.deleteBatchByIds(ids);
		return R.success("删除成功");
	}
}
