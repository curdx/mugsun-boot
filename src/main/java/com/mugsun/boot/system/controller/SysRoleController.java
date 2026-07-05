package com.mugsun.boot.system.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import com.mugsun.boot.system.entity.SysRole;
import com.mugsun.boot.system.entity.SysRoleMenu;
import com.mugsun.boot.system.mapper.SysRoleMapper;
import com.mugsun.boot.system.mapper.SysRoleMenuMapper;
import com.mugsun.boot.system.payload.GrantParam;
import com.mugsun.core.tool.api.R;
import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.query.QueryWrapper;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 角色管理
 */
@RestController
@RequestMapping("/system/role")
@SaCheckLogin
public class SysRoleController {

	private final SysRoleMapper roleMapper;
	private final SysRoleMenuMapper roleMenuMapper;

	public SysRoleController(SysRoleMapper roleMapper, SysRoleMenuMapper roleMenuMapper) {
		this.roleMapper = roleMapper;
		this.roleMenuMapper = roleMenuMapper;
	}

	@GetMapping("/page")
	public R<Page<SysRole>> page(@RequestParam(defaultValue = "1") long pageNum,
								 @RequestParam(defaultValue = "10") long pageSize) {
		return R.data(roleMapper.paginate(pageNum, pageSize, QueryWrapper.create().orderBy("sort", true)));
	}

	/** 角色下拉选项（value/label 契约样例，供用户授权等场景消费） */
	@GetMapping("/select")
	public R<List<Map<String, Object>>> select() {
		List<Map<String, Object>> options = roleMapper
			.selectListByQuery(QueryWrapper.create().orderBy("sort", true))
			.stream()
			.map(role -> {
				Map<String, Object> option = new HashMap<>();
				option.put("value", role.getId());
				option.put("label", role.getRoleName());
				return option;
			})
			.toList();
		return R.data(options);
	}

	@GetMapping("/detail")
	public R<SysRole> detail(@RequestParam Long id) {
		return R.data(roleMapper.selectOneById(id));
	}

	@PostMapping("/submit")
	public R<Void> submit(@RequestBody SysRole role) {
		if (role.getId() == null) {
			roleMapper.insert(role);
		} else {
			roleMapper.update(role);
		}
		return R.success("操作成功");
	}

	@PostMapping("/remove")
	public R<Void> remove(@RequestBody List<Long> ids) {
		roleMapper.deleteBatchByIds(ids);
		return R.success("删除成功");
	}

	/** 查询角色已授权的菜单 id 集合（授权树回显） */
	@GetMapping("/menu-ids")
	public R<List<Long>> menuIds(@RequestParam Long roleId) {
		List<Long> ids = roleMenuMapper
			.selectListByQuery(QueryWrapper.create().eq("role_id", roleId))
			.stream()
			.map(SysRoleMenu::getMenuId)
			.toList();
		return R.data(ids);
	}

	/** 角色授权菜单 */
	@PostMapping("/grant")
	public R<Void> grant(@RequestBody GrantParam param) {
		roleMenuMapper.deleteByQuery(QueryWrapper.create().eq("role_id", param.roleId()));
		if (param.menuIds() != null) {
			for (Long menuId : param.menuIds()) {
				SysRoleMenu roleMenu = new SysRoleMenu();
				roleMenu.setRoleId(param.roleId());
				roleMenu.setMenuId(menuId);
				roleMenuMapper.insert(roleMenu);
			}
		}
		return R.success("授权成功");
	}
}
