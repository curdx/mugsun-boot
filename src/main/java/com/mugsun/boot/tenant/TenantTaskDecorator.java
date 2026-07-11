package com.mugsun.boot.tenant;

import org.springframework.core.task.TaskDecorator;

/**
 * 异步任务租户上下文透传：在提交线程（请求线程）捕获当前有效租户，
 * 于工作线程（@Async/虚拟线程）设入 {@link TenantContext}，执行后严格清除——
 * 使异步/定时写入落到正确租户，杜绝虚拟线程复用导致的租户串号。
 */
public class TenantTaskDecorator implements TaskDecorator {

	@Override
	public Runnable decorate(Runnable runnable) {
		String tenantId = TenantContext.current();
		return () -> {
			if (tenantId != null) {
				TenantContext.set(tenantId);
			}
			try {
				runnable.run();
			} finally {
				TenantContext.clear();
			}
		};
	}
}
