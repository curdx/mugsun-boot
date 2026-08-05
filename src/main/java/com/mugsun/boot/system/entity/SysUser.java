package com.mugsun.boot.system.entity;

import com.mugsun.boot.common.constant.FieldMaskConstants;
import com.mugsun.boot.common.crypto.Sm4TypeHandler;
import com.mugsun.boot.log.AuditField;
import com.mugsun.core.mybatis.base.BaseEntity;
import com.mybatisflex.annotation.Column;
import com.mybatisflex.annotation.ColumnMask;
import com.mybatisflex.annotation.Table;

/**
 * 系统用户
 */
@Table("sys_user")
public class SysUser extends BaseEntity {

	private String username;
	private String password;
	@AuditField("昵称")
	private String nickname;
	@AuditField(value = "状态", dict = "user_status")
	private Integer status;
	private Long deptId;
	private Long postId;
	private String tenantId;
	/** 邮箱：通知邮件渠道联系方式 */
	private String email;
	/** 手机号：明文存储，按角色决策 明文/脱敏/不可见（@ColumnMask 自定义类型 + RoleAwareMaskProcessor） */
	@AuditField("手机号")
	@ColumnMask(FieldMaskConstants.TYPE_USER_PHONE)
	private String phone;
	/** 身份证号：SM4 加密存储（TypeHandler 入库加密、查询解密）+ 按角色决策 明文/脱敏/不可见（解密后叠加脱敏决策层） */
	@AuditField("身份证")
	@Column(typeHandler = Sm4TypeHandler.class)
	@ColumnMask(FieldMaskConstants.TYPE_USER_ID_CARD)
	private String idCard;

	/** 部门名（展示用，非表列，page/detail 富化填充） */
	@Column(ignore = true)
	private String deptName;
	/** 岗位名（展示用，非表列） */
	@Column(ignore = true)
	private String postName;
	/** 角色名串（展示用，非表列，顿号分隔） */
	@Column(ignore = true)
	private String roleNames;
	/** 角色 id 集合（建档/编辑挂角色入参 + detail 回显，非表列） */
	@Column(ignore = true)
	private java.util.List<Long> roleIds;

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

	public Long getDeptId() {
		return deptId;
	}

	public void setDeptId(Long deptId) {
		this.deptId = deptId;
	}

	public Long getPostId() {
		return postId;
	}

	public void setPostId(Long postId) {
		this.postId = postId;
	}

	public String getTenantId() {
		return tenantId;
	}

	public void setTenantId(String tenantId) {
		this.tenantId = tenantId;
	}

	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
	}

	public String getPhone() {
		return phone;
	}

	public void setPhone(String phone) {
		this.phone = phone;
	}

	public String getIdCard() {
		return idCard;
	}

	public void setIdCard(String idCard) {
		this.idCard = idCard;
	}

	public String getDeptName() {
		return deptName;
	}

	public void setDeptName(String deptName) {
		this.deptName = deptName;
	}

	public String getPostName() {
		return postName;
	}

	public void setPostName(String postName) {
		this.postName = postName;
	}

	public String getRoleNames() {
		return roleNames;
	}

	public void setRoleNames(String roleNames) {
		this.roleNames = roleNames;
	}

	public java.util.List<Long> getRoleIds() {
		return roleIds;
	}

	public void setRoleIds(java.util.List<Long> roleIds) {
		this.roleIds = roleIds;
	}
}
