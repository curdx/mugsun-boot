package com.mugsun.boot.system.service;

import com.mugsun.boot.common.constant.RoleConstants;
import com.mugsun.boot.common.constant.UserConstants;
import com.mugsun.boot.system.entity.SysDept;
import com.mugsun.boot.system.entity.SysPost;
import com.mugsun.boot.system.entity.SysRole;
import com.mugsun.boot.system.entity.SysTenant;
import com.mugsun.boot.system.entity.SysUser;
import com.mugsun.boot.system.entity.SysUserRole;
import com.mugsun.boot.system.mapper.SysDeptMapper;
import com.mugsun.boot.system.mapper.SysPostMapper;
import com.mugsun.boot.system.mapper.SysRoleMapper;
import com.mugsun.boot.system.mapper.SysTenantMapper;
import com.mugsun.boot.system.mapper.SysUserMapper;
import com.mugsun.boot.system.mapper.SysUserRoleMapper;
import com.mybatisflex.core.query.QueryWrapper;
import com.mugsun.boot.tenant.TenantContext;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Random;

/**
 * 租户服务：创建租户并一键初始化其默认部门/岗位/角色/管理员。
 */
@Service
public class TenantService {

	private final SysTenantMapper tenantMapper;
	private final SysDeptMapper deptMapper;
	private final SysPostMapper postMapper;
	private final SysRoleMapper roleMapper;
	private final SysUserMapper userMapper;
	private final SysUserRoleMapper userRoleMapper;
	private final PasswordEncoder passwordEncoder;
	private final Random random = new Random();

	public TenantService(SysTenantMapper tenantMapper, SysDeptMapper deptMapper, SysPostMapper postMapper,
						 SysRoleMapper roleMapper, SysUserMapper userMapper, SysUserRoleMapper userRoleMapper,
						 PasswordEncoder passwordEncoder) {
		this.tenantMapper = tenantMapper;
		this.deptMapper = deptMapper;
		this.postMapper = postMapper;
		this.roleMapper = roleMapper;
		this.userMapper = userMapper;
		this.userRoleMapper = userRoleMapper;
		this.passwordEncoder = passwordEncoder;
	}

	/** 创建租户并初始化默认数据，返回租户编号 */
	@Transactional(rollbackFor = Exception.class)
	public String create(SysTenant tenant) {
		String code = genCode();
		tenant.setTenantCode(code);
		tenantMapper.insertSelective(tenant);
		// 忽略当前会话租户条件，按新租户编号写入默认数据
		TenantContext.ignore(() -> initDefaults(code, tenant.getTenantName()));
		return code;
	}

	private void initDefaults(String code, String tenantName) {
		SysDept dept = new SysDept();
		dept.setDeptName(tenantName);
		dept.setParentId(0L);
		dept.setSort(1);
		dept.setTenantId(code);
		deptMapper.insertSelective(dept);

		SysPost post = new SysPost();
		post.setPostCode("default");
		post.setPostName("默认岗位");
		post.setSort(1);
		post.setTenantId(code);
		postMapper.insertSelective(post);

		SysRole role = new SysRole();
		role.setRoleName("管理员");
		role.setRoleCode(RoleConstants.ADMIN);
		role.setSort(1);
		role.setDataScope(1);
		role.setTenantId(code);
		roleMapper.insertSelective(role);

		SysUser admin = new SysUser();
		admin.setUsername(UserConstants.ADMIN_USERNAME);
		admin.setPassword(passwordEncoder.encode("123456"));
		admin.setNickname(tenantName + "管理员");
		admin.setStatus(1);
		admin.setDeptId(dept.getId());
		admin.setPostId(post.getId());
		admin.setTenantId(code);
		userMapper.insertSelective(admin);

		SysUserRole userRole = new SysUserRole();
		userRole.setUserId(admin.getId());
		userRole.setRoleId(role.getId());
		userRoleMapper.insertSelective(userRole);
	}

	/** 生成 6 位租户编号，规避超管保留号 000000 且不重复 */
	private String genCode() {
		String code;
		do {
			code = String.valueOf(100000 + random.nextInt(900000));
		} while (tenantMapper.selectCountByQuery(QueryWrapper.create().eq("tenant_code", code)) > 0);
		return code;
	}
}
