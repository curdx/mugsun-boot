package com.mugsun.boot.datascope.strategy;

import com.mugsun.boot.datascope.DataScopeContext;
import com.mugsun.boot.datascope.DataScopeStrategy;
import com.mugsun.boot.system.entity.SysDept;
import com.mugsun.boot.system.mapper.SysDeptMapper;
import com.mybatisflex.core.query.QueryWrapper;
import org.springframework.stereotype.Component;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

/** 本部门及子部门：按部门列在部门子树集合内过滤 */
@Component
public class DeptAndChildDataScopeStrategy implements DataScopeStrategy {

	private final SysDeptMapper deptMapper;

	public DeptAndChildDataScopeStrategy(SysDeptMapper deptMapper) {
		this.deptMapper = deptMapper;
	}

	@Override
	public int scope() {
		return 3;
	}

	@Override
	public Consumer<QueryWrapper> build(DataScopeContext.Scope ctx, String deptColumn, String userColumn) {
		List<Long> ids = ctx.deptId() == null ? List.of(-1L) : subtreeDeptIds(ctx.deptId());
		return query -> query.in(deptColumn, ids);
	}

	/** 本部门及子部门 id 集合（含自身），按 parent_id 向下广度遍历 */
	private List<Long> subtreeDeptIds(Long rootId) {
		List<SysDept> all = deptMapper.selectAll();
		Set<Long> result = new HashSet<>();
		Deque<Long> stack = new ArrayDeque<>();
		stack.push(rootId);
		result.add(rootId);
		while (!stack.isEmpty()) {
			Long cur = stack.pop();
			for (SysDept d : all) {
				if (cur.equals(d.getParentId()) && result.add(d.getId())) {
					stack.push(d.getId());
				}
			}
		}
		return new ArrayList<>(result);
	}
}
