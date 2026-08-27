-- 用户档案对齐：真实姓名/性别/生日/工号/直属主管/是否主管（头像列已在 V61）
ALTER TABLE sys_user ADD COLUMN IF NOT EXISTS real_name VARCHAR(64);
ALTER TABLE sys_user ADD COLUMN IF NOT EXISTS sex SMALLINT;
ALTER TABLE sys_user ADD COLUMN IF NOT EXISTS birthday DATE;
ALTER TABLE sys_user ADD COLUMN IF NOT EXISTS code VARCHAR(64);
ALTER TABLE sys_user ADD COLUMN IF NOT EXISTS leader_id BIGINT;
ALTER TABLE sys_user ADD COLUMN IF NOT EXISTS is_leader SMALLINT NOT NULL DEFAULT 0;

COMMENT ON COLUMN sys_user.real_name IS '真实姓名';
COMMENT ON COLUMN sys_user.sex IS '性别：0 未知 / 1 男 / 2 女';
COMMENT ON COLUMN sys_user.birthday IS '生日';
COMMENT ON COLUMN sys_user.code IS '工号/用户编号';
COMMENT ON COLUMN sys_user.leader_id IS '直属主管用户 id';
COMMENT ON COLUMN sys_user.is_leader IS '是否主管：1 是 / 0 否（可供流程 deptLeader 等选用）';

-- 性别字典（id 与 notice_category 冲突时 ON CONFLICT 跳过；正式补种见 V77）
INSERT INTO sys_dict (id, parent_id, code, dict_key, dict_value, sort, is_sealed, create_time, is_deleted) VALUES
(1050000000000000101, 0,                   'user_sex', 'user_sex', '用户性别', 0, 1, now(), 0),
(1050000000000000102, 1050000000000000101, 'user_sex', '0',        '未知',     1, 0, now(), 0),
(1050000000000000103, 1050000000000000101, 'user_sex', '1',        '男',       2, 0, now(), 0),
(1050000000000000104, 1050000000000000101, 'user_sex', '2',        '女',       3, 0, now(), 0)
ON CONFLICT (id) DO NOTHING;
