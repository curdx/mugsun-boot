package com.mugsun.boot.system.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.dev33.satoken.stp.StpUtil;
import com.mugsun.boot.common.constant.FieldMaskConstants;
import com.mugsun.boot.common.constant.NotifyConstants;
import com.mugsun.boot.common.tx.AfterCommit;
import com.mugsun.boot.datascope.DataScope;
import com.mugsun.boot.log.AuditService;
import com.mugsun.boot.log.OperationLog;
import com.mugsun.boot.notify.api.NotifyReceiver;
import com.mugsun.boot.notify.api.NotifySendApi;
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
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

	private static final Logger log = LoggerFactory.getLogger(SysUserController.class);

	private final SysUserMapper userMapper;
	private final PasswordEncoder passwordEncoder;
	private final AuditService auditService;
	private final SysUserRoleMapper userRoleMapper;
	private final com.mugsun.boot.system.mapper.SysRoleMapper roleMapper;
	private final com.mugsun.boot.security.SecurityPolicyService securityPolicyService;
	private final com.mugsun.boot.tenant.TenantValidator tenantValidator;
	private final NotifySendApi notifySendApi;

	public SysUserController(SysUserMapper userMapper, PasswordEncoder passwordEncoder, AuditService auditService,
							 SysUserRoleMapper userRoleMapper,
							 com.mugsun.boot.system.mapper.SysRoleMapper roleMapper,
							 com.mugsun.boot.security.SecurityPolicyService securityPolicyService,
							 com.mugsun.boot.tenant.TenantValidator tenantValidator,
							 NotifySendApi notifySendApi) {
		this.userMapper = userMapper;
		this.passwordEncoder = passwordEncoder;
		this.auditService = auditService;
		this.userRoleMapper = userRoleMapper;
		this.roleMapper = roleMapper;
		this.securityPolicyService = securityPolicyService;
		this.tenantValidator = tenantValidator;
		this.notifySendApi = notifySendApi;
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
	@DataScope
	public R<SysUser> detail(@RequestParam Long id) {
		// 手机号/身份证的 明文/脱敏/不可见 由按角色决策的脱敏处理器统一裁决（绑权限码而非绑端点，与 page 表现一致）
		// 行级：按 id 走 QueryWrapper 查询以经 @DataScope 方言注入数据范围（selectOneById 直查会绕过行级范围），越范围返 null
		SysUser user = userMapper.selectOneByQuery(QueryWrapper.create().eq("id", id));
		if (user != null) {
			user.setPassword(null);
		}
		return R.data(user);
	}

	/** 用户下拉选项（value=id / label=昵称，供收件人选择等场景，仅启用用户）；持码+数据范围约束，防全租户账号枚举 */
	@GetMapping("/select")
	@SaCheckPermission("sys:user:list")
	@DataScope
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
		// 字段级写门控（新建/编辑一致，读写权对称）：无字段明文权则不得写该敏感字段——
		// 编辑时置 null 交 Flex update 忽略、保留原值（防"看不到明文却把脱敏串覆盖入库"的污染与越权改写）；新建时即不落该字段
		if (!StpUtil.hasPermission(FieldMaskConstants.PERM_USER_PHONE_PLAIN)) {
			user.setPhone(null);
		}
		if (!StpUtil.hasPermission(FieldMaskConstants.PERM_USER_ID_CARD_PLAIN)) {
			user.setIdCard(null);
		}
		// 回写防护（编辑场景）：脱敏串（含 *）或密文形态值一律视为「未修改」置 null 交 Flex 忽略，
		// 杜绝脱敏值污染入库与密文二次加密（读管线任何回显形态都不会毁数据）
		if (user.getPhone() != null && user.getPhone().contains("*")) {
			user.setPhone(null);
		}
		if (user.getIdCard() != null
			&& (user.getIdCard().contains("*") || com.mugsun.boot.common.crypto.Sm4Util.looksLikeCipher(user.getIdCard()))) {
			user.setIdCard(null);
		}
		// 手机号格式校验（短信登录按号取人，脏号即冒注面）
		if (user.getPhone() != null && !user.getPhone().isBlank() && !user.getPhone().matches("^1\\d{10}$")) {
			throw new com.mugsun.core.tool.exception.ServiceException("手机号格式不正确");
		}
		if (user.getId() == null) {
			// 账号数配额：按当前租户 account_count 上限拦截（平台租户/不限额放行）
			tenantValidator.assertAccountQuota(com.mugsun.boot.tenant.TenantContext.current());
			String raw = (user.getPassword() == null || user.getPassword().isBlank()) ? "123456" : user.getPassword();
			user.setPassword(passwordEncoder.encode(raw));
			// 服务端清洗：审计字段与租户归属一律服务端裁定（Flex 仅对 null tenantId 才填充当前租户）
			user.sanitizeForInsert();
			user.setTenantId(null);
			userMapper.insert(user);
			securityPolicyService.logPassword(user.getId(), user.getPassword());
			notifyWelcome(user);
		} else {
			// 审计前后镜像恒脱敏读：与操作者角色无关，敏感字段永不落明文入审计
			SysUser before = com.mugsun.boot.common.mask.FieldMaskContext.maskedRead(() -> userMapper.selectOneById(user.getId()));
			if (before == null) {
				throw new com.mugsun.core.tool.exception.ServiceException("用户不存在");
			}
			assertTargetOperable(user.getId(), true);
			if (user.getPassword() != null && !user.getPassword().isBlank()) {
				user.setPassword(passwordEncoder.encode(user.getPassword()));
			}
			user.sanitizeForUpdate();
			user.setTenantId(null);
			userMapper.update(user);
			SysUser after = com.mugsun.boot.common.mask.FieldMaskContext.maskedRead(() -> userMapper.selectOneById(user.getId()));
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
	@OperationLog("删除用户")
	public R<Void> remove(@RequestBody List<Long> ids) {
		for (Long id : ids) {
			assertTargetOperable(id, false);
			userMapper.deleteById(id);
			// 删除即回收会话（逻辑删可挡新登录，但旧 token 默认 30 天有效，必须同步踢）
			StpUtil.kickout(id);
		}
		return R.success("删除成功");
	}

	@GetMapping("/export")
	@SaCheckPermission("sys:user:list")
	@DataScope
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

	/** 新用户欢迎通知：提交后按统一模板多渠道 fan-out；通知失败不阻断建用户（记日志） */
	private void notifyWelcome(SysUser user) {
		Long userId = user.getId();
		String name = user.getNickname() == null || user.getNickname().isBlank()
			? user.getUsername() : user.getNickname();
		String email = user.getEmail();
		String phone = user.getPhone();
		AfterCommit.execute(() -> {
			try {
				notifySendApi.send(NotifyConstants.TEMPLATE_WELCOME,
					java.util.List.of(NotifyReceiver.of(userId, name, email, phone)),
					java.util.Map.of("name", name));
			} catch (Exception e) {
				log.warn("新用户欢迎通知发送失败 userId={} err={}", userId, e.getMessage());
			}
		});
	}

	/** 重置密码为默认 123456（批量；事务原子，重置即踢全部在线端强制重登） */
	@PostMapping("/reset-password")
	@SaCheckPermission("sys:user:reset")
	@OperationLog("重置密码")
	@org.springframework.transaction.annotation.Transactional(rollbackFor = Exception.class)
	public R<Void> resetPassword(@RequestBody List<Long> ids) {
		for (Long id : ids) {
			assertTargetOperable(id, true);
			SysUser user = new SysUser();
			user.setId(id);
			String encoded = passwordEncoder.encode("123456");
			user.setPassword(encoded);
			userMapper.update(user);
			securityPolicyService.logPassword(id, encoded);
			StpUtil.kickout(id);
		}
		return R.success("密码已重置为 123456");
	}

	/** 启用 / 停用用户（停用即踢全部在线端） */
	@PostMapping("/status")
	@SaCheckPermission("sys:user:edit")
	@OperationLog("变更用户状态")
	public R<Void> status(@RequestBody StatusParam param) {
		assertTargetOperable(param.id(), false);
		SysUser user = new SysUser();
		user.setId(param.id());
		user.setStatus(param.status());
		userMapper.update(user);
		if (param.status() != null && param.status() == 0) {
			StpUtil.kickout(param.id());
		}
		return R.success("操作成功");
	}

	/** 查询用户已授权角色 id 集合（授权回显） */
	@GetMapping("/role-ids")
	@SaCheckPermission("sys:user:grant")
	public R<List<Long>> roleIds(@RequestParam Long userId) {
		assertUserInScope(userId);
		List<Long> ids = userRoleMapper
			.selectListByQuery(QueryWrapper.create().eq("user_id", userId))
			.stream()
			.map(SysUserRole::getRoleId)
			.toList();
		return R.data(ids);
	}

	/** 用户授权角色（事务原子；用户与角色均须属当前租户——中间表 sys_user_role 无 tenant_id，必须先证归属） */
	@PostMapping("/grant")
	@SaCheckPermission("sys:user:grant")
	@OperationLog("用户授权")
	@org.springframework.transaction.annotation.Transactional(rollbackFor = Exception.class)
	public R<Void> grant(@RequestBody UserGrantParam param) {
		assertUserInScope(param.userId());
		if (param.roleIds() != null) {
			for (Long roleId : param.roleIds()) {
				com.mugsun.boot.system.entity.SysRole role = roleMapper.selectOneById(roleId);
				if (role == null) {
					throw new com.mugsun.core.tool.exception.ServiceException("角色不存在：" + roleId);
				}
				// 内置 admin 角色仅平台超管或同为 admin 角色持有者可授（防低权角色自助升格）
				if (com.mugsun.boot.common.constant.RoleConstants.ADMIN.equals(role.getRoleCode())
					&& !com.mugsun.boot.tenant.TenantContext.isPlatformSuperAdmin()
					&& !StpUtil.getRoleList().contains(com.mugsun.boot.common.constant.RoleConstants.ADMIN)) {
					throw new com.mugsun.core.tool.exception.ServiceException("内置管理员角色仅管理员可授予");
				}
			}
		}
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

	/** 用户归属校验：Flex 租户条件天然挡跨租户直查，null 即越界或不存在 */
	private void assertUserInScope(Long userId) {
		if (userId == null || userMapper.selectOneById(userId) == null) {
			throw new com.mugsun.core.tool.exception.ServiceException("用户不存在");
		}
	}

	/**
	 * 目标用户操作保护（对齐 RuoYi checkUserAllowed）：
	 * 内置 admin 角色持有者仅平台超管可操作（防租户内改密/停用/删除管理员致账号接管）；
	 * allowSelf=false 时禁止操作自己（防自杀/自锁）。
	 */
	private void assertTargetOperable(Long userId, boolean allowSelf) {
		assertUserInScope(userId);
		if (!allowSelf && StpUtil.getLoginIdAsLong() == userId) {
			throw new com.mugsun.core.tool.exception.ServiceException("不能对当前登录账号执行该操作");
		}
		if (!com.mugsun.boot.tenant.TenantContext.isPlatformSuperAdmin() && holdsAdminRole(userId)) {
			throw new com.mugsun.core.tool.exception.ServiceException("无权操作内置管理员账号");
		}
	}

	/** 该用户是否持有内置 admin 角色（中间表无 tenant_id，角色 id 全局唯一，经角色表反查角色码） */
	private boolean holdsAdminRole(Long userId) {
		List<Long> roleIds = userRoleMapper.selectListByQuery(QueryWrapper.create().eq("user_id", userId))
			.stream().map(SysUserRole::getRoleId).toList();
		if (roleIds.isEmpty()) {
			return false;
		}
		return roleMapper.selectListByIds(roleIds).stream()
			.anyMatch(r -> com.mugsun.boot.common.constant.RoleConstants.ADMIN.equals(r.getRoleCode()));
	}
}
