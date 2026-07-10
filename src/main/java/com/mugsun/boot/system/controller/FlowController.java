package com.mugsun.boot.system.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.stp.StpUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mugsun.core.tool.api.R;
import com.mugsun.core.tool.exception.ServiceException;
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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 工作流治理：流程定义部署/发起 + 待办工作台（我的待办 / 办理 / 驳回 / 历史进度）。
 * 动作走 warm-flow 引擎，列表读取用原生 SQL 直连 flow_* 表。
 */
@RestController
@RequestMapping("/system/flow")
@SaCheckLogin
public class FlowController {

	private final ObjectMapper objectMapper;

	public FlowController(ObjectMapper objectMapper) {
		this.objectMapper = objectMapper;
	}

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

	/**
	 * 图形设计部署：由结构化设计（流程码/名 + 审批节点[名称,审批角色]）生成 warm-flow 定义 JSON 并发布。
	 * 服务端组装 nodeList（开始→N个审批→结束）与坐标/连线，保证 JSON 合法。
	 */
	@PostMapping("/design")
	public R<Long> design(@RequestBody FlowDesign req) throws Exception {
		if (req.flowCode() == null || req.flowCode().isBlank() || req.flowName() == null || req.flowName().isBlank()) {
			throw new ServiceException("流程编码与名称不能为空");
		}
		if (req.nodes() == null || req.nodes().isEmpty()) {
			throw new ServiceException("至少设计一个审批节点");
		}
		String json = buildFlowJson(req);
		Definition def = FlowEngine.defService().importJson(json);
		FlowEngine.defService().publish(def.getId());
		return R.data(def.getId());
	}

	/** 发起流程实例（沿用请假流程码 leave，兼容旧入口） */
	@PostMapping("/start/{businessId}")
	public R<Long> start(@PathVariable String businessId) {
		Instance ins = FlowEngine.insService().start(businessId,
			FlowParams.build().flowCode("leave").handler(StpUtil.getLoginIdAsString()));
		return R.data(ins.getId());
	}

	/** 发起指定流程码的实例（设计流程发起用） */
	@PostMapping("/start-by/{flowCode}/{businessId}")
	public R<Long> startBy(@PathVariable String flowCode, @PathVariable String businessId) {
		Instance ins = FlowEngine.insService().start(businessId,
			FlowParams.build().flowCode(flowCode).handler(StpUtil.getLoginIdAsString()));
		return R.data(ins.getId());
	}

	/** 由设计生成 warm-flow 定义 JSON：开始→审批节点链→结束，横向布局 */
	private String buildFlowJson(FlowDesign req) throws Exception {
		List<Map<String, Object>> nodeList = new ArrayList<>();
		int n = req.nodes().size();
		int baseX = 100;
		int step = 160;
		int y = 200;
		// 各节点编码
		List<String> codes = new ArrayList<>();
		codes.add("start");
		for (int i = 0; i < n; i++) {
			codes.add("approve" + (i + 1));
		}
		codes.add("end");

		// 开始节点
		int x = baseX;
		nodeList.add(node(0, "start", "开始", null, x, y, edge("start", codes.get(1), "提交", x, y, x + step)));

		// 审批节点
		for (int i = 0; i < n; i++) {
			x = baseX + step * (i + 1);
			FlowDesignNode dn = req.nodes().get(i);
			String code = codes.get(i + 1);
			String next = codes.get(i + 2);
			nodeList.add(node(1, code, dn.name(), dn.role(), x, y, edge(code, next, "通过", x, y, x + step)));
		}

		// 结束节点
		x = baseX + step * (n + 1);
		Map<String, Object> end = node(2, "end", "结束", null, x, y, null);
		end.put("skipList", new ArrayList<>());
		nodeList.add(end);

		Map<String, Object> root = new LinkedHashMap<>();
		root.put("flowCode", req.flowCode());
		root.put("flowName", req.flowName());
		root.put("modelValue", "CLASSICS");
		root.put("version", "1");
		root.put("formCustom", "N");
		root.put("nodeList", nodeList);
		return objectMapper.writeValueAsString(root);
	}

	private Map<String, Object> node(int type, String code, String name, String permissionFlag,
									 int x, int y, Map<String, Object> edge) {
		Map<String, Object> node = new LinkedHashMap<>();
		node.put("nodeType", type);
		node.put("nodeCode", code);
		node.put("nodeName", name);
		if (permissionFlag != null) {
			node.put("permissionFlag", permissionFlag);
			node.put("nodeRatio", "0");
		}
		node.put("coordinate", x + "," + y + "|" + x + "," + y);
		if (edge != null) {
			List<Map<String, Object>> skips = new ArrayList<>();
			skips.add(edge);
			node.put("skipList", skips);
		}
		return node;
	}

	private Map<String, Object> edge(String from, String to, String name, int x, int y, int nextX) {
		Map<String, Object> e = new LinkedHashMap<>();
		e.put("nowNodeCode", from);
		e.put("nextNodeCode", to);
		e.put("skipName", name);
		e.put("skipType", "PASS");
		e.put("coordinate", (x + 20) + "," + y + ";" + (nextX - 20) + "," + y);
		return e;
	}

	/** 流程设计请求 */
	public record FlowDesign(String flowCode, String flowName, List<FlowDesignNode> nodes) {
	}

	/** 审批节点：名称 + 审批角色码 */
	public record FlowDesignNode(String name, String role) {
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
