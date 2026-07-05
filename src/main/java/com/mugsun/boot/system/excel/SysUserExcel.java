package com.mugsun.boot.system.excel;

import cn.idev.excel.annotation.ExcelProperty;

/**
 * 用户导入导出模型：仅暴露非敏感列，密码等不参与 Excel。
 */
public class SysUserExcel {

	@ExcelProperty("用户名")
	private String username;

	@ExcelProperty("昵称")
	private String nickname;

	@ExcelProperty("状态(1启用/0禁用)")
	private Integer status;

	public String getUsername() {
		return username;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	public String getNickname() {
		return nickname;
	}

	public void setNickname(String nickname) {
		this.nickname = nickname;
	}

	public Integer getStatus() {
		return status;
	}

	public void setStatus(Integer status) {
		this.status = status;
	}
}
