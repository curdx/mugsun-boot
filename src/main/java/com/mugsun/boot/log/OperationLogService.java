package com.mugsun.boot.log;

import com.mugsun.boot.system.entity.SysOperLog;
import com.mugsun.boot.system.mapper.SysOperLogMapper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/**
 * 操作日志异步落库（独立 Bean，确保 @Async 经代理生效）。
 */
@Service
public class OperationLogService {

	private final SysOperLogMapper operLogMapper;

	public OperationLogService(SysOperLogMapper operLogMapper) {
		this.operLogMapper = operLogMapper;
	}

	@Async
	public void saveAsync(SysOperLog log) {
		operLogMapper.insertSelective(log);
	}
}
