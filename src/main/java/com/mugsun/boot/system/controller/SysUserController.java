package com.mugsun.boot.system.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.dev33.satoken.stp.StpUtil;
import com.mugsun.boot.datascope.DataScopeContext;
import com.mugsun.boot.log.AuditService;
import com.mugsun.boot.log.OperationLog;
import com.mugsun.boot.system.entity.SysUser;
import com.mugsun.boot.system.mapper.SysUserMapper;
import com.mugsun.core.tool.api.R;
import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.query.QueryWrapper;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

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

	public SysUserController(SysUserMapper userMapper, PasswordEncoder passwordEncoder, AuditService auditService) {
		this.userMapper = userMapper;
		this.passwordEncoder = passwordEncoder;
		this.auditService = auditService;
	}

	@GetMapping("/page")
	@SaCheckPermission("sys:user:list")
	public R<Page<SysUser>> page(@RequestParam(defaultValue = "1") long pageNum,
								 @RequestParam(defaultValue = "10") long pageSize) {
		QueryWrapper query = QueryWrapper.create().orderBy("id", false);
		// 行级数据权限：本部门 / 仅本人
		DataScopeContext.Scope scope = DataScopeContext.get();
		if (scope != null && scope.dataScope() != null) {
			if (scope.dataScope() == 2 && scope.deptId() != null) {
				query.and("dept_id = ?", scope.deptId());
			} else if (scope.dataScope() == 3 && scope.userId() != null) {
				query.and("id = ?", scope.userId());
			}
		}
		Page<SysUser> page = userMapper.paginate(pageNum, pageSize, query);
		// 密码脱敏
		page.getRecords().forEach(u -> u.setPassword(null));
		return R.data(page);
	}

	@GetMapping("/detail")
	public R<SysUser> detail(@RequestParam Long id) {
		SysUser user = userMapper.selectOneById(id);
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
	public R<Void> remove(@RequestParam Long id) {
		userMapper.deleteById(id);
		return R.success("删除成功");
	}
}
