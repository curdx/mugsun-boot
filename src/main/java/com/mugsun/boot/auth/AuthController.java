package com.mugsun.boot.auth;

import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.stp.StpUtil;
import com.mugsun.boot.system.entity.SysLoginLog;
import com.mugsun.boot.system.entity.SysUser;
import com.mugsun.boot.system.mapper.SysLoginLogMapper;
import com.mugsun.boot.system.mapper.SysUserMapper;
import com.mugsun.core.tool.api.R;
import com.mugsun.core.tool.exception.ServiceException;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.core.tenant.TenantManager;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

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

	public AuthController(SysUserMapper userMapper, PasswordEncoder passwordEncoder,
						  LoginLockService loginLockService, SysLoginLogMapper loginLogMapper,
						  CaptchaService captchaService,
						  com.mugsun.boot.security.SecurityPolicyService securityPolicyService,
						  TwoFactorService twoFactorService) {
		this.userMapper = userMapper;
		this.passwordEncoder = passwordEncoder;
		this.loginLockService = loginLockService;
		this.loginLogMapper = loginLogMapper;
		this.captchaService = captchaService;
		this.securityPolicyService = securityPolicyService;
		this.twoFactorService = twoFactorService;
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
		captchaService.verify(dto.getCaptchaUuid(), dto.getCaptchaCode());
		loginLockService.assertNotLocked(username);
		String tenantId = (dto.getTenantId() == null || dto.getTenantId().isBlank()) ? "000000" : dto.getTenantId();
		SysUser user = TenantManager.withoutTenantCondition(() ->
			userMapper.selectOneByQuery(QueryWrapper.create().eq("tenant_id", tenantId).eq("username", username)));
		if (user == null || !passwordEncoder.matches(dto.getPassword(), user.getPassword())) {
			loginLockService.recordFail(username);
			saveLoginLog(username, ip, 0, "账号或密码错误");
			throw new ServiceException("账号或密码错误");
		}
		loginLockService.clear(username);
		// 双因子登录（默认关闭）：密码通过后下发二次验证码，暂不发 token
		if (twoFactorService.isEnabled()) {
			String[] challenge = twoFactorService.challenge(user.getId(), null);
			saveLoginLog(username, ip, 1, "登录待二次验证");
			java.util.Map<String, Object> resp = new java.util.HashMap<>();
			resp.put("twoFactorRequired", true);
			resp.put("twoFactorToken", challenge[0]);
			if (challenge[1] != null) {
				resp.put("twoFactorCode", challenge[1]);
			}
			return R.data(resp);
		}
		StpUtil.login(user.getId());
		StpUtil.getSession().set("tenantId", user.getTenantId());
		saveLoginLog(username, ip, 1, "登录成功");
		return R.data(Map.of("token", StpUtil.getTokenValue()));
	}

	/** 双因子二次校验：验证码正确才发 token */
	@PostMapping("/two-factor")
	public R<Map<String, Object>> twoFactor(@RequestBody Map<String, String> body) {
		Long userId = twoFactorService.verify(body.get("twoFactorToken"), body.get("code"));
		SysUser user = userMapper.selectOneById(userId);
		StpUtil.login(userId);
		StpUtil.getSession().set("tenantId", user.getTenantId());
		return R.data(Map.of("token", StpUtil.getTokenValue()));
	}

	/** 登录日志留痕（平台级，登录前无租户上下文） */
	private void saveLoginLog(String username, String ip, int status, String msg) {
		SysLoginLog log = new SysLoginLog();
		log.setUsername(username);
		log.setIp(ip);
		log.setStatus(status);
		log.setMsg(msg);
		log.setLoginTime(LocalDateTime.now());
		TenantManager.withoutTenantCondition(() -> loginLogMapper.insertSelective(log));
	}

	@PostMapping("/logout")
	public R<Void> logout() {
		StpUtil.logout();
		return R.success("已登出");
	}

	@GetMapping("/info")
	@SaCheckLogin
	public R<Map<String, Object>> info() {
		SysUser user = userMapper.selectOneById(StpUtil.getLoginIdAsLong());
		return R.data(Map.of(
			"userId", user.getId(),
			"userName", user.getUsername(),
			"nickName", user.getNickname() == null ? user.getUsername() : user.getNickname(),
			"roles", List.of("R_SUPER"),
			"buttons", List.of("*"),
			"needChangePassword", securityPolicyService.needChangePassword(user.getId()),
			"watermark", securityPolicyService.isWatermarkEnabled()
		));
	}

	/** 个人中心：修改昵称 */
	@PostMapping("/update-info")
	@SaCheckLogin
	public R<Void> updateInfo(@RequestBody UpdateInfoDTO dto) {
		SysUser user = userMapper.selectOneById(StpUtil.getLoginIdAsLong());
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
		SysUser user = userMapper.selectOneById(StpUtil.getLoginIdAsLong());
		if (user == null || !passwordEncoder.matches(dto.oldPassword(), user.getPassword())) {
			throw new ServiceException("原密码错误");
		}
		// 等保：复杂度校验 + 历史密码防重
		securityPolicyService.validateComplexity(dto.newPassword());
		securityPolicyService.checkHistory(user.getId(), dto.newPassword());
		String encoded = passwordEncoder.encode(dto.newPassword());
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
