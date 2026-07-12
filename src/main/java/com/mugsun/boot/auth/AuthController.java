package com.mugsun.boot.auth;

import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.util.IdUtil;
import com.mugsun.boot.common.constant.ClientConstants;
import com.mugsun.boot.common.constant.RoleConstants;
import com.mugsun.boot.common.constant.TenantConstants;
import com.mugsun.boot.system.entity.SysLoginLog;
import com.mugsun.boot.system.entity.SysUser;
import com.mugsun.boot.system.mapper.SysLoginLogMapper;
import com.mugsun.boot.system.mapper.SysUserMapper;
import com.mugsun.core.tool.api.R;
import com.mugsun.core.tool.exception.ServiceException;
import com.mybatisflex.core.query.QueryWrapper;
import com.mugsun.boot.tenant.TenantContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 认证：登录 / 登出 / 当前用户。登录含失败锁定与登录日志留痕。
 */
@RestController
@RequestMapping("/auth")
public class AuthController {

	private final SysUserMapper userMapper;
	private final PasswordEncoder passwordEncoder;
	private final LoginLockService loginLockService;
	private final SysLoginLogMapper loginLogMapper;
	private final CaptchaService captchaService;
	private final com.mugsun.boot.security.SecurityPolicyService securityPolicyService;
	private final TwoFactorService twoFactorService;
	private final com.mugsun.boot.system.service.SmsService smsService;
	private final com.mugsun.boot.system.service.SocialService socialService;
	private final com.mugsun.boot.system.mapper.SysTenantMapper tenantMapper;
	private final com.mugsun.boot.tenant.mapper.SysTenantPackageMapper tenantPackageMapper;
	private final com.mugsun.boot.client.ClientService clientService;
	private final com.mugsun.boot.tenant.TenantValidator tenantValidator;
	private final com.mugsun.boot.common.crypto.GmCryptoConfig gmCryptoConfig;

	public AuthController(SysUserMapper userMapper, PasswordEncoder passwordEncoder,
						  LoginLockService loginLockService, SysLoginLogMapper loginLogMapper,
						  CaptchaService captchaService,
						  com.mugsun.boot.security.SecurityPolicyService securityPolicyService,
						  TwoFactorService twoFactorService,
						  com.mugsun.boot.system.service.SmsService smsService,
						  com.mugsun.boot.system.service.SocialService socialService,
						  com.mugsun.boot.system.mapper.SysTenantMapper tenantMapper,
						  com.mugsun.boot.tenant.mapper.SysTenantPackageMapper tenantPackageMapper,
						  com.mugsun.boot.client.ClientService clientService,
						  com.mugsun.boot.tenant.TenantValidator tenantValidator,
						  com.mugsun.boot.common.crypto.GmCryptoConfig gmCryptoConfig) {
		this.userMapper = userMapper;
		this.passwordEncoder = passwordEncoder;
		this.loginLockService = loginLockService;
		this.loginLogMapper = loginLogMapper;
		this.captchaService = captchaService;
		this.securityPolicyService = securityPolicyService;
		this.twoFactorService = twoFactorService;
		this.smsService = smsService;
		this.socialService = socialService;
		this.tenantMapper = tenantMapper;
		this.tenantPackageMapper = tenantPackageMapper;
		this.clientService = clientService;
		this.tenantValidator = tenantValidator;
		this.gmCryptoConfig = gmCryptoConfig;
	}

	/** SM2 传输公钥：前端登录/改密/注册前取此公钥加密密码；gmEnabled=false 时前端明文传输 */
	@GetMapping("/sm2-public-key")
	public R<Map<String, Object>> sm2PublicKey() {
		Map<String, Object> data = new java.util.HashMap<>();
		data.put("gmEnabled", gmCryptoConfig.isGmEnabled());
		data.put("publicKey", gmCryptoConfig.isGmEnabled()
			? com.mugsun.boot.common.crypto.Sm2Util.publicKey() : null);
		return R.data(data);
	}

	/** 传输密码解密：国密开关开启时按 SM2 解密（前端 sm-crypto 公钥加密），否则原样明文 */
	private String decodePassword(String raw) {
		if (raw == null || raw.isBlank() || !gmCryptoConfig.isGmEnabled()) {
			return raw;
		}
		return com.mugsun.boot.common.crypto.Sm2Util.decrypt(raw);
	}

	/** 图形验证码：生成一张，答案入 Redis */
	@GetMapping("/captcha")
	public R<CaptchaVO> captcha() {
		return R.data(captchaService.generate());
	}

