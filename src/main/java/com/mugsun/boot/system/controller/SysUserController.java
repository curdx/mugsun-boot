package com.mugsun.boot.system.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.dev33.satoken.stp.StpUtil;
import com.mugsun.boot.datascope.DataScope;
import com.mugsun.boot.log.AuditService;
import com.mugsun.boot.log.OperationLog;
import com.mugsun.boot.system.entity.SysUser;
import com.mugsun.boot.system.entity.SysUserRole;
import com.mugsun.boot.system.excel.SysUserExcel;
import com.mugsun.boot.system.mapper.SysUserMapper;
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

import java.util.List;

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
	private final com.mugsun.boot.security.SecurityPolicyService securityPolicyService;
	private final com.mugsun.boot.tenant.TenantValidator tenantValidator;

	public SysUserController(SysUserMapper userMapper, PasswordEncoder passwordEncoder, AuditService auditService,
							 SysUserRoleMapper userRoleMapper,
							 com.mugsun.boot.security.SecurityPolicyService securityPolicyService,
							 com.mugsun.boot.tenant.TenantValidator tenantValidator) {
		this.userMapper = userMapper;
		this.passwordEncoder = passwordEncoder;
		this.auditService = auditService;
		this.userRoleMapper = userRoleMapper;
		this.securityPolicyService = securityPolicyService;
		this.tenantValidator = tenantValidator;
	}

	@GetMapping("/page")
	@SaCheckPermission("sys:user:list")
	@DataScope
	public R<Page<SysUser>> page(@RequestParam(defaultValue = "1") long pageNum,
								 @RequestParam(defaultValue = "10") long pageSize) {
		QueryWrapper query = QueryWrapper.create().orderBy("id", false);
		// 行级数据权限：@DataScope 激活后由数据权限方言自动注入 OR 并集条件（无需手工 apply）
		Page<SysUser> page = userMapper.paginate(pageNum, pageSize, query);
		// 密码脱敏
		page.getRecords().forEach(u -> u.setPassword(null));
		return R.data(page);
	}

	@GetMapping("/detail")
	@SaCheckPermission("sys:user:list")
	public R<SysUser> detail(@RequestParam Long id) {
		// 详情用于编辑回显，需真实手机号：execWithoutMask 跳过脱敏（身份证由 TypeHandler 自动解密）
		SysUser user = MaskManager.execWithoutMask(() -> userMapper.selectOneById(id));
		if (user != null) {
			user.setPassword(null);
		}
		return R.data(user);
	}

	/** 用户下拉选项（value=id / label=昵称，供收件人选择等场景，仅启用用户） */
	@GetMapping("/select")
	public R<List<java.util.Map<String, Object>>> select() {
		return R.data(userMapper.selectListByQuery(QueryWrapper.create().eq("status", 1).orderBy("id", false)).stream()
			.map(u -> {
				java.util.Map<String, Object> option = new java.util.HashMap<>();
				option.put("value", u.getId());
				option.put("label", (u.getNickname() == null ? u.getUsername() : u.getNickname())
					+ "（" + u.getUsername() + "）");
				return option;
			})
			.toList());
	}

	@PostMapping("/submit")
	@OperationLog("保存用户")
	public R<Void> submit(@RequestBody SysUser user) {
		// 新增走 sys:user:add，编辑走 sys:user:edit（与前端按钮门控码对齐，避免"可见却越权失败"）
		StpUtil.checkPermission(user.getId() == null ? "sys:user:add" : "sys:user:edit");
		if (user.getId() == null) {
			// 账号数配额：按当前租户 account_count 上限拦截（平台租户/不限额放行）
			tenantValidator.assertAccountQuota(com.mugsun.boot.tenant.TenantContext.current());
			String raw = (user.getPassword() == null || user.getPassword().isBlank()) ? "123456" : user.getPassword();
			user.setPassword(passwordEncoder.encode(raw));
			userMapper.insert(user);
			securityPolicyService.logPassword(user.getId(), user.getPassword());
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
	@SaCheckPermission("sys:user:remove")
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
	@SaCheckPermission("sys:user:add")
	@OperationLog("导入用户")
	public R<Void> importUser(MultipartFile file) {
		List<SysUserExcel> rows = ExcelUtil.read(file, SysUserExcel.class);
		int inserted = 0;
		String tenant = com.mugsun.boot.tenant.TenantContext.current();
		for (SysUserExcel row : rows) {
			String username = row.getUsername() == null ? null : row.getUsername().trim();
			if (username == null || username.isEmpty()) {
				continue;
			}
			if (userMapper.selectCountByQuery(QueryWrapper.create().eq("username", username)) > 0) {
				continue;
			}
			// 账号数配额：逐条入库前校验租户上限，超额即停（平台租户/不限额放行）
			tenantValidator.assertAccountQuota(tenant);
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
	@SaCheckPermission("sys:user:reset")
	@OperationLog("重置密码")
	public R<Void> resetPassword(@RequestBody List<Long> ids) {
		for (Long id : ids) {
			SysUser user = new SysUser();
			user.setId(id);
			String encoded = passwordEncoder.encode("123456");
			user.setPassword(encoded);
			userMapper.update(user);
			securityPolicyService.logPassword(id, encoded);
		}
		return R.success("密码已重置为 123456");
	}

	/** 启用 / 停用用户 */
	@PostMapping("/status")
	@SaCheckPermission("sys:user:edit")
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
	@SaCheckPermission("sys:user:grant")
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
