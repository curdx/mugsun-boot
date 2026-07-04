package com.mugsun.boot.system.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import com.mugsun.boot.system.entity.SysMenu;
import com.mugsun.boot.system.mapper.SysMenuMapper;
import com.mugsun.core.tool.api.R;
import com.mugsun.core.tool.tree.TreeUtil;
import com.mybatisflex.core.query.QueryWrapper;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 菜单管理
 */
@RestController
@RequestMapping("/system/menu")
@SaCheckLogin
public class SysMenuController {

	private final SysMenuMapper menuMapper;

	public SysMenuController(SysMenuMapper menuMapper) {
		this.menuMapper = menuMapper;
	}

	@GetMapping("/tree")
	public R<List<SysMenu>> tree() {
		List<SysMenu> all = menuMapper.selectListByQuery(QueryWrapper.create().orderBy("sort", true));
		return R.data(TreeUtil.build(all, 0L));
	}

	@GetMapping("/detail")
	public R<SysMenu> detail(@RequestParam Long id) {
		return R.data(menuMapper.selectOneById(id));
	}

	@PostMapping("/submit")
	public R<Void> submit(@RequestBody SysMenu menu) {
		if (menu.getId() == null) {
			menuMapper.insert(menu);
		} else {
			menuMapper.update(menu);
		}
		return R.success("操作成功");
	}

	@PostMapping("/remove")
	public R<Void> remove(@RequestParam Long id) {
		menuMapper.deleteById(id);
		return R.success("删除成功");
	}
}
