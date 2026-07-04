package com.mugsun.boot.system.entity;

import com.mugsun.core.mybatis.base.BaseEntity;
import com.mybatisflex.annotation.Table;

/**
 * 数据变更审计（前后镜像）
 */
@Table("sys_data_audit")
public class SysDataAudit extends BaseEntity {

	private String bizTable;
	private String bizId;
	private String beforeData;
	private String afterData;
	private String operator;

	public String getBizTable() {
		return bizTable;
	}

	public void setBizTable(String bizTable) {
		this.bizTable = bizTable;
	}

	public String getBizId() {
		return bizId;
	}

	public void setBizId(String bizId) {
		this.bizId = bizId;
	}

	public String getBeforeData() {
		return beforeData;
	}

	public void setBeforeData(String beforeData) {
		this.beforeData = beforeData;
	}

	public String getAfterData() {
		return afterData;
	}

	public void setAfterData(String afterData) {
		this.afterData = afterData;
	}

	public String getOperator() {
		return operator;
	}

	public void setOperator(String operator) {
		this.operator = operator;
	}
}
