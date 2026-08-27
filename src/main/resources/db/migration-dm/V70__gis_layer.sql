-- 由 scripts/pg_to_dm.py 从 db/migration 转换（达梦 Oracle 系）。
-- 不引用 pg_catalog / ON CONFLICT / 部分索引 / VALUES 行构造器。

-- GIS 图层库 + 独立模块菜单（与埋点同级：工作台 / 图层 / 场景 / 底图）
CREATE TABLE gis_layer (
	id             BIGINT        PRIMARY KEY,
	tenant_id      VARCHAR(12),
	name           VARCHAR(128)  NOT NULL,
	kind           VARCHAR(32)   DEFAULT 'vector' NOT NULL,
	crs            VARCHAR(32)   DEFAULT 'EPSG:4326' NOT NULL,
	data_json      CLOB          NOT NULL,
	style_json     CLOB,
	feature_count  INT           DEFAULT 0 NOT NULL,
	bbox           VARCHAR(128),
	status         INT           DEFAULT 1 NOT NULL,
	remark         VARCHAR(255),
	create_time    TIMESTAMP,
	update_time    TIMESTAMP,
	is_deleted     INT           DEFAULT 0 NOT NULL
);

CREATE INDEX idx_gis_layer_tenant ON gis_layer (tenant_id);

COMMENT ON TABLE gis_layer IS '通用 GIS 图层（WGS84 GeoJSON，跨模块叠加）';
COMMENT ON COLUMN gis_layer.kind IS 'vector/heatmap';
COMMENT ON COLUMN gis_layer.crs IS '规范化后固定 EPSG:4326';

UPDATE sys_menu SET sort = 3, menu_name = '地理信息', icon = 'ri:earth-line', parent_id = 0, menu_type = 'M'
	WHERE path = '/gis' AND is_deleted = 0;
UPDATE sys_menu SET sort = 4 WHERE path = '/system' AND is_deleted = 0;
UPDATE sys_menu SET sort = 5 WHERE path = '/saas' AND is_deleted = 0;
UPDATE sys_menu SET sort = 6 WHERE path = '/open-platform' AND is_deleted = 0;

UPDATE sys_menu SET sort = 1, menu_name = '地图工作台'
	WHERE path = '/gis/workspace' AND is_deleted = 0;
UPDATE sys_menu SET sort = 4, menu_name = '底图配置'
	WHERE path = '/gis/provider' AND is_deleted = 0;

INSERT INTO sys_menu (id, parent_id, menu_name, path, component, menu_type, permission, sort, icon, is_public, create_time, is_deleted)
SELECT v.id, p.id, v.name, v.path, v.component, 'C', v.perm, v.sort, v.icon, 0, SYSDATE, 0
FROM (
	SELECT 1094000000000000008 AS id, '图层' AS name, '/gis/layer' AS path, '/gis/layer' AS component, 'gis:layer:list' AS perm, 2 AS sort, 'ri:stack-line' AS icon FROM DUAL
	UNION ALL
	SELECT 1094000000000000009, '场景', '/gis/scene', '/gis/scene', 'gis:scene:list', 3, 'ri:landscape-line' FROM DUAL
) v
JOIN sys_menu p ON p.path = '/gis' AND p.is_deleted = 0
WHERE NOT EXISTS (SELECT 1 FROM sys_menu x WHERE x.path = v.path AND x.is_deleted = 0);

INSERT INTO sys_menu (id, parent_id, menu_name, menu_type, permission, sort, create_time, is_deleted)
SELECT v.id, v.parent, v.name, 'F', v.perm, v.sort, SYSDATE, 0
FROM (
	SELECT 1094000000000000010 AS id, 1094000000000000008 AS parent, '保存图层' AS name, 'gis:layer:save' AS perm, 1 AS sort FROM DUAL
	UNION ALL
	SELECT 1094000000000000011, 1094000000000000008, '删除图层', 'gis:layer:remove', 2 FROM DUAL
) v
WHERE NOT EXISTS (SELECT 1 FROM sys_menu x WHERE x.permission = v.perm AND x.is_deleted = 0);
