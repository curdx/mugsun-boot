package com.mugsun.boot.system.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import com.mugsun.boot.system.entity.SysDict;
import com.mugsun.boot.system.mapper.SysDictMapper;
import com.mugsun.boot.system.service.DictService;
import com.mugsun.core.tool.api.R;
import com.mugsun.core.tool.tree.TreeUtil;
import com.mybatisflex.core.query.QueryWrapper;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 系统字典管理
 */
@RestController
@RequestMapping("/system/dict")
@SaCheckLogin
public class SysDictController {

	private final SysDictMapper dictMapper;
	private final DictService dictService;

	public SysDictController(SysDictMapper dictMapper, DictService dictService) {
		this.dictMapper = dictMapper;
		this.dictService = dictService;
	}

	@GetMapping("/tree")
	public R<List<SysDict>> tree() {
		List<SysDict> all = dictMapper.selectListByQuery(QueryWrapper.create().orderBy("sort", true));
		return R.data(TreeUtil.build(all, 0L));
	}

	/** 按编码查字典项（走缓存） */
	@GetMapping("/dictionary")
	public R<List<SysDict>> dictionary(@RequestParam String code) {
		return R.data(dictService.listItems(code));
	}

	@GetMapping("/detail")
	public R<SysDict> detail(@RequestParam Long id) {
		return R.data(dictMapper.selectOneById(id));
	}

	@PostMapping("/submit")
	public R<Void> submit(@RequestBody SysDict dict) {
		// 字典类型无键值：dictKey 兜底空串，避免 NOT NULL 约束报错
		if (dict.getDictKey() == null) {
			dict.setDictKey("");
		}
		if (dict.getId() == null) {
			dictMapper.insertSelective(dict);
		} else {
			dictMapper.update(dict);
		}
		dictService.evict(dict.getCode());
		return R.success("操作成功");
	}

	@PostMapping("/remove")
	public R<Void> remove(@RequestBody List<Long> ids) {
		ids.forEach(id -> {
			SysDict dict = dictMapper.selectOneById(id);
			dictMapper.deleteById(id);
			if (dict != null) {
				dictService.evict(dict.getCode());
			}
		});
		return R.success("删除成功");
	}
}
