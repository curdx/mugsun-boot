package com.mugsun.boot.track;

import cn.hutool.core.util.IdUtil;
import com.mugsun.boot.common.constant.TrackConstants;
import com.mugsun.boot.track.job.TrackPartitionJob;
import com.mugsun.boot.track.job.TrackSessionSettleJob;
import com.mugsun.boot.track.job.TrackStats5mJob;
import com.mugsun.boot.track.job.TrackStatsDayJob;
import com.mugsun.boot.track.job.TrackVitalsJob;
import com.mybatisflex.core.datasource.DataSourceKey;
import com.mybatisflex.core.row.Db;
import com.mybatisflex.core.row.Row;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 埋点聚合/维护任务集成测试（G99 B3）：5m/day/vitals rollup 精确性、幂等覆盖、游标补扫、
 * 采样外推口径、identity 归并 UV、直方图分位对照、会话结算定稿、分区预建/清理。
 * <p>事件一律经 {@link TrackEventStore} 直灌（receivedAtMs 可指定，窗口/日期确定），
 * 任务经公开 runNow 方法手动触发（不等调度 tick）；断言只按本类专用 appKey 过滤，天然免疫并发调度与其他测试类。
 */
class TrackJobTest extends AbstractTrackIntegrationTest {

	private static final String APP_5M = "it-job-a";
	private static final String APP_HALF = "it-job-s50";
	private static final String APP_BF = "it-job-bf";
	private static final String APP_DAY = "it-job-day";
	private static final String APP_VIT = "it-job-vit";

	@Autowired
	private TrackEventStore store;
	@Autowired
	private TrackStats5mJob stats5mJob;
	@Autowired
	private TrackStatsDayJob statsDayJob;
	@Autowired
	private TrackVitalsJob vitalsJob;
	@Autowired
	private TrackSessionSettleJob settleJob;
	@Autowired
	private TrackPartitionJob partitionJob;

	/** 播种测试应用（幂等；sampleRate 逐应用不同） */
	@BeforeEach
	void seedApps() {
		seedApp(APP_5M, 100);
		seedApp(APP_HALF, 50);
		seedApp(APP_BF, 100);
		seedApp(APP_DAY, 100);
		seedApp(APP_VIT, 100);
	}

	private void seedApp(String appKey, int sampleRate) {
		DataSourceKey.use(TrackConstants.DS_KEY, () -> Db.updateBySql(
			"INSERT INTO track_app (id, app_key, app_name, platform, tenant_id, sample_rate, enabled,"
				+ " create_time, update_time, is_deleted) VALUES (?, ?, ?, 'web', ?, ?, 1, now(), now(), 0)"
				+ " ON CONFLICT (app_key) WHERE is_deleted = 0 DO UPDATE SET sample_rate = EXCLUDED.sample_rate",
			IdUtil.getSnowflakeNextId(), appKey, "任务测试-" + appKey, PLATFORM_TENANT, sampleRate));
	}

