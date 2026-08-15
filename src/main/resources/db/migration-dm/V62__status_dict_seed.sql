-- 由 scripts/pg_to_dm.py 从 db/migration 转换（达梦 Oracle 系）。
-- 不引用 pg_catalog / ON CONFLICT / 部分索引 / VALUES 行构造器。

-- W5-H6 字典标签铺开：notice 分类 / feedback 状态 / 流程实例状态 / 错误日志处理状态 字典种子，
-- 供前端 ArtDictTag 纯字典驱动着色，替代各页手写文案 map（颜色沿用 Element 色系）
INSERT INTO sys_dict (id, parent_id, code, dict_key, dict_value, sort, is_sealed, color, create_time, is_deleted) VALUES
-- 通知公告分类（views/system/notice 分类列）
(1050000000000000101, 0,                   'notice_category', 'notice_category', '通知公告分类', 0, 1, NULL,      SYSDATE, 0),
(1050000000000000102, 1050000000000000101, 'notice_category', 'notice',          '通知',        1, 0, '#409EFF', SYSDATE, 0),
(1050000000000000103, 1050000000000000101, 'notice_category', 'announcement',    '公告',        2, 0, '#67C23A', SYSDATE, 0),
(1050000000000000104, 1050000000000000101, 'notice_category', 'warning',         '预警',        3, 0, '#E6A23C', SYSDATE, 0),
-- 反馈处理状态（views/system/feedback 状态列：0 未处理 / 1 已处理）
(1050000000000000111, 0,                   'feedback_status', 'feedback_status', '反馈处理状态', 0, 1, NULL,      SYSDATE, 0),
(1050000000000000112, 1050000000000000111, 'feedback_status', '0',               '未处理',      1, 0, '#909399', SYSDATE, 0),
(1050000000000000113, 1050000000000000111, 'feedback_status', '1',               '已处理',      2, 0, '#67C23A', SYSDATE, 0),
-- 流程实例状态（views/system/flow-todo 抄送状态列/时间线，对应 warm-flow FlowStatus）
(1050000000000000121, 0,                   'flow_status', 'flow_status', '流程实例状态', 0,  1, NULL,      SYSDATE, 0),
(1050000000000000122, 1050000000000000121, 'flow_status', '0',           '待提交',       1,  0, '#909399', SYSDATE, 0),
(1050000000000000123, 1050000000000000121, 'flow_status', '1',           '审批中',       2,  0, '#409EFF', SYSDATE, 0),
(1050000000000000124, 1050000000000000121, 'flow_status', '2',           '已通过',       3,  0, '#67C23A', SYSDATE, 0),
(1050000000000000125, 1050000000000000121, 'flow_status', '3',           '自动完成',     4,  0, '#67C23A', SYSDATE, 0),
(1050000000000000126, 1050000000000000121, 'flow_status', '4',           '已终止',       5,  0, '#F56C6C', SYSDATE, 0),
(1050000000000000127, 1050000000000000121, 'flow_status', '5',           '已作废',       6,  0, '#F56C6C', SYSDATE, 0),
(1050000000000000128, 1050000000000000121, 'flow_status', '6',           '已撤销',       7,  0, '#909399', SYSDATE, 0),
(1050000000000000129, 1050000000000000121, 'flow_status', '7',           '已取回',       8,  0, '#909399', SYSDATE, 0),
(1050000000000000130, 1050000000000000121, 'flow_status', '8',           '已完成',       9,  0, '#67C23A', SYSDATE, 0),
(1050000000000000131, 1050000000000000121, 'flow_status', '9',           '已退回',       10, 0, '#E6A23C', SYSDATE, 0),
(1050000000000000132, 1050000000000000121, 'flow_status', '10',          '已失效',       11, 0, '#909399', SYSDATE, 0),
(1050000000000000133, 1050000000000000121, 'flow_status', '11',          '已拿回',       12, 0, '#E6A23C', SYSDATE, 0),
(1050000000000000134, 1050000000000000121, 'flow_status', '12',          '已重启',       13, 0, '#409EFF', SYSDATE, 0),
(1050000000000000135, 1050000000000000121, 'flow_status', '13',          '暂存',         14, 0, '#909399', SYSDATE, 0),
-- 错误日志处理状态（views/system/error-log 状态列：0 未处理 / 1 已处理 / 2 已忽略）
(1050000000000000141, 0,                   'error_log_status', 'error_log_status', '错误日志处理状态', 0, 1, NULL,      SYSDATE, 0),
(1050000000000000142, 1050000000000000141, 'error_log_status', '0',               '未处理',        1, 0, '#E6A23C', SYSDATE, 0),
(1050000000000000143, 1050000000000000141, 'error_log_status', '1',               '已处理',        2, 0, '#67C23A', SYSDATE, 0),
(1050000000000000144, 1050000000000000141, 'error_log_status', '2',               '已忽略',        3, 0, '#909399', SYSDATE, 0);
