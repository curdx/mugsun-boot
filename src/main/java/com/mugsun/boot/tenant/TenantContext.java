package com.mugsun.boot.tenant;

import cn.dev33.satoken.stp.StpUtil;

import java.util.function.Supplier;

/**
 * 租户上下文：统一的当前租户解析与作用域切换，供 {@link SaTokenTenantFactory} 取值。
 * <p>解析优先级：显式 ThreadLocal（异步透传 / execute / ignore）＞ Sa-Token 会话超管切换租户 ＞ 会话本租户。
 * <p>ThreadLocal 由 {@link com.mugsun.boot.tenant.TenantTaskDecorator} 在 @Async/虚拟线程间捕获-恢复，
 * 严格 finally remove，杜绝虚拟线程复用串号。空串为“显式忽略租户”哨兵（不施加隔离）。
 */
public final class TenantContext {

	/** 超管切换租户会话键；值 -1 表示查看全部租户 */
	public static final String SWITCH_KEY = "switchTenant";
	public static final String ALL = "-1";

	private static final ThreadLocal<String> HOLDER = new ThreadLocal<>();

	private TenantContext() {
	}

	public static void set(String tenantId) {
		HOLDER.set(tenantId);
	}

	public static String getRaw() {
		return HOLDER.get();
	}

	public static void clear() {
		HOLDER.remove();
	}

	/** 当前有效租户；返回 null 表示不施加租户条件（全部） */
	public static String current() {
		String ctx = HOLDER.get();
		if (ctx != null) {
			return ctx.isBlank() ? null : ctx;
		}
		try {
			if (StpUtil.isLogin()) {
				Object sw = StpUtil.getSession().get(SWITCH_KEY);
				if (sw != null) {
					String s = sw.toString();
					return (ALL.equals(s) || s.isBlank()) ? null : s;
				}
				Object t = StpUtil.getSession().get("tenantId");
				return t == null ? null : t.toString();
			}
		} catch (Exception ignored) {
			// 无会话上下文（异步/初始化）
		}
		return null;
	}

	/** 作用域执行：临时切到指定租户，存旧值→set→finally 恢复（可嵌套） */
	public static <T> T execute(String tenantId, Supplier<T> supplier) {
		String prev = HOLDER.get();
		HOLDER.set(tenantId);
		try {
			return supplier.get();
		} finally {
			restore(prev);
		}
	}

	public static void execute(String tenantId, Runnable runnable) {
		execute(tenantId, () -> {
			runnable.run();
			return null;
		});
	}

	/** 作用域忽略租户（空串哨兵，current() 返 null 即不隔离），可嵌套 */
	public static <T> T ignore(Supplier<T> supplier) {
		return execute("", supplier);
	}

	public static void ignore(Runnable runnable) {
		execute("", () -> {
			runnable.run();
			return null;
		});
	}

	private static void restore(String prev) {
		if (prev == null) {
			HOLDER.remove();
		} else {
			HOLDER.set(prev);
		}
	}
}
