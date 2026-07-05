package com.mugsun.boot.system.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import com.mugsun.boot.system.entity.SysOss;
import com.mugsun.boot.system.mapper.SysOssMapper;
import com.mugsun.core.tool.api.R;
import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.query.QueryWrapper;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 对象存储配置管理
 */
@RestController
@RequestMapping("/system/oss")
@SaCheckLogin
public class SysOssController {

	private final SysOssMapper ossMapper;

	public SysOssController(SysOssMapper ossMapper) {
		this.ossMapper = ossMapper;
	}

	@GetMapping("/page")
	public R<Page<SysOss>> page(@RequestParam(defaultValue = "1") long pageNum,
								@RequestParam(defaultValue = "10") long pageSize) {
		return R.data(ossMapper.paginate(pageNum, pageSize, QueryWrapper.create().orderBy("id", false)));
	}

	@GetMapping("/detail")
	public R<SysOss> detail(@RequestParam Long id) {
		return R.data(ossMapper.selectOneById(id));
	}

	@PostMapping("/submit")
	public R<Void> submit(@RequestBody SysOss oss) {
		if (oss.getId() == null) {
			ossMapper.insertSelective(oss);
		} else {
			ossMapper.update(oss);
		}
		return R.success("操作成功");
	}

	@PostMapping("/remove")
	public R<Void> remove(@RequestBody List<Long> ids) {
		ids.forEach(ossMapper::deleteById);
		return R.success("删除成功");
	}

	/** 启用指定配置：同租户互斥，其余置禁用 */
	@PostMapping("/enable/{id}")
	public R<Void> enable(@PathVariable Long id) {
		ossMapper.selectListByQuery(QueryWrapper.create().eq("status", 1)).forEach(o -> {
			o.setStatus(0);
			ossMapper.update(o);
		});
		SysOss target = ossMapper.selectOneById(id);
		if (target != null) {
			target.setStatus(1);
			ossMapper.update(target);
		}
		return R.success("已启用");
	}
}
