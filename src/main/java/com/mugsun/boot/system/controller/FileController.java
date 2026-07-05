package com.mugsun.boot.system.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import com.mugsun.boot.system.entity.SysAttach;
import com.mugsun.boot.system.mapper.SysAttachMapper;
import com.mugsun.core.tool.api.R;
import com.mybatisflex.core.query.QueryWrapper;
import org.dromara.x.file.storage.core.FileInfo;
import org.dromara.x.file.storage.core.FileStorageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 文件上传与附件登记
 */
@RestController
@RequestMapping("/system/file")
@SaCheckLogin
public class FileController {

	private static final Logger log = LoggerFactory.getLogger(FileController.class);

	private final FileStorageService fileStorageService;
	private final SysAttachMapper attachMapper;
	private final com.mugsun.boot.system.service.OssService ossService;

	public FileController(FileStorageService fileStorageService, SysAttachMapper attachMapper,
						  com.mugsun.boot.system.service.OssService ossService) {
		this.fileStorageService = fileStorageService;
		this.attachMapper = attachMapper;
		this.ossService = ossService;
	}

	/** 上传文件并登记附件（落到当前启用的存储配置对应平台） */
	@PostMapping("/upload")
	public R<SysAttach> upload(@RequestParam("file") MultipartFile file) {
		FileInfo info = fileStorageService.of(file).setPlatform(ossService.activePlatform()).upload();
		SysAttach attach = new SysAttach();
		attach.setName(info.getOriginalFilename());
		attach.setUrl(info.getUrl());
		attach.setPath(info.getPath());
		attach.setFilename(info.getFilename());
		attach.setExt(info.getExt());
		attach.setContentType(info.getContentType());
		attach.setSize(info.getSize());
		attach.setPlatform(info.getPlatform());
		attachMapper.insertSelective(attach);
		return R.data(attach);
	}

	@GetMapping("/list")
	public R<List<SysAttach>> list() {
		return R.data(attachMapper.selectListByQuery(QueryWrapper.create().orderBy("id", false)));
	}

	@PostMapping("/remove")
	public R<Void> remove(@RequestBody List<Long> ids) {
		for (Long id : ids) {
			SysAttach attach = attachMapper.selectOneById(id);
			if (attach != null && attach.getPath() != null) {
				// 物理删除尽力而为：未实现 FileRecorder 时按存储信息构造 FileInfo 删除，失败不阻塞登记删除
				try {
					FileInfo fileInfo = new FileInfo();
					fileInfo.setPlatform(attach.getPlatform());
					fileInfo.setBasePath("");
					fileInfo.setPath(attach.getPath());
					fileInfo.setFilename(attach.getFilename());
					fileInfo.setUrl(attach.getUrl());
					fileStorageService.delete(fileInfo);
				} catch (Exception e) {
					log.warn("附件物理删除失败（仅移除登记）id={}, url={}: {}", id, attach.getUrl(), e.getMessage());
				}
			}
			attachMapper.deleteById(id);
		}
		return R.success("删除成功");
	}
}
