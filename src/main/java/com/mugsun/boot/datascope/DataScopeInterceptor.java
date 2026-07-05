package com.mugsun.boot.datascope;

import cn.dev33.satoken.stp.StpUtil;
import com.mugsun.boot.system.entity.SysRole;
import com.mugsun.boot.system.entity.SysUser;
import com.mugsun.boot.system.entity.SysUserRole;
import com.mugsun.boot.system.mapper.SysRoleMapper;
import com.mugsun.boot.system.mapper.SysUserMapper;
import com.mugsun.boot.system.mapper.SysUserRoleMapper;
import com.mybatisflex.core.query.QueryWrapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 请求级设置数据权限上下文：取当前用户部门 + 其角色中范围最广的数据范围
 * 广度：1全部 > 3本部门及子 > 2本部门 > 4仅本人（多角色取并集即最广者）
 */
public class DataScopeInterceptor implements HandlerInterceptor {

	private final SysUserMapper userMapper;
	private final SysUserRoleMapper userRoleMapper;
	private final SysRoleMapper roleMapper;

	public DataScopeInterceptor(SysUserMapper userMapper, SysUserRoleMapper userRoleMapper, SysRoleMapper roleMapper) {
		this.userMapper = userMapper;
		this.userRoleMapper = userRoleMapper;
		this.roleMapper = roleMapper;
	}

	@Override
	public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
		if (StpUtil.isLogin()) {
			Long userId = StpUtil.getLoginIdAsLong();
			SysUser user = userMapper.selectOneById(userId);
			List<Long> roleIds = userRoleMapper.selectListByQuery(QueryWrapper.create().eq("user_id", userId))
				.stream().map(SysUserRole::getRoleId).collect(Collectors.toList());
			int dataScope = 4;
			if (!roleIds.isEmpty()) {
				dataScope = roleMapper.selectListByIds(roleIds).stream()
					.map(SysRole::getDataScope).filter(Objects::nonNull)
					.max(Comparator.comparingInt(DataScopeInterceptor::breadth)).orElse(4);
			}
			DataScopeContext.set(new DataScopeContext.Scope(dataScope, user != null ? user.getDeptId() : null, userId));
		}
		return true;
	}

	/** 数据范围广度排名（越大越广） */
	private static int breadth(int dataScope) {
		return switch (dataScope) {
			case 1 -> 4;
			case 3 -> 3;
			case 2 -> 2;
			default -> 1;
		};
	}

	@Override
	public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
		DataScopeContext.clear();
	}
}
