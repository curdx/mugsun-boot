package com.mugsun.boot.tenant;

import com.mybatisflex.core.tenant.TenantFactory;

/**
 * 租户工厂：委派 {@link TenantContext#current()} 统一解析当前有效租户
 * （会话租户 / 超管切换 / 异步透传 / execute-ignore 作用域 / 开放接口令牌租户）。
 * 返回 null 即不施加租户条件。
 */
public class SaTokenTenantFactory implements TenantFactory {

	@Override
	public Object[] getTenantIds() {
		return getTenantIds(null);
	}

	@Override
	public Object[] getTenantIds(String tableName) {
		String tenant = TenantContext.current();
		return (tenant == null || tenant.isBlank()) ? null : new Object[]{tenant};
	}
}
