package com.mugsun.boot.system.service;

import com.mugsun.boot.system.entity.SysOss;
import com.mugsun.boot.system.mapper.SysOssMapper;
import com.mybatisflex.core.query.QueryWrapper;
import org.dromara.x.file.storage.core.FileStorageProperties;
import org.dromara.x.file.storage.core.FileStorageService;
import org.dromara.x.file.storage.core.platform.LocalPlusFileStorage;
import org.springframework.stereotype.Service;

/**
 * 对象存储配置解析：按启用配置动态注册并选用 x-file-storage 存储平台，实现渠道运行时切换。
 */
@Service
public class OssService {

	private final FileStorageService fileStorageService;
	private final SysOssMapper ossMapper;

	public OssService(FileStorageService fileStorageService, SysOssMapper ossMapper) {
		this.fileStorageService = fileStorageService;
		this.ossMapper = ossMapper;
	}

	/** 当前启用的存储平台名；无启用配置或非本地类型时回退默认平台 */
	public String activePlatform() {
		SysOss oss = ossMapper.selectOneByQuery(QueryWrapper.create().eq("status", 1).orderBy("id", false));
		if (oss == null || !"local".equalsIgnoreCase(oss.getCategory()) || oss.getStoragePath() == null) {
			return fileStorageService.getDefaultPlatform();
		}
		register(oss);
		return oss.getOssCode();
	}

	/** 按当前启用配置重建本地存储平台：先移除同名旧注册再追加，确保配置变更即时生效 */
	private synchronized void register(SysOss oss) {
		fileStorageService.getFileStorageList().removeIf(fs -> oss.getOssCode().equals(fs.getPlatform()));
		FileStorageProperties.LocalPlusConfig config = new FileStorageProperties.LocalPlusConfig();
		config.setPlatform(oss.getOssCode());
		config.setStoragePath(oss.getStoragePath());
		config.setBasePath("");
		config.setDomain(oss.getDomain() == null ? "" : oss.getDomain());
		fileStorageService.getFileStorageList().add(new LocalPlusFileStorage(config));
	}
}
