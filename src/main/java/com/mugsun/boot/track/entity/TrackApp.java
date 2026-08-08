package com.mugsun.boot.track.entity;

import com.mugsun.core.mybatis.base.BaseEntity;
import com.mybatisflex.annotation.Table;

/**
 * 埋点接入应用（track 库）：appKey/采样/开关/保留期/回放配置，SDK 配置下发的数据源。
 * <p>app_key 浏览器可见、非机密，仅作应用标识 + 限流维度；tenant_id 服务端映射，禁止客户端上报。
 */
@Table("track_app")
public class TrackApp extends BaseEntity {

	/** 接入标识（浏览器可见，非机密，仅作应用标识+限流维度） */
	private String appKey;
	/** 应用显示名 */
	private String appName;
	/** 平台：web/android/ios（跨端预留） */
	private String platform;
	/** 归属租户（服务端映射，禁止客户端上报） */
	private String tenantId;
	/** 事件采样率 % */
	private Integer sampleRate;
	/** 总开关（0=采集端直接拒收） */
	private Integer enabled;
	/** 前端屏蔽选择器，逗号分隔，配置下发 */
	private String maskSelectors;
	/** 明细保留天数（分区清理依据） */
	private Integer retentionDays;
	/** 会话回放开关：1 开 / 0 关（G100） */
	private Integer replayEnabled;
	/** 回放会话采样率 %（回放重，单独采样） */
	private Integer replaySampleRate;
	/** 回放保留天数（远短于事件） */
	private Integer replayRetentionDays;
	private String remark;

	public String getAppKey() {
		return appKey;
	}

	public void setAppKey(String appKey) {
		this.appKey = appKey;
	}

	public String getAppName() {
		return appName;
	}

	public void setAppName(String appName) {
		this.appName = appName;
	}

	public String getPlatform() {
		return platform;
	}

	public void setPlatform(String platform) {
		this.platform = platform;
	}

	public String getTenantId() {
		return tenantId;
	}

	public void setTenantId(String tenantId) {
		this.tenantId = tenantId;
	}

	public Integer getSampleRate() {
		return sampleRate;
	}

	public void setSampleRate(Integer sampleRate) {
		this.sampleRate = sampleRate;
	}

	public Integer getEnabled() {
		return enabled;
	}

	public void setEnabled(Integer enabled) {
		this.enabled = enabled;
	}

	public String getMaskSelectors() {
		return maskSelectors;
	}

	public void setMaskSelectors(String maskSelectors) {
		this.maskSelectors = maskSelectors;
	}

	public Integer getRetentionDays() {
		return retentionDays;
	}

	public void setRetentionDays(Integer retentionDays) {
		this.retentionDays = retentionDays;
	}

	public Integer getReplayEnabled() {
		return replayEnabled;
	}

	public void setReplayEnabled(Integer replayEnabled) {
		this.replayEnabled = replayEnabled;
	}

	public Integer getReplaySampleRate() {
		return replaySampleRate;
	}

	public void setReplaySampleRate(Integer replaySampleRate) {
		this.replaySampleRate = replaySampleRate;
	}

	public Integer getReplayRetentionDays() {
		return replayRetentionDays;
	}

	public void setReplayRetentionDays(Integer replayRetentionDays) {
		this.replayRetentionDays = replayRetentionDays;
	}

	public String getRemark() {
		return remark;
	}

	public void setRemark(String remark) {
		this.remark = remark;
	}
}
