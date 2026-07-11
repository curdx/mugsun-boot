package com.mugsun.boot.oauth;

/**
 * /open/** 请求的租户上下文持有器：由 {@link OpenApiInterceptor} 从访问令牌的 tenantId 写入，
 * 供 {@code SaTokenTenantFactory} 在无 Sa-Token 会话时回退读取，使开放接口按令牌租户隔离。
 */
public final class OpenApiTenantHolder {

	private static final ThreadLocal<String> TENANT = new ThreadLocal<>();

	private OpenApiTenantHolder() {
	}

	public static void set(String tenantId) {
		TENANT.set(tenantId);
	}

	public static String get() {
		return TENANT.get();
	}

	public static void clear() {
		TENANT.remove();
	}
}
