package com.mugsun.boot.auth;

/**
 * 登录参数
 */
public class LoginDTO {

	private String tenantId;
	private String username;
	private String password;
	private String captchaUuid;
	private String captchaCode;

	public String getTenantId() {
		return tenantId;
	}

	public void setTenantId(String tenantId) {
		this.tenantId = tenantId;
	}

	public String getUsername() {
		return username;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public String getCaptchaUuid() {
		return captchaUuid;
	}

	public void setCaptchaUuid(String captchaUuid) {
		this.captchaUuid = captchaUuid;
	}

	public String getCaptchaCode() {
		return captchaCode;
	}

	public void setCaptchaCode(String captchaCode) {
		this.captchaCode = captchaCode;
	}
}
