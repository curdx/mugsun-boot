package com.mugsun.boot.system.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.dev33.satoken.stp.StpUtil;
import com.mugsun.boot.datascope.DataScopeContext;
import com.mugsun.boot.log.AuditService;
import com.mugsun.boot.log.OperationLog;
import com.mugsun.boot.system.entity.SysUser;
import com.mugsun.boot.system.entity.SysDept;
import com.mugsun.boot.system.entity.SysUserRole;
import com.mugsun.boot.system.excel.SysUserExcel;
import com.mugsun.boot.system.mapper.SysUserMapper;
import com.mugsun.boot.system.mapper.SysDeptMapper;
import com.mugsun.boot.system.mapper.SysUserRoleMapper;
import com.mugsun.boot.system.payload.StatusParam;
import com.mugsun.boot.system.payload.UserGrantParam;
import com.mugsun.core.tool.api.R;
import com.mugsun.core.web.excel.ExcelUtil;
import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.core.mask.MaskManager;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Set;

/**
 * 用户管理
 */
@RestController
@RequestMapping("/system/user")
@SaCheckLogin
public class SysUserController {

	private final SysUserMapper userMapper;
	private final PasswordEncoder passwordEncoder;
	private final AuditService auditService;
	private final SysUserRoleMapper userRoleMapper;
	private final SysDeptMapper deptMapper;

	public SysUserController(SysUserMapper userMapper, PasswordEncoder passwordEncoder, AuditService auditService,
							 SysUserRoleMapper userRoleMapper, SysDeptMapper deptMapper) {
		this.userMapper = userMapper;
		this.passwordEncoder = passwordEncoder;
		this.auditService = auditService;
		this.userRoleMapper = userRoleMapper;
		this.deptMapper = deptMapper;
	}

	@GetMapping("/page")
	@SaCheckPermission("sys:user:list")
	public R<Page<SysUser>> page(@RequestParam(defaultValue = "1") long pageNum,
								 @RequestParam(defaultValue = "10") long pageSize) {
		QueryWrapper query = QueryWrapper.create().orderBy("id", false);
		// 行级数据权限：1全部 / 2本部门 / 3本部门及子部门 / 4仅本人
		DataScopeContext.Scope scope = DataScopeContext.get();
		if (scope != null && scope.dataScope() != null) {
			int ds = scope.dataScope();
			if (ds == 2 && scope.deptId() != null) {
				query.and("dept_id = ?", scope.deptId());
			} else if (ds == 3 && scope.deptId() != null) {
				query.in("dept_id", subtreeDeptIds(scope.deptId()));
			} else if (ds == 4 && scope.userId() != null) {
				query.and("id = ?", scope.userId());
			}
		}
		Page<SysUser> page = userMapper.paginate(pageNum, pageSize, query);
		// 密码脱敏
		page.getRecords().forEach(u -> u.setPassword(null));
		return R.data(page);
	}

