package com.mugsun.boot.datascope;

import com.mybatisflex.core.query.QueryWrapper;

import java.util.function.Consumer;

/**
 * 数据权限条件持有器（请求级 ThreadLocal）：由 {@link DataScopeAspect} 写入、查询方法读取应用、方法结束清理。
 */
public class DataScopeHolder {

	private static final ThreadLocal<Consumer<QueryWrapper>> HOLDER = new ThreadLocal<>();

	static void set(Consumer<QueryWrapper> applier) {
		HOLDER.set(applier);
	}

	/** 将当前数据权限条件应用到查询；无条件（全部数据/无上下文）则不改动 */
	public static void apply(QueryWrapper query) {
		Consumer<QueryWrapper> applier = HOLDER.get();
		if (applier != null) {
			applier.accept(query);
		}
	}

	static void clear() {
		HOLDER.remove();
	}
}
