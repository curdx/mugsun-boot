package com.mugsun.boot.system.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import com.mugsun.boot.system.entity.SysDept;
import com.mugsun.boot.system.mapper.SysDeptMapper;
import com.mugsun.core.tool.api.R;
import com.mugsun.core.tool.tree.TreeUtil;
import com.mybatisflex.core.query.QueryWrapper;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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

	@GetMapping("/detail")
	public R<SysDept> detail(@RequestParam Long id) {
		return R.data(deptMapper.selectOneById(id));
	}

	@PostMapping("/submit")
	public R<Void> submit(@RequestBody SysDept dept) {
		if (dept.getId() == null) {
			deptMapper.insert(dept);
		} else {
			deptMapper.update(dept);
		}
		return R.success("操作成功");
	}

	@PostMapping("/remove")
	public R<Void> remove(@RequestParam Long id) {
		deptMapper.deleteById(id);
		return R.success("删除成功");
	}
}