	/** ① 5m rollup：窗口数值精确（四维）+ 同窗口重跑数值不变（幂等覆盖，严禁累加） */
	@Test
	void stats5mExactAndIdempotent() {
		LocalDateTime w = floorWindow(System.currentTimeMillis() - 600000L, 5);
		long base = w.toInstant(ZoneOffset.UTC).toEpochMilli();
		String s1 = uuid();
		String s2 = uuid();
		String d1 = uuid();
		String d2 = uuid();
		List<TrackIngestEvent> events = List.of(
			ev(APP_5M, "$pageview", base + 1000, d1, s1, "/home", "google.com", "desktop", null, null),
			ev(APP_5M, "$pageview", base + 2000, d1, s1, "/home", "google.com", "desktop", null, null),
			ev(APP_5M, "$pageview", base + 3000, d2, s2, "/about", null, "mobile", null, null),
			ev(APP_5M, "$click", base + 4000, d1, s1, "/home", null, "desktop", null, null),
			ev(APP_5M, "$click", base + 5000, d1, s1, "/home", null, "desktop", null, null),
			ev(APP_5M, "$pageleave", base + 6000, d2, s2, "/about", null, "mobile", 4000, null));
		store.insertEvents(events);

		String summary = stats5mJob.rollupNow();
		assertThat(summary).contains("5m rollup");

		assertStat(APP_5M, "event", "$pageview", w, 3, 3, 2, 0);
		assertStat(APP_5M, "event", "$click", w, 0, 2, 1, 0);
		assertStat(APP_5M, "event", "$pageleave", w, 0, 1, 1, 4000);
		assertStat(APP_5M, "page", "/home", w, 2, 4, 1, 0);
		assertStat(APP_5M, "page", "/about", w, 1, 2, 1, 4000);
		assertStat(APP_5M, "referrer", "google.com", w, 2, 2, 1, 0);
		assertStat(APP_5M, "device", "desktop", w, 2, 4, 1, 0);
		assertStat(APP_5M, "device", "mobile", w, 1, 2, 1, 4000);

		// 幂等：快照全部行（计数+求和），同窗口重跑后逐项不变
		Row before = trackRow("SELECT count(*) AS rows, coalesce(sum(pv),0) AS pv,"
			+ " coalesce(sum(event_count),0) AS ec, coalesce(sum(session_count),0) AS sc,"
			+ " coalesce(sum(duration_sum),0) AS dur FROM track_stats_5m WHERE app_key = ?", APP_5M);
		stats5mJob.rollupNow();
		Row after = trackRow("SELECT count(*) AS rows, coalesce(sum(pv),0) AS pv,"
			+ " coalesce(sum(event_count),0) AS ec, coalesce(sum(session_count),0) AS sc,"
			+ " coalesce(sum(duration_sum),0) AS dur FROM track_stats_5m WHERE app_key = ?", APP_5M);
		assertThat(after.getLong("rows")).as("重跑后行数不变（覆盖而非累加）").isEqualTo(before.getLong("rows"));
		assertThat(after.getLong("pv")).isEqualTo(before.getLong("pv"));
		assertThat(after.getLong("ec")).isEqualTo(before.getLong("ec"));
		assertThat(after.getLong("sc")).isEqualTo(before.getLong("sc"));
		assertThat(after.getLong("dur")).isEqualTo(before.getLong("dur"));
	}

	/** ② 采样外推口径：sample_rate=50 → pv/event_count 翻倍还原；session_count 不外推 */
	@Test
	void stats5mSampleRateExtrapolation() {
		LocalDateTime w = floorWindow(System.currentTimeMillis() - 600000L, 5);
		long base = w.toInstant(ZoneOffset.UTC).toEpochMilli();
		String s3 = uuid();
		store.insertEvents(List.of(
			ev(APP_HALF, "$pageview", base + 1000, uuid(), s3, "/half", null, "desktop", null, null),
			ev(APP_HALF, "$pageview", base + 2000, uuid(), s3, "/half", null, "desktop", null, null)));

		stats5mJob.rollupNow();

		assertStat(APP_HALF, "event", "$pageview", w, 4, 4, 1, 0);
		assertStat(APP_HALF, "page", "/half", w, 4, 4, 1, 0);
	}

	/** ③ 游标回拨补扫追平：游标拨回 3 个窗口前 → 三个窗口全部补齐，游标推进 */
	@Test
	void stats5mCursorBackfill() {
		LocalDateTime w = floorWindow(System.currentTimeMillis() - 900000L, 5);
		LocalDateTime rewound = w.minusMinutes(15);
		// 游标拨回（upsert；模拟任务漏跑 3 个窗口）
		DataSourceKey.use(TrackConstants.DS_KEY, () -> Db.updateBySql(
			"INSERT INTO track_rollup_cursor (id, job_key, app_key, last_bucket, create_time, update_time)"
				+ " VALUES (?, 'stats_5m', ?, ?, now(), now())"
				+ " ON CONFLICT (job_key, app_key) DO UPDATE SET last_bucket = EXCLUDED.last_bucket",
			IdUtil.getSnowflakeNextId(), APP_BF, rewound));
		for (int i = 0; i < 3; i++) {
			long base = w.plusMinutes(5L * i).toInstant(ZoneOffset.UTC).toEpochMilli();
			store.insertEvents(List.of(
				ev(APP_BF, "$pageview", base + 1000, uuid(), uuid(), "/bf" + i, null, "desktop", null, null)));
		}

		stats5mJob.rollupNow();

		for (int i = 0; i < 3; i++) {
			LocalDateTime bucket = w.plusMinutes(5L * i);
			assertThat(trackLong("SELECT count(*) AS c FROM track_stats_5m WHERE app_key = ? AND bucket_time = ?",
				APP_BF, bucket)).as("补扫窗口 %s 应有数据", bucket).isGreaterThanOrEqualTo(1L);
		}
		Row cursor = trackRow("SELECT last_bucket FROM track_rollup_cursor WHERE job_key = 'stats_5m' AND app_key = ?", APP_BF);
		java.time.LocalDateTime lastBucket = ((java.sql.Timestamp) cursor.get("last_bucket")).toLocalDateTime();
		assertThat(lastBucket)
			.as("游标已追平（至少越过最后一个灌数窗口 %s）", w.plusMinutes(10))
			.isAfterOrEqualTo(w.plusMinutes(10));
	}

