package com.mugsun.boot.system.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import com.mugsun.boot.common.repeat.RepeatSubmit;
import com.mugsun.core.tool.api.R;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 防重复提交演示：同一用户 3 秒内重复提交（相同参数）将被拦截。
 */
@RestController
@RequestMapping("/system/repeat")
@SaCheckLogin
public class RepeatDemoController {

	@RepeatSubmit(interval = 3000)
	@PostMapping("/submit")
	public R<String> submit(@RequestBody(required = false) Map<String, Object> body) {
		return R.data("提交成功 @ " + LocalDateTime.now());
	}
}
