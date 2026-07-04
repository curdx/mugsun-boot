package com.mugsun.boot.log;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mugsun.boot.system.entity.SysDataAudit;
import com.mugsun.boot.system.mapper.SysDataAuditMapper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/**
 * 数据变更审计：异步记录业务数据的前后镜像。
 */
@Service
public class AuditService {

	private final SysDataAuditMapper auditMapper;
	private final ObjectMapper objectMapper;

	public AuditService(SysDataAuditMapper auditMapper, ObjectMapper objectMapper) {
		this.auditMapper = auditMapper;
		this.objectMapper = objectMapper;
	}

	@Async
	public void record(String bizTable, String bizId, Object before, Object after, String operator) {
		SysDataAudit audit = new SysDataAudit();
		audit.setBizTable(bizTable);
		audit.setBizId(bizId);
		audit.setBeforeData(toJson(before));
		audit.setAfterData(toJson(after));
		audit.setOperator(operator);
		auditMapper.insertSelective(audit);
	}

	private String toJson(Object obj) {
		if (obj == null) {
			return null;
		}
		try {
			return objectMapper.writeValueAsString(obj);
		} catch (Exception e) {
			return String.valueOf(obj);
		}
	}
}
