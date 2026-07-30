package com.mugsun.boot.monitor;

import cn.hutool.core.util.IdUtil;
import com.mugsun.boot.common.constant.MonitorConstants;
import com.mugsun.boot.system.service.ParamService;
import com.mybatisflex.core.row.Db;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDateTime;

/**
 * 日志保留期清理调度（G88 先例：固定 tick + 内存节流 + Redis 分布式锁）：
 * 定期物理清理 sys_api_log / sys_error_log / sys_oper_log 超 {@code monitor.log.retention-days}（默认 30 天）数据。
 * <p>oper_log 是逻辑删除表，此处为保留期物理清除；Db 原生 DELETE 不经租户插件，天然跨租户全量清理。
 */
@Component
public class LogCleanJob {

	private static final Logger log = LoggerFactory.getLogger(LogCleanJob.class);

	private final ParamService paramService;
	private final StringRedisTemplate redis;

	/** 上次清理时间戳（本节点内存节流；集群多节点各自节流 + 分布式锁兜底） */
	private volatile long lastCleanAt;

	public LogCleanJob(ParamService paramService, StringRedisTemplate redis) {
		this.paramService = paramService;
		this.redis = redis;
	}

	/** 固定 tick：到节流间隔才清理（保留期走 sys_param 热生效） */
	@Scheduled(fixedDelay = MonitorConstants.CLEAN_TICK_MS)
	public void tick() {
		long now = System.currentTimeMillis();
		if (now - lastCleanAt < MonitorConstants.CLEAN_INTERVAL_MS) {
			return;
		}
		lastCleanAt = now;
		cleanExpired();
	}

	/** 物理清理超保留期日志（package-private 供集成测试直接触发，避免长时间 sleep 等待调度） */
	void cleanExpired() {
		String token = IdUtil.fastSimpleUUID();
		Boolean acquired = redis.opsForValue().setIfAbsent(MonitorConstants.CLEAN_LOCK_KEY, token,
			Duration.ofSeconds(MonitorConstants.CLEAN_LOCK_SECONDS));
		if (!Boolean.TRUE.equals(acquired)) {
			return;
		}
		try {
			LocalDateTime deadline = LocalDateTime.now().minusDays(retentionDays());
			int api = Db.deleteBySql("DELETE FROM sys_api_log WHERE create_time < ?", deadline);
			int error = Db.deleteBySql("DELETE FROM sys_error_log WHERE create_time < ?", deadline);
			int oper = Db.deleteBySql("DELETE FROM sys_oper_log WHERE create_time < ?", deadline);
			log.info("日志保留期清理完成：api_log {} 条，error_log {} 条，oper_log {} 条（保留 {} 天）",
				api, error, oper, retentionDays());
		} finally {
			// 比对 token 释放，防误删他节点锁
			if (token.equals(redis.opsForValue().get(MonitorConstants.CLEAN_LOCK_KEY))) {
				redis.delete(MonitorConstants.CLEAN_LOCK_KEY);
			}
		}
	}

	private int retentionDays() {
		String value = paramService.getValue(MonitorConstants.PARAM_RETENTION_DAYS);
		try {
			return value == null ? MonitorConstants.DEFAULT_RETENTION_DAYS : Integer.parseInt(value.trim());
		} catch (NumberFormatException e) {
			return MonitorConstants.DEFAULT_RETENTION_DAYS;
		}
	}
}
