package com.mugsun.boot.track.entity;

import com.mugsun.core.mybatis.base.BaseEntity;
import com.mybatisflex.annotation.Table;

import java.time.LocalDateTime;

/**
 * 会话回放元数据（track 库，G100）：列表检索用；rrweb 本体存对象存储私有桶压缩块，绝不进数据库事实表。
 * <p>回放是最高敏感数据：短保留期（默认 14 天）到期删对象 + 逻辑删元数据；查看必写操作日志留痕。
 */
@Table("track_replay")
public class TrackReplay extends BaseEntity {

	/** 会话 ID */
	private String sessionId;
	/** 接入应用标识 */
	private String appKey;
	/** 归属租户 */
	private String tenantId;
	/** 匿名 ID */
	private String distinctId;
	/** 登录用户 */
	private Long userId;
	/** 会话开始时间 */
	private LocalDateTime startTime;
	/** 回放时长（毫秒） */
	private Integer durationMs;
	/** 页面数 */
	private Integer pageCount;
	/** rrweb 事件条数 */
	private Integer rrwebEvents;
	/** 压缩后体积（字节） */
	private Long sizeBytes;
	/** 1=会话内发生过 $error */
	private Integer hasError;
	/** 入口路径 */
	private String entryPath;
	/** 对象存储对象键（私有桶） */
	private String storageKey;

	public String getSessionId() {
		return sessionId;
	}

	public void setSessionId(String sessionId) {
		this.sessionId = sessionId;
	}

	public String getAppKey() {
		return appKey;
	}

	public void setAppKey(String appKey) {
		this.appKey = appKey;
	}

	public String getTenantId() {
		return tenantId;
	}

	public void setTenantId(String tenantId) {
		this.tenantId = tenantId;
	}

	public String getDistinctId() {
		return distinctId;
	}

	public void setDistinctId(String distinctId) {
		this.distinctId = distinctId;
	}

	public Long getUserId() {
		return userId;
	}

	public void setUserId(Long userId) {
		this.userId = userId;
	}

	public LocalDateTime getStartTime() {
		return startTime;
	}

	public void setStartTime(LocalDateTime startTime) {
		this.startTime = startTime;
	}

	public Integer getDurationMs() {
		return durationMs;
	}

	public void setDurationMs(Integer durationMs) {
		this.durationMs = durationMs;
	}

	public Integer getPageCount() {
		return pageCount;
	}

	public void setPageCount(Integer pageCount) {
		this.pageCount = pageCount;
	}

	public Integer getRrwebEvents() {
		return rrwebEvents;
	}

	public void setRrwebEvents(Integer rrwebEvents) {
		this.rrwebEvents = rrwebEvents;
	}

	public Long getSizeBytes() {
		return sizeBytes;
	}

	public void setSizeBytes(Long sizeBytes) {
		this.sizeBytes = sizeBytes;
	}

	public Integer getHasError() {
		return hasError;
	}

	public void setHasError(Integer hasError) {
		this.hasError = hasError;
	}

	public String getEntryPath() {
		return entryPath;
	}

	public void setEntryPath(String entryPath) {
		this.entryPath = entryPath;
	}

	public String getStorageKey() {
		return storageKey;
	}

	public void setStorageKey(String storageKey) {
		this.storageKey = storageKey;
	}
}
