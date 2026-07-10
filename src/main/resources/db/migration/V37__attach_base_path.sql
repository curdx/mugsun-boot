ALTER TABLE sys_attach ADD COLUMN base_path VARCHAR(255);

COMMENT ON COLUMN sys_attach.base_path IS '存储平台基础路径（授权流式下载定位文件用）';
