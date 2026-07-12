package com.mugsun.boot.system.service;

import com.mugsun.boot.common.constant.FlowConstants;
import org.dromara.warm.flow.core.entity.Instance;
import org.dromara.warm.flow.core.entity.Task;
import org.dromara.warm.flow.core.listener.GlobalListener;
import org.dromara.warm.flow.core.listener.ListenerVariable;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * warm-flow 全局监听器（单例 @Component，引擎自动装配）：在 assignment 阶段（flow_user 落库前、含实例上下文）
 * 解析需上下文的候选人占位并做兜底。
 * <ul><li>{@code initiator} → 实例发起人 id（发起人本人审批）</li>
 * <li>{@code deptLeader} → 发起人所在部门负责人</li>
 * <li>兜底①：候选为空 → 超管（再空→门控哨兵，fail-closed，防"空办理人=人人可批")</li>
 * <li>兜底②：办理人仅发起人本人 → 追加超管，防自批卡死</li></ul>
 */
@Component
public class MugsunFlowListener implements GlobalListener {

	private final HandlerSelectService selectService;

	public MugsunFlowListener(HandlerSelectService selectService) {
		this.selectService = selectService;
	}

	@Override
	public void assignment(ListenerVariable variable) {
		Instance instance = variable.getInstance();
		String initiator = instance == null ? null : instance.getCreateBy();
		List<Task> tasks = variable.getNextTasks();
		if (tasks == null) {
			return;
		}
		for (Task task : tasks) {
			List<String> permissionList = task.getPermissionList();
			// 空 permissionList 也要兜底：空办理人在引擎里 = 人人可批(fail-open)，必须写入哨兵/超管
			Set<String> resolved = new LinkedHashSet<>();
			if (permissionList != null) {
				for (String p : permissionList) {
					if (FlowConstants.PLACEHOLDER_INITIATOR.equals(p)) {
						if (initiator != null) {
							resolved.add(initiator);
						}
					} else if (FlowConstants.PLACEHOLDER_DEPT_LEADER.equals(p)) {
						if (initiator != null) {
							resolved.addAll(selectService.deptLeaderByUser(initiator));
						}
					} else {
						resolved.add(p);
					}
				}
			}
			// 兜底①：空候选 → 超管；连超管都无 → 门控哨兵（无人持有，任务挂起而非人人可批）
			if (resolved.isEmpty()) {
				resolved.addAll(selectService.fallbackHandlers());
				if (resolved.isEmpty()) {
					resolved.add(FlowConstants.GATE_SENTINEL);
				}
			}
			// 兜底②：办理人仅发起人本人 → 追加超管，防自审自批卡死
			if (initiator != null && resolved.size() == 1 && resolved.contains(initiator)) {
				selectService.fallbackHandlers().stream()
					.filter(id -> !id.equals(initiator))
					.forEach(resolved::add);
			}
			task.setPermissionList(new ArrayList<>(resolved));
		}
	}
}
