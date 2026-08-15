-- 由 scripts/pg_to_dm.py 从 db/migration 转换（达梦 Oracle 系）。
-- 不引用 pg_catalog / ON CONFLICT / 部分索引 / VALUES 行构造器。

-- V58 菜单管理字段补齐（BladeX 标配）：图标 / 隐藏 / 页面缓存(keep-alive) / 外链新窗口
-- 全部幂等（ADD COLUMN ）；存量行由 DEFAULT 回填：默认显示、缓存、非外链

ALTER TABLE sys_menu ADD icon          VARCHAR(64);
ALTER TABLE sys_menu ADD is_hide       INT DEFAULT 0 NOT NULL;
ALTER TABLE sys_menu ADD is_keep_alive INT DEFAULT 1 NOT NULL;
ALTER TABLE sys_menu ADD is_external   INT DEFAULT 0 NOT NULL;

COMMENT ON COLUMN sys_menu.icon IS '图标（Iconify 名，如 ri:user-line，与前端路由 meta.icon 同体系）';
COMMENT ON COLUMN sys_menu.is_hide IS '是否隐藏（0 显示 / 1 隐藏，隐藏后不出现在侧边栏）';
COMMENT ON COLUMN sys_menu.is_keep_alive IS '是否缓存页面（0 不缓存 / 1 缓存，对应路由 keepAlive）';
COMMENT ON COLUMN sys_menu.is_external IS '是否外链（0 否 / 1 是，外链新窗口打开）';
