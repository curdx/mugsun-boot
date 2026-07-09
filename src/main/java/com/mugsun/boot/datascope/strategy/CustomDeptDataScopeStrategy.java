package com.mugsun.boot.datascope.strategy;

import com.mugsun.boot.datascope.DataScopeContext;
import com.mugsun.boot.datascope.DataScopeStrategy;
import com.mugsun.boot.system.entity.SysRoleDept;
import com.mugsun.boot.system.entity.SysUserRole;
import com.mugsun.boot.system.mapper.SysRoleDeptMapper;
import com.mugsun.boot.system.mapper.SysUserRoleMapper;
import com.mybatisflex.core.query.QueryWrapper;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.function.Consumer;

/**
 * 自定义部门：可见部门取当前用户所属角色配置的自定义部门集合（sys_role_dept）。
 * 未配置任何部门时按不可见处理（in (-1)），杜绝穿透。
 */
@Component
public class CustomDeptDataScopeStrategy implements DataScopeStrategy {

	private final SysUserRoleMapper userRoleMapper;
	private final SysRoleDeptMapper roleDeptMapper;

	public CustomDeptDataScopeStrategy(SysUserRoleMapper userRoleMapper, SysRoleDeptMapper roleDeptMapper) {
		this.userRoleMapper = userRoleMapper;
		this.roleDeptMapper = roleDeptMapper;
	}

	@Override
	public int scope() {
		return 5;
	}

	@Override
	public Consumer<QueryWrapper> build(DataScopeContext.Scope ctx, String deptColumn, String userColumn) {
		List<Long> deptIds = List.of(-1L);
		if (ctx.userId() != null) {
			List<Long> roleIds = userRoleMapper
				.selectListByQuery(QueryWrapper.create().eq("user_id", ctx.userId()))
				.stream().map(SysUserRole::getRoleId).toList();
			if (!roleIds.isEmpty()) {
				List<Long> ids = roleDeptMapper
					.selectListByQuery(QueryWrapper.create().in("role_id", roleIds))
					.stream().map(SysRoleDept::getDeptId).distinct().toList();
				if (!ids.isEmpty()) {
					deptIds = ids;
				}
			}
		}
		List<Long> finalIds = deptIds;
		return query -> query.in(deptColumn, finalIds);
	}
}
