package com.mugsun.boot.tenant;

import cn.dev33.satoken.stp.StpUtil;
import com.mybatisflex.core.tenant.TenantFactory;

/**
 * 租户工厂：从当前登录会话取租户编号。
 * 无登录上下文（如启动初始化、登录鉴权前）返回 null，即不施加租户条件。
 */
public class SaTokenTenantFactory implements TenantFactory {

	@Override
	public Object[] getTenantIds() {
		return getTenantIds(null);
	}

	@Override
	public Object[] getTenantIds(String tableName) {
		try {
			if (!StpUtil.isLogin()) {
				return null;
			}
			Object tenantId = StpUtil.getSession().get("tenantId");
			return tenantId == null ? null : new Object[]{tenantId};
		} catch (Exception e) {
			return null;
		}
	}
}
