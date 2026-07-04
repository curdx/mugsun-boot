package com.mugsun.boot.datascope;

import com.mugsun.boot.system.mapper.SysRoleMapper;
import com.mugsun.boot.system.mapper.SysUserMapper;
import com.mugsun.boot.system.mapper.SysUserRoleMapper;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * 数据权限装配：注册请求拦截器（设置当前用户数据范围上下文）。
 * 采用「查询处手动追加 QueryWrapper 条件」而非全局方言，避免破坏数据库方言。
 */
@Configuration
public class DataScopeConfig implements WebMvcConfigurer {

	private final SysUserMapper userMapper;
	private final SysUserRoleMapper userRoleMapper;
	private final SysRoleMapper roleMapper;

	public DataScopeConfig(SysUserMapper userMapper, SysUserRoleMapper userRoleMapper, SysRoleMapper roleMapper) {
		this.userMapper = userMapper;
		this.userRoleMapper = userRoleMapper;
		this.roleMapper = roleMapper;
	}

	@Override
	public void addInterceptors(InterceptorRegistry registry) {
		registry.addInterceptor(new DataScopeInterceptor(userMapper, userRoleMapper, roleMapper))
			.addPathPatterns("/system/**");
	}
}
