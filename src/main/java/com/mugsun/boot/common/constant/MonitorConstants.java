package com.mugsun.boot.common.constant;

import java.util.List;
import java.util.Set;

/**
 * 全局可观测（G90）常量：链路追踪 / 访问日志 / 错误日志 / 指标端点 / 日志清理。
 */
public interface MonitorConstants {

	/** 链路追踪请求/响应头（网关预留下发沿用，缺省服务端生成） */
	String TRACE_HEADER = "X-Trace-Id";
	/** MDC 中 traceId 键（日志 pattern 输出与异步透传共用） */
	String TRACE_MDC_KEY = "traceId";

	/** sys_param 键：访问日志采样率（0-100，仅对 GET 生效；写操作有 oper_log 留痕） */
	String PARAM_SAMPLE_RATE = "monitor.access-log.sample-rate";
	/** sys_param 键：慢接口阈值（毫秒），超过必记且 slow=1（不受采样影响） */
	String PARAM_SLOW_MS = "monitor.access-log.slow-ms";
	/** sys_param 键：日志保留天数（api_log/error_log/oper_log 超期物理清理） */
	String PARAM_RETENTION_DAYS = "monitor.log.retention-days";
	/** 兜底默认：全量采样 */
	int DEFAULT_SAMPLE_RATE = 100;
	/** 兜底默认：慢接口阈值 1s */
	long DEFAULT_SLOW_MS = 1000L;
	/** 兜底默认：保留 30 天 */
	int DEFAULT_RETENTION_DAYS = 30;

	/** 访问日志排除路径前缀（监控端点/文档/推送/静态与上传下载，与 XssFilter 跳过集合并集） */
	List<String> API_LOG_EXCLUDES = List.of(
		"/actuator", "/v3/api-docs", "/doc.html", "/swagger", "/warm-flow",
		"/ws", "/file", "/system/oss", "/system/file", "/favicon.ico", "/error");

	/** 受鉴权保护的 actuator 端点前缀（health/info 保持公开保 G1 契约） */
	Set<String> ACTUATOR_GUARDED = Set.of("/actuator/prometheus", "/actuator/metrics");

	/** 权限码：访问日志查询 */
	String PERM_API_LOG_LIST = "sys:api-log:list";
	/** 权限码：错误日志查询 */
	String PERM_ERROR_LOG_LIST = "sys:error-log:list";
	/** 权限码：错误日志认领处理 */
	String PERM_ERROR_LOG_HANDLE = "sys:error-log:handle";
	/** 权限码：错误日志删除 */
	String PERM_ERROR_LOG_REMOVE = "sys:error-log:remove";
	/** 权限码：服务监控（含受保护的 actuator 端点） */
	String PERM_MONITOR_LIST = "sys:monitor:list";
	/** 权限码：在线数据库文档 */
	String PERM_DB_DOC = "sys:monitor:db-doc";

	/** 错误日志状态：未处理 */
	int ERROR_STATUS_TODO = 0;
	/** 错误日志状态：已处理 */
	int ERROR_STATUS_DONE = 1;
	/** 错误日志状态：已忽略 */
	int ERROR_STATUS_IGNORED = 2;

	/** 脱敏占位符 */
	String MASK = "***";
	/** 参数摘要截断长度 */
	int PARAMS_MAX_LEN = 2000;
	/** 异常摘要截断长度 */
	int ERROR_MSG_MAX_LEN = 500;
	/** 堆栈截断长度 */
	int STACK_MAX_LEN = 8000;
	/** UA 截断长度（与登录日志 500 对齐） */
	int UA_MAX_LEN = 500;

	/** 日志清理调度 tick（固定 1h 触发一次，实际清理按节流间隔执行） */
	long CLEAN_TICK_MS = 3600000L;
	/** 清理节流间隔（默认 24h，本节点内存节流 + 分布式锁兜底） */
	long CLEAN_INTERVAL_MS = 86400000L;
	/** 清理分布式锁键（Redis SETNX，集群仅一节点执行） */
	String CLEAN_LOCK_KEY = "mugsun:monitor:log-clean-lock";
	/** 锁持有上限（秒） */
	long CLEAN_LOCK_SECONDS = 600L;

	/** 在线终端扩展数据键：登录 IP / 登录 UA（SaLoginParameter.terminalExtra → SaTerminalInfo） */
	String TERMINAL_EXTRA_IP = "loginIp";
	String TERMINAL_EXTRA_UA = "loginUa";

	/** 请求属性键：ErrorLogRecorder 回填异常摘要，ApiLogFilter 取作访问日志 error_msg */
	String ERROR_SUMMARY_ATTR = "mugsun.error.summary";
}
