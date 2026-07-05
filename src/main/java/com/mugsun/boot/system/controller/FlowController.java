package com.mugsun.boot.system.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.stp.StpUtil;
import com.mugsun.core.tool.api.R;
import com.mybatisflex.core.row.Db;
import com.mybatisflex.core.row.Row;
import org.dromara.warm.flow.core.FlowEngine;
import org.dromara.warm.flow.core.dto.FlowParams;
import org.dromara.warm.flow.core.entity.Definition;
import org.dromara.warm.flow.core.entity.Instance;
import org.springframework.core.io.ClassPathResource;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 工作流治理：流程定义部署/发起 + 待办工作台（我的待办 / 办理 / 驳回 / 历史进度）。
 * 动作走 warm-flow 引擎，列表读取用原生 SQL 直连 flow_* 表。
 */
@RestController
@RequestMapping("/system/flow")
@SaCheckLogin
public class FlowController {

	/** 流程定义列表 */
	@GetMapping("/definitions")
	public R<List<Row>> definitions() {
		return R.data(Db.selectListBySql(
			"select id as \"id\", flow_code as \"flowCode\", flow_name as \"flowName\", version as \"version\", "
				+ "is_publish as \"isPublish\", create_time as \"createTime\" "
				+ "from flow_definition where coalesce(del_flag, '0') <> '1' order by id desc"));
	}

	/** 部署：导入请假流程定义 JSON 并发布 */
	@PostMapping("/deploy")
	public R<Long> deploy() throws Exception {
		String json = new String(new ClassPathResource("flow/leave.json").getInputStream()
			.readAllBytes(), StandardCharsets.UTF_8);
		Definition def = FlowEngine.defService().importJson(json);
		FlowEngine.defService().publish(def.getId());
		return R.data(def.getId());
	}

	/** 发起流程实例 */
	@PostMapping("/start/{businessId}")
	public R<Long> start(@PathVariable String businessId) {
		Instance ins = FlowEngine.insService().start(businessId,
			FlowParams.build().flowCode("leave").handler(StpUtil.getLoginIdAsString()));
		return R.data(ins.getId());
	}

	/** 我的待办：按当前用户的角色码/用户标识匹配任务办理人 */
	@GetMapping("/my-todo")
	public R<List<Row>> myTodo() {
		List<String> flags = userFlags();
		String in = flags.stream().map(f -> "?").collect(Collectors.joining(","));
		Object[] args = flags.toArray();
		return R.data(Db.selectListBySql(
			"select t.id as \"taskId\", t.instance_id as \"instanceId\", t.node_name as \"nodeName\", "
				+ "i.business_id as \"businessId\", i.flow_status as \"flowStatus\", d.flow_name as \"flowName\", "
				+ "t.create_time as \"createTime\" "
				+ "from flow_task t "
				+ "join flow_user u on u.associated = t.id and u.type = '1' and coalesce(u.del_flag, '0') <> '1' "
				+ "join flow_instance i on i.id = t.instance_id "
				+ "join flow_definition d on d.id = t.definition_id "
				+ "where coalesce(t.del_flag, '0') <> '1' and u.processed_by in (" + in + ") order by t.id desc", args));
	}

	/** 办理任务（审批通过） */
	@PostMapping("/handle/{taskId}")
	public R<String> handle(@PathVariable Long taskId) {
		Instance ins = FlowEngine.taskService().skip(taskId,
			FlowParams.build().skipType("PASS").handler(StpUtil.getLoginIdAsString())
				.message("同意").ignore(true));
		return R.data(ins.getFlowStatus());
	}

	/** 驳回任务（终止实例） */
	@PostMapping("/reject/{taskId}")
	public R<String> reject(@PathVariable Long taskId) {
		Instance ins = FlowEngine.taskService().termination(taskId,
			FlowParams.build().handler(StpUtil.getLoginIdAsString()).message("驳回").ignore(true));
		return R.data(ins.getFlowStatus());
	}

	/** 实例历史流转（进度时间线） */
	@GetMapping("/history")
	public R<List<Row>> history(@RequestParam Long instanceId) {
		return R.data(Db.selectListBySql(
			"select node_name as \"nodeName\", approver as \"approver\", skip_type as \"skipType\", "
				+ "flow_status as \"flowStatus\", message as \"message\", create_time as \"createTime\" "
				+ "from flow_his_task where instance_id = ? and coalesce(del_flag, '0') <> '1' order by id asc", instanceId));
	}

	/** 查询实例（含流程状态） */
	@GetMapping("/instance")
	public R<Instance> instance(@RequestParam Long instanceId) {
		return R.data(FlowEngine.insService().getById(instanceId));
	}

	/** 当前用户的办理人标识集合：角色码 + 用户 id */
	private List<String> userFlags() {
		List<String> flags = new ArrayList<>();
		Db.selectListBySql(
			"select r.role_code as \"roleCode\" from sys_user_role ur "
				+ "join sys_role r on r.id = ur.role_id "
				+ "where ur.user_id = ? and ur.is_deleted = 0", StpUtil.getLoginIdAsLong())
			.forEach(row -> flags.add(String.valueOf(row.getString("roleCode"))));
		flags.add(StpUtil.getLoginIdAsString());
		return flags;
	}
}
