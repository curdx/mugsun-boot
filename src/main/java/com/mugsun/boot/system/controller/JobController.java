package com.mugsun.boot.system.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.annotation.SaCheckPermission;
import com.mugsun.core.tool.api.R;
import org.springframework.web.bind.annotation.*;
import tech.powerjob.client.PowerJobClient;
import tech.powerjob.common.enums.ExecuteType;
import tech.powerjob.common.enums.ProcessorType;
import tech.powerjob.common.enums.TimeExpressionType;
import tech.powerjob.common.request.http.SaveJobInfoRequest;
import tech.powerjob.common.request.query.InstancePageQuery;
import tech.powerjob.common.response.InstanceInfoDTO;
import tech.powerjob.common.response.JobInfoDTO;

import java.util.List;

/**
 * 定时任务管理：代理 PowerJob OpenAPI，任务 CRUD / 触发 / 启停 / 执行日志。
 * 处理器固定内置 DemoProcessor，任务以「名称 + 时间表达式」维度维护。
 */
@RestController
@RequestMapping("/system/job")
@SaCheckLogin
public class JobController {

	private static final String PROCESSOR = "com.mugsun.boot.job.DemoProcessor";

	/** PowerJob OpenAPI 地址/凭据外置（默认本地联调值，生产经环境变量覆盖） */
	@org.springframework.beans.factory.annotation.Value("${powerjob.openapi.address:${powerjob.worker.server-address:127.0.0.1:7700}}")
	private String serverAddress;

	@org.springframework.beans.factory.annotation.Value("${powerjob.openapi.app-name:${powerjob.worker.app-name:mugsun}}")
	private String appName;

	@org.springframework.beans.factory.annotation.Value("${powerjob.openapi.password:${POWERJOB_OPENAPI_PASSWORD:mugsun}}")
	private String password;

	private volatile PowerJobClient client;

	private PowerJobClient client() {
		if (client == null) {
			synchronized (this) {
				if (client == null) {
					client = new PowerJobClient(serverAddress, appName, password);
				}
			}
		}
		return client;
	}

	/** 任务列表（过滤已删除 status=99） */
	@GetMapping("/list")
	public R<List<JobInfoDTO>> list() {
		List<JobInfoDTO> jobs = client().fetchAllJob().getData();
		return R.data(jobs == null ? List.of()
			: jobs.stream().filter(j -> j.getStatus() == null || j.getStatus() != 99).toList());
	}

	/** 新建 / 更新任务 */
	@SaCheckPermission("sys:job:save")
	@PostMapping("/save")
	public R<Long> save(@RequestBody JobParam param) {
		SaveJobInfoRequest req = new SaveJobInfoRequest();
		if (param.id() != null) {
			req.setId(param.id());
		}
		req.setJobName(param.jobName());
		req.setJobDescription(param.jobDescription());
		req.setProcessorType(ProcessorType.BUILT_IN);
		req.setProcessorInfo(PROCESSOR);
		req.setExecuteType(ExecuteType.STANDALONE);
		TimeExpressionType type = TimeExpressionType.valueOf(
			param.timeExpressionType() == null || param.timeExpressionType().isBlank() ? "API" : param.timeExpressionType());
		req.setTimeExpressionType(type);
		if (type == TimeExpressionType.CRON) {
			req.setTimeExpression(param.timeExpression());
		}
		return R.data(client().saveJob(req).getData());
	}

	/** 立即执行一次，返回执行实例ID */
	@SaCheckPermission("sys:job:run")
	@PostMapping("/run/{jobId}")
	public R<Long> run(@PathVariable Long jobId) {
		return R.data(client().runJob(jobId, "手动触发", 0L).getData());
	}

	/** 启用任务 */
	@SaCheckPermission("sys:job:edit")
	@PostMapping("/enable/{jobId}")
	public R<Void> enable(@PathVariable Long jobId) {
		client().enableJob(jobId);
		return R.success("已启用");
	}

	/** 停用任务 */
	@SaCheckPermission("sys:job:edit")
	@PostMapping("/disable/{jobId}")
	public R<Void> disable(@PathVariable Long jobId) {
		client().disableJob(jobId);
		return R.success("已停用");
	}

	/** 删除任务 */
	@SaCheckPermission("sys:job:remove")
	@PostMapping("/delete/{jobId}")
	public R<Void> delete(@PathVariable Long jobId) {
		client().deleteJob(jobId);
		return R.success("删除成功");
	}

	/** 任务执行日志（最近实例） */
	@GetMapping("/instances")
	public R<List<InstanceInfoDTO>> instances(@RequestParam Long jobId) {
		InstancePageQuery query = new InstancePageQuery();
		query.setJobIdEq(jobId);
		query.setIndex(0);
		query.setPageSize(20);
		return R.data(client().queryInstanceInfo(query).getData().getData());
	}

	/** 查询执行实例状态（3 运行中 / 5 成功 / 4 失败） */
	@GetMapping("/status")
	public R<Integer> status(@RequestParam Long instanceId) {
		return R.data(client().fetchInstanceStatus(instanceId).getData());
	}

	/** 任务参数：id 为空则新建，时间表达式类型 API（手动）/ CRON（定时） */
	public record JobParam(Long id, String jobName, String jobDescription,
						   String timeExpressionType, String timeExpression) {
	}
}
