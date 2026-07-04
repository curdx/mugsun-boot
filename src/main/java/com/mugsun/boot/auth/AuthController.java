package com.mugsun.boot.auth;

import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.stp.StpUtil;
import com.mugsun.boot.system.entity.SysUser;
import com.mugsun.boot.system.mapper.SysUserMapper;
import com.mugsun.core.tool.api.R;
import com.mugsun.core.tool.exception.ServiceException;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.core.tenant.TenantManager;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 认证：登录 / 登出 / 当前用户
 */
@RestController
@RequestMapping("/auth")
public class AuthController {

	private final SysUserMapper userMapper;
	private final PasswordEncoder passwordEncoder;

	public AuthController(SysUserMapper userMapper, PasswordEncoder passwordEncoder) {
		this.userMapper = userMapper;
		this.passwordEncoder = passwordEncoder;
	}

	@PostMapping("/login")
	public R<String> login(@RequestParam(defaultValue = "000000") String tenantId,
						   @RequestParam String username, @RequestParam String password) {
		SysUser user = TenantManager.withoutTenantCondition(() ->
			userMapper.selectOneByQuery(QueryWrapper.create().eq("tenant_id", tenantId).eq("username", username)));
		if (user == null || !passwordEncoder.matches(password, user.getPassword())) {
			throw new ServiceException("账号或密码错误");
		}
		StpUtil.login(user.getId());
		StpUtil.getSession().set("tenantId", user.getTenantId());
		return R.data(StpUtil.getTokenValue());
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
			"id", user.getId(),
			"username", user.getUsername(),
			"nickname", user.getNickname()
		));
	}
}
