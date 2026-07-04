package com.mugsun.boot.system.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.stp.StpUtil;
import com.mugsun.core.tool.api.R;
import org.dromara.warm.flow.core.FlowEngine;
import org.dromara.warm.flow.core.dto.FlowParams;
import org.dromara.warm.flow.core.entity.Definition;
import org.dromara.warm.flow.core.entity.Instance;
import org.dromara.warm.flow.core.entity.Task;
import org.springframework.core.io.ClassPathResource;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * 工作流：部署请假流程并驱动发起 / 待办 / 办理 / 办结全链路。
 */
@RestController
@RequestMapping("/system/flow")
@SaCheckLogin
public class FlowController {

	/** 部署：导入流程定义 JSON 并发布 */
	@PostMapping("/deploy")
	public R<Long> deploy() throws Exception {
		String json = new String(new ClassPathResource("flow/leave.json").getInputStream()
			.readAllBytes(), StandardCharsets.UTF_8);
		Definition def = FlowEngine.defService().importJson(json);
		FlowEngine.defService().publish(def.getId());
		return R.data(def.getId());
	}

	/** 发起流程实例 */
	@PostMapping("/start")
	public R<Long> start(@RequestParam String businessId) {
		Instance ins = FlowEngine.insService().start(businessId,
			FlowParams.build().flowCode("leave").handler(StpUtil.getLoginIdAsString()));
		return R.data(ins.getId());
	}

	/** 查询实例待办任务 */
	@GetMapping("/todo")
	public R<List<Task>> todo(@RequestParam Long instanceId) {
		return R.data(FlowEngine.taskService().getByInsId(instanceId));
	}

	/** 办理任务（审批通过） */
	@PostMapping("/handle")
	public R<String> handle(@RequestParam Long taskId) {
		Instance ins = FlowEngine.taskService().skip(taskId,
			FlowParams.build().skipType("PASS").handler(StpUtil.getLoginIdAsString())
				.message("同意").ignore(true));
		return R.data(ins.getFlowStatus());
	}

	/** 查询实例（含流程状态） */
	@GetMapping("/instance")
	public R<Instance> instance(@RequestParam Long instanceId) {
		return R.data(FlowEngine.insService().getById(instanceId));
	}
}
