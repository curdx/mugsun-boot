package com.mugsun.boot.system.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import com.mugsun.boot.system.entity.SysAttach;
import com.mugsun.boot.system.mapper.SysAttachMapper;
import com.mugsun.core.tool.api.R;
import com.mybatisflex.core.query.QueryWrapper;
import org.dromara.x.file.storage.core.FileInfo;
import org.dromara.x.file.storage.core.FileStorageService;
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

	private final FileStorageService fileStorageService;
	private final SysAttachMapper attachMapper;

	public FileController(FileStorageService fileStorageService, SysAttachMapper attachMapper) {
		this.fileStorageService = fileStorageService;
		this.attachMapper = attachMapper;
	}

	/** 上传文件并登记附件 */
	@PostMapping("/upload")
	public R<SysAttach> upload(@RequestParam("file") MultipartFile file) {
		FileInfo info = fileStorageService.of(file).upload();
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
	public R<Void> remove(@RequestParam Long id) {
		SysAttach attach = attachMapper.selectOneById(id);
		if (attach != null && attach.getUrl() != null) {
			fileStorageService.delete(attach.getUrl());
		}
		attachMapper.deleteById(id);
		return R.success("删除成功");
	}
}
