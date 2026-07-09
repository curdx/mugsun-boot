package com.mugsun.boot.search;

import cn.dev33.satoken.annotation.SaCheckLogin;
import com.mugsun.boot.datascope.DataScopeContext;
import com.mugsun.boot.system.entity.SysDept;
import com.mugsun.boot.system.entity.SysNotice;
import com.mugsun.boot.system.entity.SysRole;
import com.mugsun.boot.system.entity.SysUser;
import com.mugsun.boot.system.mapper.SysDeptMapper;
import com.mugsun.boot.system.mapper.SysNoticeMapper;
import com.mugsun.boot.system.mapper.SysRoleMapper;
import com.mugsun.boot.system.mapper.SysUserMapper;
import com.mugsun.core.tool.api.R;
import com.mybatisflex.core.query.QueryWrapper;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 全局搜索·后端联想：顶栏搜索框在前端菜单联想之外，补充用户/部门/角色/通知等业务数据的实时联想。
 * 结果统一为 {type,typeName,title,subtitle,path}，前端按 type 分组、点击 path 跳转对应模块。
 * 多租户经 Flex 自动隔离；用户联想额外套用行级数据权限，杜绝越权发现；like 全程参数化防注入。
 */
@RestController
@RequestMapping("/system/search")
@SaCheckLogin
public class GlobalSearchController {

	/** 每类联想最多返回条数，避免弹层过长 */
	private static final int PER_TYPE_LIMIT = 5;

	private final SysUserMapper userMapper;
	private final SysDeptMapper deptMapper;
	private final SysRoleMapper roleMapper;
	private final SysNoticeMapper noticeMapper;

	public GlobalSearchController(SysUserMapper userMapper, SysDeptMapper deptMapper,
								  SysRoleMapper roleMapper, SysNoticeMapper noticeMapper) {
		this.userMapper = userMapper;
		this.deptMapper = deptMapper;
		this.roleMapper = roleMapper;
		this.noticeMapper = noticeMapper;
	}

	/** 关键词联想：跨用户/部门/角色/通知白名单数据源，每类取前 N 条 */
	@GetMapping("/suggest")
	public R<List<Map<String, Object>>> suggest(@RequestParam String keyword) {
		List<Map<String, Object>> out = new ArrayList<>();
		String kw = keyword == null ? "" : keyword.trim();
		if (kw.isEmpty()) {
			return R.data(out);
		}
		String pat = "%" + kw + "%";

		// 用户：套行级数据权限，仅暴露昵称/账号，绝不含密码/手机/身份证等敏感字段
		QueryWrapper uq = QueryWrapper.create()
			.and("(username like ? or nickname like ?)", pat, pat)
			.orderBy("id", false).limit(PER_TYPE_LIMIT);
		applyUserScope(uq);
		for (SysUser u : userMapper.selectListByQuery(uq)) {
			String title = u.getNickname() != null && !u.getNickname().isBlank() ? u.getNickname() : u.getUsername();
			out.add(item("user", "用户", title, "账号 " + u.getUsername(), "/system/user"));
		}

		// 部门
		QueryWrapper dq = QueryWrapper.create().like("dept_name", kw).orderBy("id", false).limit(PER_TYPE_LIMIT);
		for (SysDept d : deptMapper.selectListByQuery(dq)) {
			out.add(item("dept", "部门", d.getDeptName(), null, "/system/dept"));
		}

		// 角色
		QueryWrapper rq = QueryWrapper.create().like("role_name", kw).orderBy("id", false).limit(PER_TYPE_LIMIT);
		for (SysRole r : roleMapper.selectListByQuery(rq)) {
			out.add(item("role", "角色", r.getRoleName(), r.getRoleCode(), "/system/role"));
		}

		// 通知公告（业务数据）
		QueryWrapper nq = QueryWrapper.create().like("title", kw).orderBy("id", false).limit(PER_TYPE_LIMIT);
		for (SysNotice n : noticeMapper.selectListByQuery(nq)) {
			out.add(item("notice", "通知", n.getTitle(), null, "/system/notice"));
		}

		return R.data(out);
	}

	/** 组装联想项：type 供前端分组，path 供点击跳转 */
	private Map<String, Object> item(String type, String typeName, String title, String subtitle, String path) {
		Map<String, Object> m = new LinkedHashMap<>();
		m.put("type", type);
		m.put("typeName", typeName);
		m.put("title", title);
		m.put("subtitle", subtitle);
		m.put("path", path);
		return m;
	}

	/** 用户联想套行级数据权限，与 SysUserController.page 同口径（1全部/2本部门/3本部门及子/4仅本人） */
	private void applyUserScope(QueryWrapper query) {
		DataScopeContext.Scope scope = DataScopeContext.get();
		if (scope == null || scope.dataScope() == null) {
			return;
		}
		int ds = scope.dataScope();
		if (ds == 2 && scope.deptId() != null) {
			query.and("dept_id = ?", scope.deptId());
		} else if (ds == 3 && scope.deptId() != null) {
			query.in("dept_id", subtreeDeptIds(scope.deptId()));
		} else if (ds == 4 && scope.userId() != null) {
			query.and("id = ?", scope.userId());
		}
	}

	/** 本部门及子部门 id 集合（含自身），按 parent_id 向下广度遍历 */
	private List<Long> subtreeDeptIds(Long rootId) {
		List<SysDept> all = deptMapper.selectAll();
		Set<Long> result = new HashSet<>();
		Deque<Long> stack = new ArrayDeque<>();
		stack.push(rootId);
		result.add(rootId);
		while (!stack.isEmpty()) {
			Long cur = stack.pop();
			for (SysDept d : all) {
				if (cur.equals(d.getParentId()) && result.add(d.getId())) {
					stack.push(d.getId());
				}
			}
		}
		return new ArrayList<>(result);
	}
}