	/** 本部门及子部门：以 parent_id 关系向下广度遍历得到部门子树 id 集合（含自身） */
	private List<Long> subtreeDeptIds(Long rootId) {
		List<SysDept> all = deptMapper.selectAll();
		Set<Long> result = new java.util.HashSet<>();
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

	@GetMapping("/detail")
	public R<SysUser> detail(@RequestParam Long id) {
		// 详情用于编辑回显，需真实手机号：execWithoutMask 跳过脱敏（身份证由 TypeHandler 自动解密）
		SysUser user = MaskManager.execWithoutMask(() -> userMapper.selectOneById(id));
		if (user != null) {
			user.setPassword(null);
		}
		return R.data(user);
	}

	@PostMapping("/submit")
	@OperationLog("保存用户")
	public R<Void> submit(@RequestBody SysUser user) {
		if (user.getId() == null) {
			String raw = (user.getPassword() == null || user.getPassword().isBlank()) ? "123456" : user.getPassword();
			user.setPassword(passwordEncoder.encode(raw));
			userMapper.insert(user);
		} else {
			SysUser before = userMapper.selectOneById(user.getId());
			if (user.getPassword() != null && !user.getPassword().isBlank()) {
				user.setPassword(passwordEncoder.encode(user.getPassword()));
			}
			userMapper.update(user);
			SysUser after = userMapper.selectOneById(user.getId());
			if (before != null) {
				before.setPassword(null);
			}
			if (after != null) {
				after.setPassword(null);
			}
			Object operator = StpUtil.getLoginIdDefaultNull();
			auditService.record("sys_user", user.getId().toString(), before, after,
				operator == null ? null : operator.toString());
		}
		return R.success("操作成功");
	}

	@PostMapping("/remove")
	public R<Void> remove(@RequestBody List<Long> ids) {
		userMapper.deleteBatchByIds(ids);
		return R.success("删除成功");
	}

	@GetMapping("/export")
	@SaCheckPermission("sys:user:list")
	public void export(HttpServletResponse response) {
		List<SysUserExcel> rows = userMapper.selectListByQuery(QueryWrapper.create().orderBy("id", false))
			.stream().map(user -> {
				SysUserExcel row = new SysUserExcel();
				row.setUsername(user.getUsername());
				row.setNickname(user.getNickname());
				row.setStatus(user.getStatus());
				return row;
			}).toList();
		ExcelUtil.export(response, "用户数据", "用户", rows, SysUserExcel.class);
	}

	@PostMapping("/import")
	@OperationLog("导入用户")
	public R<Void> importUser(MultipartFile file) {
		List<SysUserExcel> rows = ExcelUtil.read(file, SysUserExcel.class);
		int inserted = 0;
		for (SysUserExcel row : rows) {
			String username = row.getUsername() == null ? null : row.getUsername().trim();
			if (username == null || username.isEmpty()) {
				continue;
			}
			if (userMapper.selectCountByQuery(QueryWrapper.create().eq("username", username)) > 0) {
				continue;
			}
			SysUser user = new SysUser();
			user.setUsername(username);
			user.setNickname(row.getNickname());
			user.setStatus(row.getStatus() == null ? 1 : row.getStatus());
			user.setPassword(passwordEncoder.encode("123456"));
			userMapper.insert(user);
			inserted++;
		}
		return R.success("导入完成，新增 " + inserted + " 条");
	}

	/** 重置密码为默认 123456（批量） */
	@PostMapping("/reset-password")
	@OperationLog("重置密码")
	public R<Void> resetPassword(@RequestBody List<Long> ids) {
		for (Long id : ids) {
			SysUser user = new SysUser();
			user.setId(id);
			user.setPassword(passwordEncoder.encode("123456"));
			userMapper.update(user);
		}
		return R.success("密码已重置为 123456");
	}

	/** 启用 / 停用用户 */
	@PostMapping("/status")
	@OperationLog("变更用户状态")
	public R<Void> status(@RequestBody StatusParam param) {
		SysUser user = new SysUser();
		user.setId(param.id());
		user.setStatus(param.status());
		userMapper.update(user);
		return R.success("操作成功");
	}

	/** 查询用户已授权角色 id 集合（授权回显） */
	@GetMapping("/role-ids")
	public R<List<Long>> roleIds(@RequestParam Long userId) {
		List<Long> ids = userRoleMapper
			.selectListByQuery(QueryWrapper.create().eq("user_id", userId))
			.stream()
			.map(SysUserRole::getRoleId)
			.toList();
		return R.data(ids);
	}

	/** 用户授权角色 */
	@PostMapping("/grant")
	@OperationLog("用户授权")
	public R<Void> grant(@RequestBody UserGrantParam param) {
		userRoleMapper.deleteByQuery(QueryWrapper.create().eq("user_id", param.userId()));
		if (param.roleIds() != null) {
			for (Long roleId : param.roleIds()) {
				SysUserRole userRole = new SysUserRole();
				userRole.setUserId(param.userId());
				userRole.setRoleId(roleId);
				userRoleMapper.insert(userRole);
			}
		}
		return R.success("授权成功");
	}
}
