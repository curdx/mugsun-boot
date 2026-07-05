package com.mugsun.boot.system.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import com.mugsun.boot.system.entity.SysOperLog;
import com.mugsun.boot.system.mapper.SysOperLogMapper;
import com.mugsun.core.tool.api.R;
import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.query.QueryWrapper;
import org.springframework.web.bind.annotation.*;

/**
 * 操作日志查询（含错误日志：按 status 区分成功/失败）
 */
@RestController
@RequestMapping("/system/oper-log")
@SaCheckLogin
public class SysOperLogController {

	private final SysOperLogMapper operLogMapper;

	public SysOperLogController(SysOperLogMapper operLogMapper) {
		this.operLogMapper = operLogMapper;
	}

	@GetMapping("/page")
	public R<Page<SysOperLog>> page(@RequestParam(defaultValue = "1") long pageNum,
									@RequestParam(defaultValue = "10") long pageSize,
									@RequestParam(required = false) Integer status) {
		QueryWrapper query = QueryWrapper.create().orderBy("id", false);
		if (status != null) {
			query.and("status = ?", status);
		}
		return R.data(operLogMapper.paginate(pageNum, pageSize, query));
	}

	@GetMapping("/detail")
	public R<SysOperLog> detail(@RequestParam Long id) {
		return R.data(operLogMapper.selectOneById(id));
	}
}
