package com.mugsun.boot.system.service;

import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.util.IdUtil;
import com.mugsun.core.tool.exception.ServiceException;
import com.mybatisflex.core.row.Db;
import com.mybatisflex.core.row.Row;
import org.dromara.warm.flow.core.FlowEngine;
import org.dromara.warm.flow.core.dto.FlowParams;
import org.dromara.warm.flow.core.entity.Instance;
import org.dromara.warm.flow.core.entity.Task;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 工作流办理服务：审批动作**一律走 warm-flow 引擎 API**（非裸 SQL），承载权限/状态守卫。
 * <p>动作集：通过 skip(PASS) / 退回上一步 rejectLast / 退回指定节点 skip(REJECT)+nodeCode /
 * 撤回 revoke(发起人) / 作废 termination / 转办·委派·加签·减签 单入口 taskOperation / 抄送（应用层，flow_user type='C'）。
 * <p>驳回语义修正：退回走 rejectLast（实例存活、回退上一步），**不再误用 termination（终止作废）**。
 */
@Service
public class FlowService {

	/** 抄送用户类型（warm-flow 引擎不识别，仅应用层读写；引擎办理人为 1/2/3） */
	private static final String USER_TYPE_COPY = "C";

	// ==================== 列表读取 ====================

	/** 流程定义列表 */
	public List<Row> definitions() {
		return Db.selectListBySql(
			"select id as \"id\", flow_code as \"flowCode\", flow_name as \"flowName\", version as \"version\", "
				+ "is_publish as \"isPublish\", create_time as \"createTime\" "
				+ "from flow_definition where coalesce(del_flag, '0') <> '1' order by id desc");
	}

	/** 我的待办：按当前用户的角色码/用户标识匹配任务办理人 */
	public List<Row> myTodo() {
		List<String> flags = userFlags();
		String in = flags.stream().map(f -> "?").collect(Collectors.joining(","));
		return Db.selectListBySql(
			"select t.id as \"taskId\", t.instance_id as \"instanceId\", t.node_code as \"nodeCode\", "
				+ "t.node_name as \"nodeName\", i.business_id as \"businessId\", i.flow_status as \"flowStatus\", "
				+ "d.flow_name as \"flowName\", t.create_time as \"createTime\" "
				+ "from flow_task t "
				+ "join flow_user u on u.associated = t.id and u.type in ('1','2','3') and coalesce(u.del_flag, '0') <> '1' "
				+ "join flow_instance i on i.id = t.instance_id "
				+ "join flow_definition d on d.id = t.definition_id "
				+ "where coalesce(t.del_flag, '0') <> '1' and u.processed_by in (" + in + ") order by t.id desc",
			flags.toArray());
	}

	/** 我的抄送：应用层 flow_user type='C'（associated = 实例 id） */
	public List<Row> myCopy() {
		return Db.selectListBySql(
			"select i.id as \"instanceId\", i.business_id as \"businessId\", i.flow_status as \"flowStatus\", "
				+ "d.flow_name as \"flowName\", u.create_time as \"createTime\" "
				+ "from flow_user u "
				+ "join flow_instance i on i.id = u.associated "
				+ "join flow_definition d on d.id = i.definition_id "
				+ "where u.type = '" + USER_TYPE_COPY + "' and coalesce(u.del_flag,'0') <> '1' and u.processed_by = ? "
				+ "order by u.id desc", StpUtil.getLoginIdAsString());
	}

	/** 实例历史流转（进度时间线） */
	public List<Row> history(Long instanceId) {
		return Db.selectListBySql(
			"select node_code as \"nodeCode\", node_name as \"nodeName\", approver as \"approver\", "
				+ "skip_type as \"skipType\", flow_status as \"flowStatus\", message as \"message\", "
				+ "create_time as \"createTime\" "
				+ "from flow_his_task where instance_id = ? and coalesce(del_flag, '0') <> '1' order by id asc", instanceId);
	}

	/** 实例可退回的历史审批节点（供“退回指定节点”选择；排除开始/结束/网关） */
	public List<Row> backNodes(Long instanceId) {
		return Db.selectListBySql(
			"select distinct node_code as \"nodeCode\", node_name as \"nodeName\" "
				+ "from flow_his_task where instance_id = ? and node_type = 1 and coalesce(del_flag,'0') <> '1' "
				+ "order by node_code", instanceId);
	}

	// ==================== 审批动作（引擎 API） ====================

	/** 通过 */
	public String pass(Long taskId, String message) {
		requireTask(taskId);
		Instance ins = FlowEngine.taskService().skip(taskId,
			base().skipType("PASS").message(text(message, "同意")));
		return ins.getFlowStatus();
	}

