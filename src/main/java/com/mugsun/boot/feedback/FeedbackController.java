package com.mugsun.boot.feedback;

import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.dev33.satoken.stp.StpUtil;
import com.mugsun.boot.feedback.entity.SysFeedback;
import com.mugsun.boot.feedback.mapper.SysFeedbackMapper;
import com.mugsun.core.tool.api.R;
import com.mugsun.core.tool.exception.ServiceException;
import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.query.QueryWrapper;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 意见反馈：任意登录用户提交（可带附件），管理员查看与处理。
 */
@RestController
@RequestMapping("/system/feedback")
@SaCheckLogin
public class FeedbackController {

	private final SysFeedbackMapper feedbackMapper;

	public FeedbackController(SysFeedbackMapper feedbackMapper) {
		this.feedbackMapper = feedbackMapper;
	}

	/** 用户提交反馈 */
	@PostMapping("/submit")
	public R<Void> submit(@RequestBody SysFeedback feedback) {
		if (feedback.getContent() == null || feedback.getContent().isBlank()) {
			throw new ServiceException("反馈内容不能为空");
		}
		// 提交人与状态由服务端定，忽略客户端 id
		feedback.setId(null);
		feedback.setUserId(StpUtil.getLoginIdAsLong());
		feedback.setStatus(0);
		feedbackMapper.insertSelective(feedback);
		return R.success("提交成功");
	}

	/** 后台分页列表 */
	@GetMapping("/page")
	@SaCheckPermission("sys:feedback:manage")
	public R<Page<SysFeedback>> page(@RequestParam(defaultValue = "1") long pageNum,
									 @RequestParam(defaultValue = "10") long pageSize,
									 @RequestParam(required = false) Integer status) {
		QueryWrapper query = QueryWrapper.create().orderBy("id", false);
		if (status != null) {
			query.eq("status", status);
		}
		return R.data(feedbackMapper.paginate(pageNum, pageSize, query));
	}

	/** 切换处理状态 */
	@PostMapping("/status/{id}")
	@SaCheckPermission("sys:feedback:manage")
	public R<Void> status(@PathVariable Long id) {
		SysFeedback feedback = feedbackMapper.selectOneById(id);
		if (feedback != null) {
			feedback.setStatus(feedback.getStatus() != null && feedback.getStatus() == 1 ? 0 : 1);
			feedbackMapper.update(feedback);
		}
		return R.success("操作成功");
	}

	@PostMapping("/remove")
	@SaCheckPermission("sys:feedback:manage")
	public R<Void> remove(@RequestBody List<Long> ids) {
		feedbackMapper.deleteBatchByIds(ids);
		return R.success("删除成功");
	}
}
