package com.mugsun.boot.config;

import com.mugsun.boot.common.constant.RoleConstants;
import com.mugsun.boot.common.constant.TenantConstants;
import com.mugsun.boot.common.constant.UserConstants;
import com.mugsun.boot.tenant.TenantContext;
import com.mugsun.boot.system.entity.SysMenu;
import com.mugsun.boot.system.entity.SysRole;
import com.mugsun.boot.system.entity.SysRoleMenu;
import com.mugsun.boot.system.entity.SysUser;
import com.mugsun.boot.system.entity.SysUserRole;
import com.mugsun.boot.system.mapper.SysMenuMapper;
import com.mugsun.boot.system.mapper.SysRoleMapper;
import com.mugsun.boot.system.mapper.SysRoleMenuMapper;
import com.mugsun.boot.system.mapper.SysUserMapper;
import com.mugsun.boot.system.mapper.SysUserRoleMapper;
import com.mybatisflex.core.query.QueryWrapper;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * 初始化数据：首次启动创建超级管理员 admin/123456 及其角色、菜单权限
 */
@Component
public class DataInitializer implements CommandLineRunner {

	private final SysUserMapper userMapper;
	private final SysRoleMapper roleMapper;
	private final SysMenuMapper menuMapper;
	private final SysUserRoleMapper userRoleMapper;
	private final SysRoleMenuMapper roleMenuMapper;
	private final PasswordEncoder passwordEncoder;

	public DataInitializer(SysUserMapper userMapper, SysRoleMapper roleMapper, SysMenuMapper menuMapper,
						   SysUserRoleMapper userRoleMapper, SysRoleMenuMapper roleMenuMapper,
						   PasswordEncoder passwordEncoder) {
		this.userMapper = userMapper;
		this.roleMapper = roleMapper;
		this.menuMapper = menuMapper;
		this.userRoleMapper = userRoleMapper;
		this.roleMenuMapper = roleMenuMapper;
		this.passwordEncoder = passwordEncoder;
	}

	@Override
	public void run(String... args) {
		// 启动初始化写平台租户表，无会话上下文——经中心忽略入口显式声明，规避 fail-closed
		TenantContext.ignore(() -> {
			seed();
			reanchorSeedMenus();
			return null;
		});
	}

	/**
	 * 种子按钮/授权重锚（幂等）：V39/V45/V46 迁移含硬编码雪花 ID，全新环境播种菜单 ID 不同会悬空——
	 * 启动时按业务键（permission/role_code）把种子菜单挂回真实父菜单、授权挂回 datatest 角色。
	 */
	private void reanchorSeedMenus() {
		com.mybatisflex.core.row.Db.updateBySql(
			"UPDATE sys_menu SET parent_id = (SELECT id FROM sys_menu WHERE permission = 'sys:user:list' AND is_deleted = 0 LIMIT 1) "
				+ "WHERE permission IN ('sys:user:add','sys:user:edit','sys:user:remove','sys:user:grant','sys:user:reset',"
				+ "'sys:user:phone','sys:user:phone:plain','sys:user:idcard','sys:user:idcard:plain') AND is_deleted = 0 "
				+ "AND EXISTS (SELECT 1 FROM sys_menu WHERE permission = 'sys:user:list' AND is_deleted = 0)");
		com.mybatisflex.core.row.Db.updateBySql(
			"UPDATE sys_menu SET parent_id = (SELECT parent_id FROM sys_menu WHERE permission = 'sys:user:list' AND is_deleted = 0 LIMIT 1) "
				+ "WHERE permission = 'sys:gen:list' AND is_deleted = 0 "
				+ "AND EXISTS (SELECT 1 FROM sys_menu WHERE permission = 'sys:user:list' AND is_deleted = 0)");
		com.mybatisflex.core.row.Db.updateBySql(
			"UPDATE sys_role_menu SET role_id = (SELECT id FROM sys_role WHERE role_code = 'datatest' AND is_deleted = 0 LIMIT 1) "
				+ "WHERE menu_id IN (SELECT id FROM sys_menu WHERE permission IN ('sys:user:add','sys:user:phone') AND is_deleted = 0) "
				+ "AND is_deleted = 0 AND role_id NOT IN (SELECT id FROM sys_role) "
				+ "AND EXISTS (SELECT 1 FROM sys_role WHERE role_code = 'datatest' AND is_deleted = 0)");
	}

	private void seed() {
		if (userMapper.selectCountByQuery(QueryWrapper.create().eq("username", UserConstants.ADMIN_USERNAME)) > 0) {
			return;
		}
		// 角色
		SysRole adminRole = new SysRole();
		adminRole.setRoleName("超级管理员");
		adminRole.setRoleCode(RoleConstants.ADMIN);
		adminRole.setSort(1);
		adminRole.setDataScope(com.mugsun.boot.common.constant.DataScopeConstants.ALL);
		adminRole.setTenantId(TenantConstants.DEFAULT_TENANT_ID);
		roleMapper.insert(adminRole);
		// 菜单：系统管理 > 用户管理
		SysMenu systemMenu = new SysMenu();
		systemMenu.setParentId(0L);
		systemMenu.setMenuName("系统管理");
		systemMenu.setPath("/system");
		systemMenu.setMenuType("M");
		systemMenu.setSort(1);
		menuMapper.insert(systemMenu);
		SysMenu userMenu = new SysMenu();
		userMenu.setParentId(systemMenu.getId());
		userMenu.setMenuName("用户管理");
		userMenu.setPath("/system/user");
		userMenu.setMenuType("M");
		userMenu.setPermission("sys:user:list");
		userMenu.setSort(1);
		menuMapper.insert(userMenu);
		// 管理员用户
		SysUser admin = new SysUser();
		admin.setUsername(UserConstants.ADMIN_USERNAME);
		admin.setPassword(passwordEncoder.encode("123456"));
		admin.setNickname("超级管理员");
		admin.setStatus(1);
		admin.setTenantId(TenantConstants.DEFAULT_TENANT_ID);
		userMapper.insert(admin);
		// 用户-角色
		SysUserRole userRole = new SysUserRole();
		userRole.setUserId(admin.getId());
		userRole.setRoleId(adminRole.getId());
		userRoleMapper.insert(userRole);
		// 角色-菜单
		for (Long menuId : new Long[]{systemMenu.getId(), userMenu.getId()}) {
			SysRoleMenu roleMenu = new SysRoleMenu();
			roleMenu.setRoleId(adminRole.getId());
			roleMenu.setMenuId(menuId);
			roleMenuMapper.insert(roleMenu);
		}
	}
}