	/** 退回上一步（实例存活、回退，非终止作废） */
	public String rejectLast(Long taskId, String message) {
		requireTask(taskId);
		Instance ins = FlowEngine.taskService().rejectLast(taskId,
			base().message(text(message, "退回上一步")));
		return ins.getFlowStatus();
	}

	/** 退回指定历史节点 */
	public String rejectToNode(Long taskId, String nodeCode, String message) {
		requireTask(taskId);
		if (nodeCode == null || nodeCode.isBlank()) {
			throw new ServiceException("请选择退回节点");
		}
		Instance ins = FlowEngine.taskService().skip(taskId,
			base().skipType("REJECT").nodeCode(nodeCode).message(text(message, "退回")));
		return ins.getFlowStatus();
	}

	/** 撤回（仅发起人，引擎校验 handler == 实例 createBy） */
	public String revoke(Long instanceId, String message) {
		Instance ins = FlowEngine.insService().getById(instanceId);
		if (ins == null) {
			throw new ServiceException("流程实例不存在");
		}
		Instance r = FlowEngine.taskService().revoke(instanceId,
			FlowParams.build().handler(uid()).message(text(message, "撤回")));
		return r.getFlowStatus();
	}

	/** 作废（终止整个实例，语义独立于退回） */
	public String terminate(Long taskId, String message) {
		requireTask(taskId);
		Instance ins = FlowEngine.taskService().termination(taskId,
			base().message(text(message, "作废")));
		return ins.getFlowStatus();
	}

	/** 转办/委派/加签/减签 单入口：目标办理人 handlers（减签为要移除的人） */
	public void taskOperation(Long taskId, String op, List<String> handlers, String message) {
		requireTask(taskId);
		if (handlers == null || handlers.isEmpty()) {
			throw new ServiceException("请选择目标办理人");
		}
		FlowParams p = base().message(text(message, opName(op)));
		switch (op) {
			case "transfer" -> FlowEngine.taskService().transfer(taskId, p.addHandlers(handlers));
			case "depute" -> FlowEngine.taskService().depute(taskId, p.addHandlers(handlers));
			case "addSignature" -> FlowEngine.taskService().addSignature(taskId, p.addHandlers(handlers));
			case "reductionSignature" -> FlowEngine.taskService().reductionSignature(taskId, p.reductionHandlers(handlers));
			default -> throw new ServiceException("不支持的操作：" + op);
		}
	}

	/** 抄送（应用层）：向被抄送人写 flow_user type='C'，关联实例 */
	public void copyTo(Long taskId, List<String> userIds) {
		Task task = requireTask(taskId);
		if (userIds == null || userIds.isEmpty()) {
			return;
		}
		Long instanceId = task.getInstanceId();
		for (String u : userIds) {
			Db.updateBySql(
				"insert into flow_user(id, type, processed_by, associated, create_time, del_flag) "
					+ "values (?, ?, ?, ?, now(), '0')",
				IdUtil.getSnowflakeNextId(), USER_TYPE_COPY, u, instanceId);
		}
	}

	// ==================== 守卫 / 工具 ====================

	/** 校验任务存在且待办（已办任务已流转至历史，flow_task 查无即非法流转） */
	private Task requireTask(Long taskId) {
		Task t = FlowEngine.taskService().getById(taskId);
		if (t == null) {
			throw new ServiceException("任务不存在或已处理");
		}
		return t;
	}

	/** 引擎办理参数基座：当前办理人 + 权限标识集（ignore 默认 false → 引擎校验办理权限） */
	private FlowParams base() {
		return FlowParams.build().handler(uid()).permissionFlag(userFlags());
	}

	private String uid() {
		return StpUtil.getLoginIdAsString();
	}

	/** 当前用户的办理人标识集合：角色码 + 用户 id */
	private List<String> userFlags() {
		List<String> flags = new ArrayList<>();
		Db.selectListBySql(
			"select r.role_code as \"roleCode\" from sys_user_role ur "
				+ "join sys_role r on r.id = ur.role_id "
				+ "where ur.user_id = ? and ur.is_deleted = 0", StpUtil.getLoginIdAsLong())
			.forEach(row -> flags.add(String.valueOf(row.getString("roleCode"))));
		flags.add(uid());
		return flags;
	}

	private String text(String message, String fallback) {
		return message == null || message.isBlank() ? fallback : message;
	}

	private String opName(String op) {
		return switch (op) {
			case "transfer" -> "转办";
			case "depute" -> "委派";
			case "addSignature" -> "加签";
			case "reductionSignature" -> "减签";
			default -> op;
		};
	}
}
