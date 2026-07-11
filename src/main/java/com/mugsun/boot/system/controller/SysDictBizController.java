package com.mugsun.boot.system.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.annotation.SaCheckPermission;
import com.mugsun.boot.system.entity.SysDictBiz;
import com.mugsun.boot.system.mapper.SysDictBizMapper;
import com.mugsun.core.tool.api.R;
import com.mugsun.core.tool.tree.TreeUtil;
import com.mybatisflex.core.query.QueryWrapper;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 业务字典管理（租户隔离于 G8 多租户阶段接入）
 */
@RestController
@RequestMapping("/system/dict-biz")
@SaCheckLogin
public class SysDictBizController {

	private final SysDictBizMapper dictBizMapper;

	public SysDictBizController(SysDictBizMapper dictBizMapper) {
		this.dictBizMapper = dictBizMapper;
	}

	@GetMapping("/tree")
	public R<List<SysDictBiz>> tree() {
		List<SysDictBiz> all = dictBizMapper.selectListByQuery(QueryWrapper.create().orderBy("sort", true));
		return R.data(TreeUtil.build(all, 0L));
	}

	@GetMapping("/dictionary")
	public R<List<SysDictBiz>> dictionary(@RequestParam String code) {
		return R.data(dictBizMapper.selectListByQuery(QueryWrapper.create()
			.eq("code", code).ne("parent_id", 0L).orderBy("sort", true)));
	}

	@GetMapping("/detail")
	public R<SysDictBiz> detail(@RequestParam Long id) {
		return R.data(dictBizMapper.selectOneById(id));
	}

	@SaCheckPermission("sys:dict-biz:save")
	@PostMapping("/submit")
	public R<Void> submit(@RequestBody SysDictBiz dict) {
		// 字典类型无键值：dictKey 兜底空串，避免 NOT NULL 约束报错
		if (dict.getDictKey() == null) {
			dict.setDictKey("");
		}
		if (dict.getId() == null) {
			dictBizMapper.insertSelective(dict);
		} else {
			dictBizMapper.update(dict);
		}
		return R.success("操作成功");
	}

	@SaCheckPermission("sys:dict-biz:remove")
	@PostMapping("/remove")
	public R<Void> remove(@RequestBody List<Long> ids) {
		dictBizMapper.deleteBatchByIds(ids);
		return R.success("删除成功");
	}
}