	@PostMapping("/login")
	public R<Map<String, Object>> login(@RequestBody LoginDTO dto, HttpServletRequest request) {
		String username = dto.getUsername();
		String ip = request.getRemoteAddr();
		if (username == null || username.isBlank()) {
			throw new ServiceException("账号或密码错误");
		}
		com.mugsun.boot.client.entity.SysClient client = clientService.loadClient(dto.getClientId());
		// 按 client 策略：图形验证码开关
		if (Integer.valueOf(1).equals(client.getCaptchaEnabled())) {
			captchaService.verify(dto.getCaptchaUuid(), dto.getCaptchaCode());
		}
		loginLockService.assertNotLocked(username);
		String tenantId = (dto.getTenantId() == null || dto.getTenantId().isBlank()) ? TenantConstants.DEFAULT_TENANT_ID : dto.getTenantId();
		// 登录层租户生命周期校验：停用/过期/不存在的租户禁止登录（平台租户 000000 短路放行）
		String tenantInvalid = tenantValidator.validate(tenantId);
		if (tenantInvalid != null) {
			saveLoginLog(username, request, client.getClientId(), tenantId, 0, tenantInvalid);
			throw new ServiceException(tenantInvalid);
		}
		SysUser user = TenantContext.ignore(() ->
			userMapper.selectOneByQuery(QueryWrapper.create().eq("tenant_id", tenantId).eq("username", username)));
		String rawPassword;
		try {
			rawPassword = decodePassword(dto.getPassword());
		} catch (Exception e) {
			// SM2 解密失败与密码错误归一，避免暴露「密文格式/是否加密」的探测预言机
			rawPassword = null;
		}
		if (user == null || rawPassword == null || !passwordEncoder.matches(rawPassword, user.getPassword())) {
			loginLockService.recordFail(username);
			saveLoginLog(username, request, client.getClientId(), tenantId, 0, "账号或密码错误");
			throw new ServiceException("账号或密码错误");
		}
		loginLockService.clear(username);
		// 双因子登录（默认关闭）：密码通过后下发二次验证码，暂不发 token
		if (twoFactorService.isEnabled()) {
			String[] challenge = twoFactorService.challenge(user.getId(), null);
			saveLoginLog(username, request, client.getClientId(), user.getTenantId(), 1, "登录待二次验证");
			java.util.Map<String, Object> resp = new java.util.HashMap<>();
			resp.put("twoFactorRequired", true);
			resp.put("twoFactorToken", challenge[0]);
			if (challenge[1] != null) {
				resp.put("twoFactorCode", challenge[1]);
			}
			return R.data(resp);
		}
		// 按 client 策略：令牌有效期 + 设备类型
		StpUtil.login(user.getId(), new cn.dev33.satoken.stp.parameter.SaLoginParameter()
			.setDeviceType(client.getClientId())
			.setTimeout(client.getTokenTimeout() == null ? 2592000 : client.getTokenTimeout()));
		StpUtil.getSession().set(TenantContext.TENANT_SESSION_KEY, user.getTenantId());
		// 按 client 策略：单账号最大在线终端数（超出踢最旧）
		enforceMaxOnline(user.getId(), client.getMaxOnline());
		saveLoginLog(username, request, client.getClientId(), user.getTenantId(), 1, "登录成功");
		return R.data(Map.of("token", StpUtil.getTokenValue()));
	}

	/** 按 client 策略限制单账号最大在线终端数：超出则踢最旧的会话 */
	private void enforceMaxOnline(long userId, Integer maxOnline) {
		if (maxOnline == null || maxOnline <= 0) {
			return;
		}
		java.util.List<String> tokens = StpUtil.getTokenValueListByLoginId(userId);
		int excess = tokens.size() - maxOnline;
		for (int i = 0; i < excess; i++) {
			StpUtil.kickoutByTokenValue(tokens.get(i));
		}
	}

	/** 双因子二次校验：验证码正确才发 token */
	@PostMapping("/two-factor")
	public R<Map<String, Object>> twoFactor(@RequestBody Map<String, String> body) {
		Long userId = twoFactorService.verify(body.get("twoFactorToken"), body.get("code"));
		SysUser user = TenantContext.ignore(() -> userMapper.selectOneById(userId));
		StpUtil.login(userId);
		StpUtil.getSession().set(TenantContext.TENANT_SESSION_KEY, user.getTenantId());
		return R.data(Map.of("token", StpUtil.getTokenValue()));
	}

