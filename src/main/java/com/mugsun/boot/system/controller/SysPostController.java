package com.mugsun.boot.system.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import com.mugsun.boot.system.entity.SysPost;
import com.mugsun.boot.system.mapper.SysPostMapper;
import com.mugsun.core.tool.api.R;
import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.query.QueryWrapper;
import org.springframework.web.bind.annotation.*;

/**
 * 岗位管理
 */
@RestController
@RequestMapping("/system/post")
@SaCheckLogin
public class SysPostController {

	private final SysPostMapper postMapper;

	public SysPostController(SysPostMapper postMapper) {
		this.postMapper = postMapper;
	}

	@GetMapping("/page")
	public R<Page<SysPost>> page(@RequestParam(defaultValue = "1") long pageNum,
								 @RequestParam(defaultValue = "10") long pageSize) {
		return R.data(postMapper.paginate(pageNum, pageSize, QueryWrapper.create().orderBy("sort", true)));
	}

	@GetMapping("/detail")
	public R<SysPost> detail(@RequestParam Long id) {
		return R.data(postMapper.selectOneById(id));
	}

	@PostMapping("/submit")
	public R<Void> submit(@RequestBody SysPost post) {
		if (post.getId() == null) {
			postMapper.insert(post);
		} else {
			postMapper.update(post);
		}
		return R.success("操作成功");
	}

	@PostMapping("/remove")
	public R<Void> remove(@RequestParam Long id) {
		postMapper.deleteById(id);
		return R.success("删除成功");
	}
}
