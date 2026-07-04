package com.mugsun.boot.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.dev33.satoken.stp.StpUtil;
import com.mugsun.core.tool.api.R;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * G2 鉴权验证接口（G3 接入真实登录后，mock-login 由认证域替代）
 */
@RestController
public class SecureController {

	@GetMapping("/mock-login")
	public R<String> mockLogin() {
		StpUtil.login(1L);
		return R.data(StpUtil.getTokenValue());
	}

	@GetMapping("/secure")
	@SaCheckLogin
	public R<String> secure() {
		return R.data("已登录，loginId=" + StpUtil.getLoginId());
	}

	@GetMapping("/admin-only")
	@SaCheckPermission("admin")
	public R<String> adminOnly() {
		return R.data("admin 权限校验通过");
	}
}
