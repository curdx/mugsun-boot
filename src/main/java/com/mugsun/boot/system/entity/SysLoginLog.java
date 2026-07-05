package com.mugsun.boot.system.entity;

import com.mugsun.core.mybatis.base.BaseEntity;
import com.mybatisflex.annotation.Table;

import java.time.LocalDateTime;

/**
 * 登录日志
 */
@Table("sys_login_log")
public class SysLoginLog extends BaseEntity {

	private String username;
	private String ip;
	private Integer status;
	private String msg;
	private LocalDateTime loginTime;

	public String getUsername() {
		return username;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	public String getIp() {
		return ip;
	}

	public void setIp(String ip) {
		this.ip = ip;
	}

	public Integer getStatus() {
		return status;
	}

	public void setStatus(Integer status) {
		this.status = status;
	}

	public String getMsg() {
		return msg;
	}

	public void setMsg(String msg) {
		this.msg = msg;
	}

	public LocalDateTime getLoginTime() {
		return loginTime;
	}

	public void setLoginTime(LocalDateTime loginTime) {
		this.loginTime = loginTime;
	}
}