	/** ④ day rollup：UV 经 track_identity 归并（两 distinct_id 绑同 user → UV=1）；会话/跳出来自 track_session */
	@Test
	void statsDayIdentityMergedUv() {
		LocalDate yesterday = LocalDate.now(ZoneOffset.UTC).minusDays(1);
		long base = yesterday.atTime(12, 0).toInstant(ZoneOffset.UTC).toEpochMilli();
		String d1 = uuid();
		String d2 = uuid();
		String sa = uuid();
		String sb = uuid();
		// 两个匿名 ID 首绑同一登录用户（共享设备口径）
		store.upsertIdentities(List.of(
			new TrackEventStore.IdentityBinding(APP_DAY, d1, 1001L, PLATFORM_TENANT),
			new TrackEventStore.IdentityBinding(APP_DAY, d2, 1001L, PLATFORM_TENANT)));
		store.insertEvents(List.of(
			ev(APP_DAY, "$pageview", base, d1, sa, "/home", "google.com", "desktop", null, null),
			ev(APP_DAY, "$pageview", base + 1000, d2, sb, "/about", null, "mobile", null, null),
			ev(APP_DAY, "$click", base + 2000, d1, sa, "/home", "google.com", "desktop", null, null)));
		// 会话物化：sa 单 PV（跳出），sb 双 PV
		store.upsertSessions(List.of(
			session(sa, APP_DAY, d1, base, base + 60000, 1),
			session(sb, APP_DAY, d2, base + 1000, base + 120000, 2)));

		String summary = statsDayJob.rollupNow();
		assertThat(summary).contains("day rollup");

		Row ov = trackRow("SELECT pv, uv, session_count, bounce_count, event_count FROM track_stats_day"
			+ " WHERE app_key = ? AND dim_type = 'overview' AND dim_key = 'ALL' AND stat_date = ?", APP_DAY, yesterday);
		assertThat(ov).as("overview 行应存在").isNotNull();
		assertThat(ov.getLong("pv")).isEqualTo(2L);
		assertThat(ov.getLong("uv")).as("identity 归并：两 distinct_id 同 user → UV=1").isEqualTo(1L);
		assertThat(ov.getLong("event_count")).isEqualTo(3L);
		assertThat(ov.getLong("session_count")).as("会话数来自 track_session（按 start_time 归日）").isEqualTo(2L);
		assertThat(ov.getLong("bounce_count")).as("跳出 = pageviews<=1").isEqualTo(1L);

		Row dim = trackRow("SELECT uv FROM track_stats_day WHERE app_key = ? AND dim_type = 'event'"
			+ " AND dim_key = '$pageview' AND stat_date = ?", APP_DAY, yesterday);
		assertThat(dim.getLong("uv")).as("维度行 UV 同样归并").isEqualTo(1L);

		// 幂等：重跑数值不变
		statsDayJob.rollupNow();
		Row ov2 = trackRow("SELECT pv, uv, session_count, bounce_count, event_count FROM track_stats_day"
			+ " WHERE app_key = ? AND dim_type = 'overview' AND dim_key = 'ALL' AND stat_date = ?", APP_DAY, yesterday);
		assertThat(ov2.getLong("uv")).isEqualTo(1L);
		assertThat(ov2.getLong("event_count")).isEqualTo(3L);
	}