	/** 自助注册：用户名唯一 + BCrypt + 默认租户，建号后可登录 */
	@PostMapping("/register")
	public R<Void> register(@RequestBody RegisterDTO dto) {
		if (dto.getUsername() == null || dto.getUsername().isBlank()
			|| dto.getPassword() == null || dto.getPassword().isBlank()) {
			throw new ServiceException("用户名和密码不能为空");
		}
		// 传输密码 SM2 解密（国密开关开启时），后续校验/落库用明文
		String rawPassword = decodePassword(dto.getPassword());
		// 等保密码复杂度校验（min-length + 大小写/数字/特殊字符组合，策略可后台改）
		securityPolicyService.validateComplexity(rawPassword);
		String tenantId = TenantConstants.DEFAULT_TENANT_ID;
		long exists = TenantContext.ignore(() -> userMapper.selectCountByQuery(
			QueryWrapper.create().eq("tenant_id", tenantId).eq("username", dto.getUsername())));
		if (exists > 0) {
			throw new ServiceException("用户名已存在");
		}
		SysUser user = new SysUser();
		user.setUsername(dto.getUsername());
		user.setNickname((dto.getNickname() == null || dto.getNickname().isBlank()) ? dto.getUsername() : dto.getNickname());
		user.setPassword(passwordEncoder.encode(rawPassword));
		user.setPhone(dto.getPhone());
		user.setStatus(1);
		user.setTenantId(tenantId);
		TenantContext.ignore(() -> userMapper.insert(user));
		securityPolicyService.logPassword(user.getId(), user.getPassword());
		return R.success("注册成功");
	}

	/** 短信登录：发送验证码（开发回显） */
	@PostMapping("/sms-code")
	public R<Map<String, Object>> smsCode(@RequestBody Map<String, String> body) {
		String phone = body.get("phone");
		if (phone == null || phone.isBlank()) {
			throw new ServiceException("手机号不能为空");
		}
		smsService.sendCode(phone);
		Map<String, Object> resp = new java.util.HashMap<>();
		if (smsService.isShowCode()) {
			resp.put("code", smsService.peekCode(phone));
		}
		return R.data(resp);
	}

	/** 短信登录：校验验证码 + 按手机号查用户 + 发 token */
	@PostMapping("/sms-login")
	public R<Map<String, Object>> smsLogin(@RequestBody Map<String, String> body, HttpServletRequest request) {
		String phone = body.get("phone");
		String code = body.get("code");
		if (phone == null || phone.isBlank()) {
			throw new ServiceException("手机号不能为空");
		}
		// 图形验证码前置（等保：短信登录也需图形码，防短信轰炸/枚举）
		captchaService.verify(body.get("captchaUuid"), body.get("captchaCode"));
		// 纳入失败锁定（按手机号维度）
		loginLockService.assertNotLocked(phone);
		if (!smsService.verifyCode(phone, code)) {
			loginLockService.recordFail(phone);
			saveLoginLog(phone, request, ClientConstants.DEFAULT_CLIENT_ID, TenantConstants.DEFAULT_TENANT_ID, 0, "短信验证码错误");
			throw new ServiceException("验证码错误或已过期");
		}
		SysUser user = TenantContext.ignore(() ->
			userMapper.selectOneByQuery(QueryWrapper.create().eq("phone", phone).orderBy("id", false)));
		if (user == null) {
			loginLockService.recordFail(phone);
			saveLoginLog(phone, request, ClientConstants.DEFAULT_CLIENT_ID, TenantConstants.DEFAULT_TENANT_ID, 0, "手机号未注册");
			throw new ServiceException("该手机号未注册");
		}
		// 登录层租户生命周期校验：停用/过期的租户禁止短信登录
		String smsTenantInvalid = tenantValidator.validate(user.getTenantId());
		if (smsTenantInvalid != null) {
			saveLoginLog(user.getUsername(), request, ClientConstants.DEFAULT_CLIENT_ID, user.getTenantId(), 0, smsTenantInvalid);
			throw new ServiceException(smsTenantInvalid);
		}
		loginLockService.clear(phone);
		StpUtil.login(user.getId());
		StpUtil.getSession().set(TenantContext.TENANT_SESSION_KEY, user.getTenantId());
		saveLoginLog(user.getUsername(), request, ClientConstants.DEFAULT_CLIENT_ID, user.getTenantId(), 1, "短信登录成功");
		return R.data(Map.of("token", StpUtil.getTokenValue()));
	}

