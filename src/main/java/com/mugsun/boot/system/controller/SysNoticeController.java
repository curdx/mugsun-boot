package com.mugsun.boot.system.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import com.mugsun.boot.system.entity.SysNotice;
import com.mugsun.boot.system.mapper.SysNoticeMapper;
import com.mugsun.core.tool.api.R;
import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.query.QueryWrapper;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 通知公告管理
 */
@RestController
@RequestMapping("/system/notice")
@SaCheckLogin
public class SysNoticeController {

	private final SysNoticeMapper noticeMapper;

	public SysNoticeController(SysNoticeMapper noticeMapper) {
		this.noticeMapper = noticeMapper;
	}

	/** 分页：置顶优先，再按时间倒序 */
	@GetMapping("/page")
	public R<Page<SysNotice>> page(@RequestParam(defaultValue = "1") long pageNum,
								   @RequestParam(defaultValue = "10") long pageSize) {
		return R.data(noticeMapper.paginate(pageNum, pageSize,
			QueryWrapper.create().orderBy("is_top", false).orderBy("id", false)));
	}

	/** 置顶公告列表 */
	@GetMapping("/top")
	public R<List<SysNotice>> top() {
		return R.data(noticeMapper.selectListByQuery(
			QueryWrapper.create().eq("is_top", 1).orderBy("id", false)));
	}

	@GetMapping("/detail")
	public R<SysNotice> detail(@RequestParam Long id) {
		return R.data(noticeMapper.selectOneById(id));
	}

	@PostMapping("/submit")
	public R<Void> submit(@RequestBody SysNotice notice) {
		if (notice.getId() == null) {
			noticeMapper.insertSelective(notice);
		} else {
			noticeMapper.update(notice);
		}
		return R.success("操作成功");
	}

	@PostMapping("/remove")
	public R<Void> remove(@RequestParam Long id) {
		noticeMapper.deleteById(id);
		return R.success("删除成功");
	}
}
