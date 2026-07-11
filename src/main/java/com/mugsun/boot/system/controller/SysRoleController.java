package com.mugsun.boot.system.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.annotation.SaCheckPermission;
import com.mugsun.boot.system.entity.SysRole;
import com.mugsun.boot.system.entity.SysRoleDept;
import com.mugsun.boot.system.entity.SysRoleMenu;
import com.mugsun.boot.system.mapper.SysRoleDeptMapper;
import com.mugsun.boot.system.mapper.SysRoleMapper;
import com.mugsun.boot.system.mapper.SysRoleMenuMapper;
import com.mugsun.boot.system.payload.GrantParam;
import com.mugsun.core.tool.api.R;
import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.query.QueryWrapper;
import org.springframework.transaction.annotation.Transactional;
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
	private final SysRoleDeptMapper roleDeptMapper;

	public SysRoleController(SysRoleMapper roleMapper, SysRoleMenuMapper roleMenuMapper,
							 SysRoleDeptMapper roleDeptMapper) {
		this.roleMapper = roleMapper;
		this.roleMenuMapper = roleMenuMapper;
		this.roleDeptMapper = roleDeptMapper;
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

	/** 角色码下拉选项（value=角色码），供流程审批人等按角色码匹配的场景消费 */
	@GetMapping("/code-select")
	public R<List<Map<String, Object>>> codeSelect() {
		List<Map<String, Object>> options = roleMapper
			.selectListByQuery(QueryWrapper.create().orderBy("sort", true))
			.stream()
			.map(role -> {
				Map<String, Object> option = new HashMap<>();
				option.put("value", role.getRoleCode());
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

	@SaCheckPermission("sys:role:save")
	@PostMapping("/submit")
	@Transactional(rollbackFor = Exception.class)
	public R<Void> submit(@RequestBody SysRole role) {
		if (role.getId() == null) {
			roleMapper.insert(role);
		} else {
			roleMapper.update(role);
		}
		syncRoleDept(role);
		return R.success("操作成功");
	}

	/** 同步角色自定义部门：data_scope=5 时按 deptIds 重建，否则清空（避免残留旧配置穿透） */
	private void syncRoleDept(SysRole role) {
		roleDeptMapper.deleteByQuery(QueryWrapper.create().eq("role_id", role.getId()));
		if (role.getDataScope() != null && role.getDataScope() == 5 && role.getDeptIds() != null) {
			for (Long deptId : role.getDeptIds()) {
				SysRoleDept roleDept = new SysRoleDept();
				roleDept.setRoleId(role.getId());
				roleDept.setDeptId(deptId);
				roleDeptMapper.insert(roleDept);
			}
		}
	}

	/** 角色自定义部门 id 集合（data_scope=5 授权回显） */
	@GetMapping("/dept-ids")
	public R<List<Long>> deptIds(@RequestParam Long roleId) {
		List<Long> ids = roleDeptMapper
			.selectListByQuery(QueryWrapper.create().eq("role_id", roleId))
			.stream()
			.map(SysRoleDept::getDeptId)
			.toList();
		return R.data(ids);
	}

	@SaCheckPermission("sys:role:remove")
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
	@SaCheckPermission("sys:role:grant")
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
