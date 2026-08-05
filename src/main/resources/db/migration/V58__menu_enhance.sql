-- V58 菜单管理字段补齐（BladeX 标配）：图标 / 隐藏 / 页面缓存(keep-alive) / 外链新窗口
-- 全部幂等（ADD COLUMN IF NOT EXISTS）；存量行由 DEFAULT 回填：默认显示、缓存、非外链

ALTER TABLE sys_menu ADD COLUMN IF NOT EXISTS icon          VARCHAR(64);
ALTER TABLE sys_menu ADD COLUMN IF NOT EXISTS is_hide       INT NOT NULL DEFAULT 0;
ALTER TABLE sys_menu ADD COLUMN IF NOT EXISTS is_keep_alive INT NOT NULL DEFAULT 1;
ALTER TABLE sys_menu ADD COLUMN IF NOT EXISTS is_external   INT NOT NULL DEFAULT 0;

COMMENT ON COLUMN sys_menu.icon IS '图标（Iconify 名，如 ri:user-line，与前端路由 meta.icon 同体系）';
COMMENT ON COLUMN sys_menu.is_hide IS '是否隐藏（0 显示 / 1 隐藏，隐藏后不出现在侧边栏）';
COMMENT ON COLUMN sys_menu.is_keep_alive IS '是否缓存页面（0 不缓存 / 1 缓存，对应路由 keepAlive）';
COMMENT ON COLUMN sys_menu.is_external IS '是否外链（0 否 / 1 是，外链新窗口打开）';
