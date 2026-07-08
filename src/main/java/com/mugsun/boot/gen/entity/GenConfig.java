package com.mugsun.boot.gen.entity;

import com.mugsun.core.mybatis.base.BaseEntity;
import com.mybatisflex.annotation.Table;

/**
 * 代码生成表配置：按表名持久化生成参数，重开回显。
 */
@Table("gen_config")
public class GenConfig extends BaseEntity {

	/** 表名（唯一） */
	private String tableName;
	/** 基础包 */
	private String basePackage;
	/** 表前缀 */
	private String tablePrefix;

	public String getTableName() {
		return tableName;
	}

	public void setTableName(String tableName) {
		this.tableName = tableName;
	}

	public String getBasePackage() {
		return basePackage;
	}

	public void setBasePackage(String basePackage) {
		this.basePackage = basePackage;
	}

	public String getTablePrefix() {
		return tablePrefix;
	}

	public void setTablePrefix(String tablePrefix) {
		this.tablePrefix = tablePrefix;
	}
}
