package com.mugsun.boot.system.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.annotation.SaCheckPermission;
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

	@SaCheckPermission("sys:menu:save")
	@PostMapping("/submit")
	public R<Void> submit(@RequestBody SysMenu menu) {
		// 防环：父级不能是自身或自身后代（成环即子树蒸发 + 权限幽灵残留）
		if (menu.getId() != null && menu.getParentId() != null && !menu.getParentId().equals(0L)) {
			if (menu.getParentId().equals(menu.getId())) {
				throw new com.mugsun.core.tool.exception.ServiceException("上级菜单不能是自身");
			}
			if (isDescendant(menu.getParentId(), menu.getId())) {
				throw new com.mugsun.core.tool.exception.ServiceException("上级菜单不能是自身子级");
			}
		}
		if (menu.getId() == null) {
			menu.sanitizeForInsert();
			menuMapper.insert(menu);
		} else {
			menu.sanitizeForUpdate();
			menuMapper.update(menu);
		}
		return R.success("操作成功");
	}

	@SaCheckPermission("sys:menu:remove")
	@PostMapping("/remove")
	public R<Void> remove(@RequestBody List<Long> ids) {
		// 引用检查：存在子菜单时拒删（防子树蒸发与授权幽灵残留）
		for (Long id : ids) {
			if (menuMapper.selectCountByQuery(QueryWrapper.create().eq("parent_id", id)) > 0) {
				throw new com.mugsun.core.tool.exception.ServiceException("存在子菜单，请先删除子级");
			}
		}
		menuMapper.deleteBatchByIds(ids);
		return R.success("删除成功");
	}

	/** startId 的父链上是否含 targetId（成环判定：拟设父级的祖先里有我，则我成了自己的后代） */
	private boolean isDescendant(Long startId, Long targetId) {
		Long cur = startId;
		for (int i = 0; i < 64 && cur != null && !cur.equals(0L); i++) {
			if (cur.equals(targetId)) {
				return true;
			}
			SysMenu parent = menuMapper.selectOneById(cur);
			cur = parent == null ? null : parent.getParentId();
		}
		return false;
	}
}
