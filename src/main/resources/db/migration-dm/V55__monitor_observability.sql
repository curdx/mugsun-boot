-- 由 scripts/pg_to_dm.py 从 db/migration 转换（达梦 Oracle 系）。
-- 不引用 pg_catalog / ON CONFLICT / 部分索引 / VALUES 行构造器。

-- G90 全局可观测：访问日志（轻表无哈希链）+ 错误日志处理闭环 + 监控参数种子 + 权限锚点

-- 访问日志：全量请求流量（含 GET，采样仅对 GET 生效），轻表无哈希链——
-- 刻意不复用 sys_oper_log 落库路径（其 SM3 哈希链 synchronized 串行点无法承受全量 GET 流量）
CREATE TABLE sys_api_log (
	id            BIGINT PRIMARY KEY,
	trace_id      VARCHAR(64),
	title         VARCHAR(255),
	method        VARCHAR(255),
	request_method VARCHAR(8),
	request_uri   VARCHAR(512),
	ip            VARCHAR(64),
	user_agent    VARCHAR(512),
	operator      VARCHAR(64),
	tenant_id     VARCHAR(12),
	status        INT,
	duration      BIGINT,
	slow          INT          DEFAULT 0 NOT NULL,
	params        CLOB,
	error_msg     VARCHAR(512),
	create_time   TIMESTAMP,
	update_time   TIMESTAMP,
	is_deleted    INT          DEFAULT 0 NOT NULL
);
CREATE INDEX idx_api_log_trace ON sys_api_log (trace_id);
CREATE INDEX idx_api_log_time ON sys_api_log (create_time);
CREATE INDEX idx_api_log_slow ON sys_api_log (slow);
COMMENT ON TABLE sys_api_log IS '访问日志（全量请求流水；慢接口 slow=1 必记不受采样影响；params 结构化递归脱敏截断）';
COMMENT ON COLUMN sys_api_log.trace_id IS '全站链路追踪号（与响应头 X-Trace-Id 一致）';
COMMENT ON COLUMN sys_api_log.title IS '接口标题（@OperationLog ＞ @Operation ＞ @Tag ＞ uri 回退链）';
COMMENT ON COLUMN sys_api_log.status IS 'HTTP 响应状态码';
COMMENT ON COLUMN sys_api_log.slow IS '慢接口标记：1 超 monitor.access-log.slow-ms';

-- 错误日志：全局未捕获异常落库，栈顶四元组精确定位 + 处理闭环（0 未处理/1 已处理/2 已忽略认领）
CREATE TABLE sys_error_log (
	id              BIGINT PRIMARY KEY,
	trace_id        VARCHAR(64),
	request_uri     VARCHAR(512),
	request_method  VARCHAR(8),
	operator        VARCHAR(64),
	tenant_id       VARCHAR(12),
	exception_class VARCHAR(512),
	message         VARCHAR(1024),
	location_class  VARCHAR(255),
	location_file   VARCHAR(255),
	location_method VARCHAR(255),
	location_line   INT,
	stacktrace      CLOB,
	status          INT          DEFAULT 0 NOT NULL,
	handle_user     VARCHAR(64),
	handle_note     VARCHAR(512),
	handle_time     TIMESTAMP,
	create_time     TIMESTAMP,
	update_time     TIMESTAMP,
	is_deleted      INT          DEFAULT 0 NOT NULL
);
CREATE INDEX idx_error_log_trace ON sys_error_log (trace_id);
CREATE INDEX idx_error_log_status ON sys_error_log (status);
COMMENT ON TABLE sys_error_log IS '错误日志（未捕获异常；栈顶四元组定位；处理闭环 0 未处理/1 已处理/2 已忽略）';
COMMENT ON COLUMN sys_error_log.location_class IS '栈顶定位：首个 com.mugsun 业务栈帧类名（无则首帧）';
COMMENT ON COLUMN sys_error_log.stacktrace IS '完整堆栈（截断 8000）';

-- 监控参数种子（代码常量兜底默认，此处落库支持运行时调整）
INSERT INTO sys_param (id, param_name, param_key, param_value, remark, create_time, is_deleted) VALUES
(900015, '访问日志采样率(%)', 'monitor.access-log.sample-rate', '100', '仅对 GET 生效（写操作有 oper_log 留痕）；慢接口必记不受采样影响', SYSDATE, 0),
(900016, '慢接口阈值(毫秒)', 'monitor.access-log.slow-ms', '1000', '超过必记且 slow=1 标记', SYSDATE, 0),
(900017, '日志保留天数', 'monitor.log.retention-days', '30', 'api_log/error_log/oper_log 超期物理清理', SYSDATE, 0);

-- 监控权限锚点：页面菜单为前端静态路由驱动（不走 DB），此处仅作权限码载体供角色→菜单派生 buttons
INSERT INTO sys_menu (id, parent_id, menu_name, menu_type, permission, sort, create_time, is_deleted) VALUES
(1092000000000000001, 0, '访问日志', 'C', 'sys:api-log:list', 90, SYSDATE, 0),
(1092000000000000002, 0, '错误日志', 'C', 'sys:error-log:list', 91, SYSDATE, 0),
(1092000000000000003, 1092000000000000002, '处理错误', 'F', 'sys:error-log:handle', 1, SYSDATE, 0),
(1092000000000000004, 1092000000000000002, '删除错误', 'F', 'sys:error-log:remove', 2, SYSDATE, 0),
(1092000000000000005, 0, '服务监控', 'C', 'sys:monitor:list', 92, SYSDATE, 0),
(1092000000000000006, 1092000000000000005, '数据库文档', 'F', 'sys:monitor:db-doc', 1, SYSDATE, 0);