	/** 社交登录入口：生成第三方授权跳转地址（state 落 Redis 防 CSRF），前端跳转此地址 */
	@GetMapping("/social/render/{source}")
	public R<Map<String, Object>> socialRender(@PathVariable String source) {
		return R.data(Map.of("authorizeUrl", socialService.renderAuthUrl(source)));
	}

	/** 本地 mock 授权页：模拟第三方放行，生成 code 回跳前端回调地址（dev 联调，无真实凭证时验证全链路） */
	@GetMapping("/social/mock-authorize")
	public void socialMockAuthorize(@RequestParam("redirect_uri") String redirectUri,
									@RequestParam String state, HttpServletResponse response) throws IOException {
		String code = "mockcode-" + IdUtil.fastSimpleUUID();
		String sep = redirectUri.contains("?") ? "&" : "?";
		response.sendRedirect(redirectUri + sep + "code=" + code + "&state=" + state);
	}

	/** 社交登录：只收 source+code+state，openId 由服务端凭 code 换取（杜绝越权）。clientType=app 走 C 端自动注册，默认管理端禁自动开户 */
	@PostMapping("/social/login")
	public R<Map<String, Object>> socialLogin(@RequestBody Map<String, String> body) {
		String source = body.get("source");
		String code = body.get("code");
		String state = body.get("state");
		if (source == null || source.isBlank() || code == null || code.isBlank()) {
			throw new ServiceException("社交来源与授权码不能为空");
		}
		boolean autoRegister = "app".equalsIgnoreCase(body.get("clientType"));
		String token = socialService.loginByCode(source, code, state, autoRegister);
		return R.data(Map.of("token", token));
	}

	/** 当前登录用户绑定社交账号：同样只收 code+state，服务端换 openId */
	@PostMapping("/social/bind")
	@SaCheckLogin
	public R<Void> socialBind(@RequestBody Map<String, String> body) {
		String source = body.get("source");
		String code = body.get("code");
		String state = body.get("state");
		if (source == null || source.isBlank() || code == null || code.isBlank()) {
			throw new ServiceException("社交来源与授权码不能为空");
		}
		socialService.bindByCode(StpUtil.getLoginIdAsLong(), source, code, state);
		return R.success("绑定成功");
	}

	/** 当前登录用户解绑某来源社交账号 */
	@DeleteMapping("/social/unbind/{source}")
	@SaCheckLogin
	public R<Void> socialUnbind(@PathVariable String source) {
		socialService.unbind(StpUtil.getLoginIdAsLong(), source);
		return R.success("解绑成功");
	}

	/** 登录日志留痕（平台级，登录前无租户上下文）：含租户/UA/设备增强字段 */
	private void saveLoginLog(String username, HttpServletRequest request, String device, String tenantId,
							 int status, String msg) {
		SysLoginLog log = new SysLoginLog();
		log.setUsername(username);
		log.setIp(request.getRemoteAddr());
		String ua = request.getHeader("User-Agent");
		log.setUserAgent(ua != null && ua.length() > 500 ? ua.substring(0, 500) : ua);
		log.setDevice(device);
		log.setTenantId(tenantId);
		log.setStatus(status);
		log.setMsg(msg);
		log.setLoginTime(LocalDateTime.now());
		TenantContext.ignore(() -> loginLogMapper.insertSelective(log));
	}

	@PostMapping("/logout")
	public R<Void> logout() {
		StpUtil.logout();
		return R.success("已登出");
	}

	/** 超管切换查看租户：落 Sa-Token 会话，跨请求生效（含异步/定时透传），登出即随会话销毁复位。-1 查看全部 */
	@PostMapping("/switch-tenant/{tenantId}")
	@SaCheckLogin
	public R<Void> switchTenant(@PathVariable String tenantId) {
		// 仅平台超管（平台租户 000000 + admin 角色）可切换租户视图，杜绝自助注册用户凭 000000 会话跨租户逃逸
		if (!TenantContext.isPlatformSuperAdmin()) {
			throw new ServiceException("仅平台超管可切换租户视图");
		}
		StpUtil.getSession().set(TenantContext.SWITCH_KEY, tenantId);
		return R.success("已切换租户视图");
	}

	/** 复位租户切换，回本租户视图 */
	@PostMapping("/switch-tenant/reset")
	@SaCheckLogin
	public R<Void> switchTenantReset() {
		StpUtil.getSession().delete(TenantContext.SWITCH_KEY);
		return R.success("已复位租户视图");
	}

