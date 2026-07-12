package com.mugsun.boot.system.service;

import com.mugsun.boot.common.constant.FlowConstants;
import com.mugsun.boot.common.constant.RoleConstants;
import com.mybatisflex.core.row.Db;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * 审批候选人解析引擎：把节点 permissionFlag 里的 storageId（role:/dept:/post:/user: 前缀或裸值）
 * 解析、合并成实际用户 id 集；并提供发起人部门负责人、兜底超管等派生查询。
 * <p>上下文无关的前缀在 PermissionHandler.convertPermissions 落库前解析；需实例上下文的占位
 * （发起人/部门负责人）在 assignment 监听器解析。
 */
@Service
public class HandlerSelectService {

	/** 静态前缀解析：role:/dept:/post:/user: → 用户 id；占位符与裸值原样透传（交监听器/引擎） */
	public List<String> resolveStatic(List<String> flags) {
		Set<String> ids = new LinkedHashSet<>();
		if (flags == null) {
			return new ArrayList<>();
		}
		for (String f : flags) {
			if (f == null || f.isBlank()) {
				continue;
			}
			if (f.startsWith(FlowConstants.CANDIDATE_ROLE)) {
				ids.addAll(byRole(f.substring(FlowConstants.CANDIDATE_ROLE.length())));
			} else if (f.startsWith(FlowConstants.CANDIDATE_DEPT)) {
				ids.addAll(byDept(f.substring(FlowConstants.CANDIDATE_DEPT.length())));
			} else if (f.startsWith(FlowConstants.CANDIDATE_POST)) {
				ids.addAll(byPost(f.substring(FlowConstants.CANDIDATE_POST.length())));
			} else if (f.startsWith(FlowConstants.CANDIDATE_USER)) {
				ids.add(f.substring(FlowConstants.CANDIDATE_USER.length()));
			} else {
				// 裸值（userId / 旧流程角色码 / 占位符）透传
				ids.add(f);
			}
		}
		return new ArrayList<>(ids);
	}

	/** 某角色码下全部用户 id */
	public List<String> byRole(String roleCode) {
		return column("select u.id as \"v\" from sys_user u "
			+ "join sys_user_role ur on ur.user_id = u.id and ur.is_deleted = 0 "
			+ "join sys_role r on r.id = ur.role_id and r.is_deleted = 0 "
			+ "where r.role_code = ? and u.is_deleted = 0", roleCode);
	}

	/** 某部门下全部用户 id */
	public List<String> byDept(String deptId) {
		return column("select id as \"v\" from sys_user where dept_id = ? and is_deleted = 0", asLong(deptId));
	}

	/** 某岗位下全部用户 id */
	public List<String> byPost(String postId) {
		return column("select id as \"v\" from sys_user where post_id = ? and is_deleted = 0", asLong(postId));
	}

	/** 某用户所在部门的负责人 id（sys_dept.leader_id） */
	public List<String> deptLeaderByUser(String userId) {
		return column("select d.leader_id as \"v\" from sys_user u "
			+ "join sys_dept d on d.id = u.dept_id and d.is_deleted = 0 "
			+ "where u.id = ? and u.is_deleted = 0 and d.leader_id is not null", asLong(userId));
	}

	/** 兜底办理人：超管用户 id 集（空候选/审批人==发起人时用，fail-closed） */
	public List<String> fallbackHandlers() {
		return byRole(RoleConstants.ADMIN);
	}

	private List<String> column(String sql, Object arg) {
		List<String> list = new ArrayList<>();
		Db.selectListBySql(sql, arg).forEach(row -> {
			String v = row.getString("v");
			if (v != null && !v.isBlank()) {
				list.add(v);
			}
		});
		return list;
	}

	private Long asLong(String s) {
		try {
			return Long.valueOf(s.trim());
		} catch (NumberFormatException e) {
			return -1L;
		}
	}
}