	/** ⑤ vitals 直方图：桶计数精确；插值分位与明细 percentile_cont 误差 < 桶宽 */
	@Test
	void vitalsHistogramPercentileCloseToExact() {
		LocalDate yesterday = LocalDate.now(ZoneOffset.UTC).minusDays(1);
		long base = yesterday.atTime(12, 0).toInstant(ZoneOffset.UTC).toEpochMilli();
		long[] lcpValues = {50, 150, 300, 700, 1500, 3000, 7000, 15000, 40000};
		List<TrackIngestEvent> events = new ArrayList<>();
		for (int i = 0; i < lcpValues.length; i++) {
			events.add(ev(APP_VIT, "$web_vitals", base + i * 100, uuid(), uuid(), "/home", null, "desktop",
				null, "{\"metric\":\"lcp\",\"value\":" + lcpValues[i] + "}"));
		}
		long[] clsValues = {5, 15, 1200};
		for (int i = 0; i < clsValues.length; i++) {
			events.add(ev(APP_VIT, "$web_vitals", base + 5000 + i * 100, uuid(), uuid(), "/home", null, "desktop",
				null, "{\"metric\":\"cls\",\"value\":" + clsValues[i] + "}"));
		}
		store.insertEvents(events);

		vitalsJob.rollupNow();

		// 桶计数精确：9 个 lcp 值恰好各占一桶
		for (int bucket = 0; bucket <= 8; bucket++) {
			assertThat(trackLong("SELECT cnt AS c FROM track_stats_vitals WHERE app_key = ? AND stat_date = ?"
				+ " AND metric = 'lcp' AND url_path = '/home' AND bucket = ?", APP_VIT, yesterday, bucket))
				.as("lcp 桶 %d 计数", bucket).isEqualTo(1L);
		}
		assertThat(trackLong("SELECT cnt AS c FROM track_stats_vitals WHERE app_key = ? AND stat_date = ?"
			+ " AND metric = 'cls' AND url_path = '/home' AND bucket = 0", APP_VIT, yesterday)).isEqualTo(1L);
		assertThat(trackLong("SELECT cnt AS c FROM track_stats_vitals WHERE app_key = ? AND stat_date = ?"
			+ " AND metric = 'cls' AND url_path = '/home' AND bucket = 1", APP_VIT, yesterday)).isEqualTo(1L);
		assertThat(trackLong("SELECT cnt AS c FROM track_stats_vitals WHERE app_key = ? AND stat_date = ?"
			+ " AND metric = 'cls' AND url_path = '/home' AND bucket = 7", APP_VIT, yesterday))
			.as("cls 1200 落溢出桶").isEqualTo(1L);

		// 直方图计数 → 服务同口径插值；与明细 percentile_cont 对照（误差 < 所在桶桶宽）
		long[] counts = new long[9];
		for (int bucket = 0; bucket <= 8; bucket++) {
			counts[bucket] = trackLong("SELECT coalesce(sum(cnt),0) AS c FROM track_stats_vitals"
				+ " WHERE app_key = ? AND stat_date = ? AND metric = 'lcp' AND bucket = ?", APP_VIT, yesterday, bucket);
		}
		assertPercentileClose(counts, 0.50);
		assertPercentileClose(counts, 0.95);

		// 幂等：同日重算（先清后写）桶计数不变
		vitalsJob.rollupNow();
		assertThat(trackLong("SELECT coalesce(sum(cnt),0) AS c FROM track_stats_vitals"
			+ " WHERE app_key = ? AND stat_date = ? AND metric = 'lcp'", APP_VIT, yesterday))
			.as("重算后 lcp 总计数不变").isEqualTo(9L);
	}

	/** 分位对照：明细 percentile_cont vs 直方图插值，误差 < 精确值所在桶的桶宽 */
	private void assertPercentileClose(long[] counts, double q) {
		Row exact = trackRow("SELECT percentile_cont(" + q + ") WITHIN GROUP (ORDER BY (props->>'value')::numeric) AS c"
			+ " FROM track_event WHERE app_key = ? AND event_name = '$web_vitals'"
			+ " AND props->>'metric' = 'lcp'", APP_VIT);
		double exactValue = ((Number) exact.get("c")).doubleValue();
		double interpolated = TrackAnalysisService.interpolate(TrackConstants.VITALS_MS_BUCKET_BOUNDS, counts, q);
		double width = bucketWidth(TrackConstants.VITALS_MS_BUCKET_BOUNDS, exactValue);
		assertThat(Math.abs(interpolated - exactValue))
			.as("q%s：插值 %s 与精确 %s 误差应小于桶宽 %s", q, interpolated, exactValue, width)
			.isLessThan(width);
	}