	@GetMapping("/info")
	@SaCheckLogin
	public R<Map<String, Object>> info() {
		SysUser user = TenantContext.ignore(() -> userMapper.selectOneById(StpUtil.getLoginIdAsLong()));
		Map<String, Object> data = new java.util.HashMap<>();
		data.put("userId", user.getId());
		data.put("userName", user.getUsername());
		data.put("nickName", user.getNickname() == null ? user.getUsername() : user.getNickname());
		// 真实角色/权限下发（经 MugsunStpInterface 从 用户→角色→菜单权限 派生）
		java.util.List<String> realRoles = StpUtil.getRoleList();
		java.util.LinkedHashSet<String> roles = new java.util.LinkedHashSet<>(realRoles);
		// 前端菜单门控伪角色按真实身份派生（不再对所有人无条件注入，杜绝普通用户菜单越权可见）：
		// 平台超管(000000+admin)→R_SUPER+R_ADMIN；租户管理员(admin 角色)→R_ADMIN；普通用户→均无（仅见未门控页）
		if (TenantContext.isPlatformSuperAdmin()) {
			roles.add(RoleConstants.FRONT_SUPER);
			roles.add(RoleConstants.FRONT_ADMIN);
		} else if (realRoles.contains(RoleConstants.ADMIN)) {
			roles.add(RoleConstants.FRONT_ADMIN);
		}
		data.put("roles", new java.util.ArrayList<>(roles));
		data.put("buttons", StpUtil.getPermissionList());
		data.put("needChangePassword", securityPolicyService.needChangePassword(user.getId()));
		data.put("watermark", securityPolicyService.isWatermarkEnabled());
		// 租户套餐：非超管租户按套餐限定可用菜单（null 表示不限）
		data.put("menus", resolveTenantMenus(user.getTenantId()));
		return R.data(data);
	}

	/** 解析当前租户套餐允许的菜单标识；超管租户或未绑套餐返回 null（不限功能） */
	private List<String> resolveTenantMenus(String tenantId) {
		if (tenantId == null || TenantConstants.DEFAULT_TENANT_ID.equals(tenantId)) {
			return null;
		}
		com.mugsun.boot.system.entity.SysTenant tenant = TenantContext.ignore(() ->
			tenantMapper.selectOneByQuery(QueryWrapper.create().eq("tenant_code", tenantId)));
		if (tenant == null || tenant.getPackageId() == null) {
			return null;
		}
		com.mugsun.boot.tenant.entity.SysTenantPackage pkg = tenantPackageMapper.selectOneById(tenant.getPackageId());
		if (pkg == null || pkg.getMenuKeys() == null || pkg.getMenuKeys().isBlank()) {
			return null;
		}
		return java.util.Arrays.stream(pkg.getMenuKeys().split(","))
			.map(String::trim).filter(s -> !s.isEmpty()).toList();
	}

	/** 个人中心：修改昵称 */
	@PostMapping("/update-info")
	@SaCheckLogin
	public R<Void> updateInfo(@RequestBody UpdateInfoDTO dto) {
		SysUser user = TenantContext.ignore(() -> userMapper.selectOneById(StpUtil.getLoginIdAsLong()));
		if (user == null) {
			throw new ServiceException("用户不存在");
		}
		if (dto.nickname() != null && !dto.nickname().isBlank()) {
			user.setNickname(dto.nickname());
		}
		userMapper.update(user);
		return R.success("修改成功");
	}

	/** 个人中心：修改密码（校验原密码） */
	@PostMapping("/update-password")
	@SaCheckLogin
	public R<Void> updatePassword(@RequestBody UpdatePasswordDTO dto) {
		SysUser user = TenantContext.ignore(() -> userMapper.selectOneById(StpUtil.getLoginIdAsLong()));
		// 传输密码 SM2 解密（国密开关开启时）
		String oldPassword = decodePassword(dto.oldPassword());
		String newPassword = decodePassword(dto.newPassword());
		if (user == null || !passwordEncoder.matches(oldPassword, user.getPassword())) {
			throw new ServiceException("原密码错误");
		}
		// 等保：复杂度校验 + 历史密码防重
		securityPolicyService.validateComplexity(newPassword);
		securityPolicyService.checkHistory(user.getId(), newPassword);
		String encoded = passwordEncoder.encode(newPassword);
		user.setPassword(encoded);
		userMapper.update(user);
		securityPolicyService.logPassword(user.getId(), encoded);
		return R.success("密码修改成功");
	}

	/** 改昵称参数 */
	public record UpdateInfoDTO(String nickname) {
	}

	/** 改密参数 */
	public record UpdatePasswordDTO(String oldPassword, String newPassword) {
	}
}
