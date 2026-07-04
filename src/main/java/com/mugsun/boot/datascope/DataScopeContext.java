package com.mugsun.boot.datascope;

/**
 * 数据权限上下文（请求级 ThreadLocal，请求结束必须 clear）
 */
public class DataScopeContext {

	/**
	 * @param dataScope 1 全部 / 2 本部门 / 3 仅本人
	 * @param deptId    当前用户部门
	 * @param userId    当前用户
	 */
	public record Scope(Integer dataScope, Long deptId, Long userId) {
	}

	private static final ThreadLocal<Scope> HOLDER = new ThreadLocal<>();

	public static void set(Scope scope) {
		HOLDER.set(scope);
	}

	public static Scope get() {
		return HOLDER.get();
	}

	public static void clear() {
		HOLDER.remove();
	}
}
