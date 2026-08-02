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
	private final com.mugsun.boot.log.OperationLogService operationLogService;

	public SysOperLogController(SysOperLogMapper operLogMapper,
								com.mugsun.boot.log.OperationLogService operationLogService) {
		this.operLogMapper = operLogMapper;
		this.operationLogService = operationLogService;
	}

	@GetMapping("/page")
	@cn.dev33.satoken.annotation.SaCheckPermission("sys:oper-log:list")
	public R<Page<SysOperLog>> page(@RequestParam(defaultValue = "1") long pageNum,
									@RequestParam(defaultValue = "10") long pageSize,
									@RequestParam(required = false) Integer status) {
		QueryWrapper query = QueryWrapper.create().orderBy("id", false);
		if (status != null) {
			query.and("status = ?", status);
		}
		return R.data(operLogMapper.paginate(pageNum, Math.min(pageSize, 500), query));
	}

	@GetMapping("/detail")
	@cn.dev33.satoken.annotation.SaCheckPermission("sys:oper-log:list")
	public R<SysOperLog> detail(@RequestParam Long id) {
		return R.data(operLogMapper.selectOneById(id));
	}

	/** 审计完整性验签：重算哈希链 + 验证 SM2 签名，检出篡改并定位首个被篡改记录。
	 *  仅管理员可触发（防任意用户反复触发全量验签 DoS）；limit&gt;0 只校最近 N 条（有界内存）。 */
	@cn.dev33.satoken.annotation.SaCheckPermission("sys:oper-log:verify")
	@GetMapping("/verify")
	public R<java.util.Map<String, Object>> verify(@RequestParam(required = false) Integer limit) {
		return R.data(operationLogService.verify(limit));
	}
}