	/** 值所在桶的桶宽（溢出桶取末两界差） */
	private double bucketWidth(long[] bounds, double value) {
		int bucket = 0;
		while (bucket < bounds.length && value >= bounds[bucket]) {
			bucket++;
		}
		if (bucket == 0) {
			return bounds[0];
		}
		if (bucket >= bounds.length) {
			return bounds[bounds.length - 1] - bounds[bounds.length - 2];
		}
		return bounds[bucket] - bounds[bucket - 1];
	}

	/** ⑥ 会话结算：30min 静默会话定稿（is_bounce/duration 定稿、settled=1）；活跃会话不动 */
	@Test
	void sessionSettleFinalizesSilentSessions() {
		long now = System.currentTimeMillis();
		String bounceSid = uuid();
		String activeSid = uuid();
		String freshSid = uuid();
		// 静默 40 分钟：单 PV（应判跳出）/ 双 PV（不跳出）；duration 终值 = end-start = 10 分钟
		store.upsertSessions(List.of(
			session(bounceSid, "it-job-settle", uuid(), now - 3000000L, now - 2400000L, 1),
			session(activeSid, "it-job-settle", uuid(), now - 3000000L, now - 2400000L, 2),
			session(freshSid, "it-job-settle", uuid(), now - 60000L, now, 1)));

		// 定时 tick 可能抢先定稿（结果幂等），这里只需确保「执行过一轮持锁结算」；
		// 正确性断言行状态而非本轮定稿条数（谁定稿都一样）
		awaitUntil("持锁结算一轮完成", () -> settleJob.settleNow() >= 0);

		Row bounce = trackRow("SELECT settled, is_bounce, duration_ms FROM track_session WHERE session_id = ?", bounceSid);
		assertThat(bounce.getInt("settled")).isEqualTo(1);
		assertThat(bounce.getInt("is_bounce")).as("单 PV 会话应判跳出").isEqualTo(1);
		assertThat(bounce.getInt("duration_ms")).as("duration=end-start=10min").isBetween(599000, 601000);

		Row active = trackRow("SELECT settled, is_bounce FROM track_session WHERE session_id = ?", activeSid);
		assertThat(active.getInt("settled")).isEqualTo(1);
		assertThat(active.getInt("is_bounce")).as("双 PV 会话不跳出").isEqualTo(0);

		Row fresh = trackRow("SELECT settled FROM track_session WHERE session_id = ?", freshSid);
		assertThat(fresh.getInt("settled")).as("活跃会话不应被结算").isEqualTo(0);
	}

	/** ⑦ 分区维护：预建任意月分区存在；到期分区（2020_01，远超 max retention）DETACH→DROP */
	@Test
	void partitionPrebuildAndRetentionDrop() {
		// 预建：次次月（T1 只建当月+次月；+2 月由任务逻辑创建，验证真实建区路径）
		YearMonth plus2 = YearMonth.now(ZoneOffset.UTC).plusMonths(2);
		String suffix2 = String.format("%04d_%02d", plus2.getYear(), plus2.getMonthValue());
		partitionJob.ensureMonthPartitions(plus2);
		assertThat(trackLong("SELECT count(*) AS c FROM pg_tables WHERE tablename = ?", "track_event_" + suffix2))
			.as("任务应预建事件月分区").isEqualTo(1L);
		assertThat(trackLong("SELECT count(*) AS c FROM pg_tables WHERE tablename = ?", "track_event_data_" + suffix2))
			.as("任务应预建 EAV 月分区").isEqualTo(1L);

		// 到期清理：造 2020_01 老分区（月界 2020-02-01，远早于任何 retention 到期线）
		partitionJob.ensureMonthPartitions(YearMonth.of(2020, 1));
		assertThat(trackLong("SELECT count(*) AS c FROM pg_tables WHERE tablename = 'track_event_2020_01'"))
			.as("老分区已造出").isEqualTo(1L);

		String summary = partitionJob.maintainNow();
		assertThat(summary).contains("分区维护");

		assertThat(trackLong("SELECT count(*) AS c FROM pg_tables WHERE tablename = 'track_event_2020_01'"))
			.as("到期分区应被 DETACH→DROP").isEqualTo(0L);
		assertThat(trackLong("SELECT count(*) AS c FROM pg_tables WHERE tablename = 'track_event_data_2020_01'"))
			.as("到期 EAV 分区应一并清理").isEqualTo(0L);
		// 当月分区绝不被误删
		YearMonth current = YearMonth.now(ZoneOffset.UTC);
		String currentSuffix = String.format("%04d_%02d", current.getYear(), current.getMonthValue());
		assertThat(trackLong("SELECT count(*) AS c FROM pg_tables WHERE tablename = ?", "track_event_" + currentSuffix))
			.as("当月分区必须存在").isEqualTo(1L);
	}

