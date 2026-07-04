package com.mugsun.boot.system.entity;

import com.mugsun.core.mybatis.base.BaseEntity;
import com.mugsun.core.tool.tree.INode;
import com.mybatisflex.annotation.Column;
import com.mybatisflex.annotation.Table;

import java.util.List;

/**
 * 菜单 / 按钮权限
 */
@Table("sys_menu")
public class SysMenu extends BaseEntity implements INode<SysMenu> {

	private Long parentId;
	private String menuName;
	private String path;
	private String component;
	private String menuType;
	private String permission;
	private Integer sort;

	@Column(ignore = true)
	private List<SysMenu> children;

	@Override
	public Long getParentId() {
		return parentId;
	}

	public void setParentId(Long parentId) {
		this.parentId = parentId;
	}

	public String getMenuName() {
		return menuName;
	}

	public void setMenuName(String menuName) {
		this.menuName = menuName;
	}

	public String getPath() {
		return path;
	}

	public void setPath(String path) {
		this.path = path;
	}

	public String getComponent() {
		return component;
	}

	public void setComponent(String component) {
		this.component = component;
	}

	public String getMenuType() {
		return menuType;
	}

	public void setMenuType(String menuType) {
		this.menuType = menuType;
	}

	public String getPermission() {
		return permission;
	}

	public void setPermission(String permission) {
		this.permission = permission;
	}

	public Integer getSort() {
		return sort;
	}

	public void setSort(Integer sort) {
		this.sort = sort;
	}

	@Override
	public List<SysMenu> getChildren() {
		return children;
	}

	@Override
	public void setChildren(List<SysMenu> children) {
		this.children = children;
	}
}
