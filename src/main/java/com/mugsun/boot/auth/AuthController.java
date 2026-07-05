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

	public AuthController(SysUserMapper userMapper, PasswordEncoder passwordEncoder,
						  LoginLockService loginLockService, SysLoginLogMapper loginLogMapper) {
		this.userMapper = userMapper;
		this.passwordEncoder = passwordEncoder;
		this.loginLockService = loginLockService;
		this.loginLogMapper = loginLogMapper;
	}

	@PostMapping("/login")
	public R<Map<String, Object>> login(@RequestBody LoginDTO dto, HttpServletRequest request) {
		String username = dto.getUsername();
		String ip = request.getRemoteAddr();
		if (username == null || username.isBlank()) {
			throw new ServiceException("账号或密码错误");
		}
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
		StpUtil.login(user.getId());
		StpUtil.getSession().set("tenantId", user.getTenantId());
		saveLoginLog(username, ip, 1, "登录成功");
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
			"buttons", List.of("*")
		));
	}
}
