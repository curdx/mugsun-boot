package com.mugsun.boot.config;

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
		if (userMapper.selectCountByQuery(QueryWrapper.create().eq("username", "admin")) > 0) {
			return;
		}
		// 角色
		SysRole adminRole = new SysRole();
		adminRole.setRoleName("超级管理员");
		adminRole.setRoleCode("admin");
		adminRole.setSort(1);
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
		admin.setUsername("admin");
		admin.setPassword(passwordEncoder.encode("123456"));
		admin.setNickname("超级管理员");
		admin.setStatus(1);
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
