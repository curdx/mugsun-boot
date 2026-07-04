package com.mugsun.boot.system.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import com.mugsun.core.tool.api.R;
import org.springframework.web.bind.annotation.*;
import tech.powerjob.client.PowerJobClient;
import tech.powerjob.common.enums.ExecuteType;
import tech.powerjob.common.enums.ProcessorType;
import tech.powerjob.common.enums.TimeExpressionType;
import tech.powerjob.common.request.http.SaveJobInfoRequest;

/**
 * 任务调度：通过 PowerJob OpenAPI 新建任务并触发执行。
 */
@RestController
@RequestMapping("/system/job")
@SaCheckLogin
public class JobController {

	private PowerJobClient client() {
		return new PowerJobClient("127.0.0.1:7700", "mugsun", "mugsun");
	}

	/** 新建示例任务并立即触发一次，返回执行实例ID */
	@PostMapping("/demo")
	public R<Long> demo() {
		PowerJobClient client = client();
		SaveJobInfoRequest req = new SaveJobInfoRequest();
		req.setJobName("mugsun-demo-job");
		req.setProcessorType(ProcessorType.BUILT_IN);
		req.setProcessorInfo("com.mugsun.boot.job.DemoProcessor");
		req.setExecuteType(ExecuteType.STANDALONE);
		req.setTimeExpressionType(TimeExpressionType.API);
		Long jobId = client.saveJob(req).getData();
		Long instanceId = client.runJob(jobId, "mugsun 手动触发", 0L).getData();
		return R.data(instanceId);
	}

	/** 查询执行实例状态（PowerJob InstanceStatus：3 运行中 / 5 成功 / 4 失败） */
	@GetMapping("/status")
	public R<Integer> status(@RequestParam Long instanceId) {
		return R.data(client().fetchInstanceStatus(instanceId).getData());
	}
}
