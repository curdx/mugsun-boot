package com.mugsun.boot.datasource.entity;

import com.mugsun.core.mybatis.base.BaseEntity;
import com.mybatisflex.annotation.Table;

/**
 * 租户独立数据源配置（平台级，无 tenant_id 列不参与租户隔离）。
 * 配置后运行时注册进 FlexDataSource，该租户业务数据按 DataSourceKey 路由到独立库。
 */
@Table("sys_tenant_datasource")
public class SysTenantDatasource extends BaseEntity {

	private String tenantCode;
	private String dsUrl;
	private String dsUsername;
	private String dsPassword;
	private Integer status;
	private String remark;

	public String getTenantCode() {
		return tenantCode;
	}

	public void setTenantCode(String tenantCode) {
		this.tenantCode = tenantCode;
	}

	public String getDsUrl() {
		return dsUrl;
	}

	public void setDsUrl(String dsUrl) {
		this.dsUrl = dsUrl;
	}

	public String getDsUsername() {
		return dsUsername;
	}

	public void setDsUsername(String dsUsername) {
		this.dsUsername = dsUsername;
	}

	public String getDsPassword() {
		return dsPassword;
	}

	public void setDsPassword(String dsPassword) {
		this.dsPassword = dsPassword;
	}

	public Integer getStatus() {
		return status;
	}

	public void setStatus(Integer status) {
		this.status = status;
	}

	public String getRemark() {
		return remark;
	}

	public void setRemark(String remark) {
		this.remark = remark;
	}
}