	// ---------- 测试工具 ----------

	private void assertStat(String appKey, String dimType, String dimKey, LocalDateTime bucket,
							long pv, long eventCount, long sessionCount, long durationSum) {
		Row row = trackRow("SELECT pv, event_count, session_count, duration_sum FROM track_stats_5m"
			+ " WHERE app_key = ? AND dim_type = ? AND dim_key = ? AND bucket_time = ?", appKey, dimType, dimKey, bucket);
		assertThat(row).as("统计行应存在: %s/%s/%s@%s", appKey, dimType, dimKey, bucket).isNotNull();
		assertThat(row.getLong("pv")).as("pv").isEqualTo(pv);
		assertThat(row.getLong("event_count")).as("event_count").isEqualTo(eventCount);
		assertThat(row.getLong("session_count")).as("session_count").isEqualTo(sessionCount);
		assertThat(row.getLong("duration_sum")).as("duration_sum").isEqualTo(durationSum);
	}

	/** 造事件（receivedAtMs=tsMs=base 指定值；时间规约由 store 绑定） */
	private TrackIngestEvent ev(String appKey, String name, long ms, String distinctId, String sessionId,
								String routePath, String referrer, String device, Integer durationMs, String propsJson) {
		TrackIngestEvent e = new TrackIngestEvent();
		e.setEventId(uuid());
		e.setAppKey(appKey);
		e.setEventName(name);
		e.setClientTsMs(ms);
		e.setTsMs(ms);
		e.setReceivedAtMs(ms);
		e.setClockSkewed(0);
		e.setDistinctId(distinctId);
		e.setSessionId(sessionId);
		e.setTenantId(PLATFORM_TENANT);
		e.setUrlPath(routePath);
		e.setRoutePath(routePath);
		e.setReferrerDomain(referrer);
		e.setDevice(device);
		e.setDurationMs(durationMs);
		e.setPropsJson(propsJson == null ? "{}" : propsJson);
		return e;
	}

	/** 造会话聚合（startTime/endTime 为 UTC 墙钟；pageviews 决定跳出定稿） */
	private TrackEventStore.SessionAggregate session(String sessionId, String appKey, String distinctId,
													 long startMs, long endMs, int pageviews) {
		return new TrackEventStore.SessionAggregate(sessionId, appKey, PLATFORM_TENANT, distinctId, null,
			LocalDateTime.ofInstant(Instant.ofEpochMilli(startMs), ZoneOffset.UTC),
			LocalDateTime.ofInstant(Instant.ofEpochMilli(endMs), ZoneOffset.UTC),
			(int) (endMs - startMs), pageviews, pageviews, "/entry", "/exit",
			null, null, "Chrome", "macOS", "desktop", null, 0, 0);
	}

	/** epoch 毫秒 → 窗口起点（UTC 墙钟，与任务同口径） */
	private static LocalDateTime floorWindow(long epochMs, long minutes) {
		long windowMs = minutes * 60000L;
		long floored = (epochMs / windowMs) * windowMs;
		return LocalDateTime.ofInstant(Instant.ofEpochMilli(floored), ZoneOffset.UTC);
	}

	private String uuid() {
		return UUID.randomUUID().toString();
	}
}
