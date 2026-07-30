-- G89 对象存储多云：分片登记（FileRecorder.saveFilePart 落库，支撑分片上传/断点续传的分片追踪与清理）
-- + 私有附件预签名下载 URL 有效期参数种子

-- 分片上传的分片登记：x-file-storage FileRecorder 语义对齐 FilePartInfo 字段，
-- upload_id 关联一次分片上传会话，完成/中止时按 upload_id 级联清理（deleteFilePartByUploadId）
CREATE TABLE sys_attach_part (
	id            BIGINT       PRIMARY KEY,
	tenant_id     VARCHAR(12),
	platform      VARCHAR(64)  NOT NULL,
	upload_id     VARCHAR(128) NOT NULL,
	e_tag         VARCHAR(255),
	part_number   INT,
	part_size     BIGINT,
	hash_info     VARCHAR(512),
	last_modified TIMESTAMP,
	create_time   TIMESTAMP,
	update_time   TIMESTAMP,
	is_deleted    INT          NOT NULL DEFAULT 0
);
CREATE INDEX idx_attach_part_upload ON sys_attach_part (upload_id) WHERE is_deleted = 0;
COMMENT ON TABLE sys_attach_part IS '附件分片登记（x-file-storage 分片上传会话的分片追踪，upload_id 级联清理）';
COMMENT ON COLUMN sys_attach_part.upload_id IS '分片上传会话标识（平台侧 multipart uploadId）';
COMMENT ON COLUMN sys_attach_part.e_tag IS '分片 ETag（completeMultipartUpload 合并凭证）';
COMMENT ON COLUMN sys_attach_part.hash_info IS '分片摘要 JSON（x-file-storage HashInfo 序列化，可空）';

-- 私有附件预签名下载 URL 有效期（秒）：云平台私有附件 download/{id} 签发限时 URL 用
INSERT INTO sys_param (id, param_name, param_key, param_value, remark, create_time, is_deleted) VALUES
(1091000000000000001, '私有下载签名URL有效期', 'oss.presigned-url-expire-seconds', '300',
 '云平台私有附件 download/{id} 签发的预签名 URL 有效期（秒），过期须重新授权获取', now(), 0)
ON CONFLICT (id) DO NOTHING;
