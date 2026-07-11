package com.mugsun.boot.tenant;

import cn.dev33.satoken.stp.StpUtil;
import com.mugsun.boot.oauth.OpenApiTenantHolder;
import com.mybatisflex.core.tenant.TenantFactory;

/**
 * 租户工厂：从当前登录会话取租户编号。
 * 无登录上下文（如启动初始化、登录鉴权前）返回 null，即不施加租户条件。
 * /open/** 开放接口无 Sa-Token 会话时，回退按访问令牌绑定的租户隔离（防跨租户越权）。
 */
public class SaTokenTenantFactory implements TenantFactory {

	@Override
	public Object[] getTenantIds() {
		return getTenantIds(null);
	}

	@Override
	public Object[] getTenantIds(String tableName) {
		try {
			if (StpUtil.isLogin()) {
				Object tenantId = StpUtil.getSession().get("tenantId");
				if (tenantId != null) {
					return new Object[]{tenantId};
				}
			}
		} catch (Exception e) {
			// 无会话/异常时回退开放接口令牌租户
		}
		String openTenant = OpenApiTenantHolder.get();
		return (openTenant == null || openTenant.isBlank()) ? null : new Object[]{openTenant};
	}
}
