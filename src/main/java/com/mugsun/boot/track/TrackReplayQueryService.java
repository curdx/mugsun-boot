package com.mugsun.boot.track;

import com.mugsun.boot.common.constant.TrackConstants;
import com.mugsun.boot.track.entity.TrackReplay;
import com.mugsun.boot.track.mapper.TrackReplayMapper;
import com.mugsun.core.tool.exception.ServiceException;
import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.query.QueryWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.GZIPInputStream;

/**
 * 回放读取服务（/system/track/replay/** 数据源，G100）：类级 {@link TrackDS} 路由埋点库。
 * <p><b>租户隔离</b>：Flex mapper 查询，租户行级插件按会话上下文自动拼 tenant_id 条件
 * （超管「查看全部」模式不加条件；跨租户读取命中「不存在」），与 TrackAdminService 同语义。
 * <p><b>块内容读取</b>：按元数据行记录的存储坐标（platform/basePath + storage_key 目录 + seq 推导文件名）
 * 经 x-file-storage 下载 gzip 字节，服务端解压后以 application/json 明文返回 rrweb 事件数组——
 * 前端零解压依赖；写时已约束单块解压后 ≤1MB，读侧同界解压兜底。
 * <p><b>纪律</b>：本类严禁调用 Sa-Token 权限校验/业务库 DAO（@TrackDS 范围内 DB 访问全落埋点库），
 * 权限校验与操作日志留痕由控制器在进入本类前完成。
 */
@Service
@TrackDS
public class TrackReplayQueryService {

	private static final Logger log = LoggerFactory.getLogger(TrackReplayQueryService.class);

	/** 读侧解压分块大小 */
	private static final int GZIP_CHUNK_BYTES = 8192;

	private final TrackReplayMapper replayMapper;
	private final TrackReplayStorage storage;

	public TrackReplayQueryService(TrackReplayMapper replayMapper, TrackReplayStorage storage) {
		this.replayMapper = replayMapper;
		this.storage = storage;
	}

	/** 回放会话分页：start_time 倒序；appKey/hasError 可选过滤；投影剔除存储坐标（内部实现细节不下发） */
	public Page<Map<String, Object>> page(String appKey, Integer hasError, long pageNum, long pageSize) {
		QueryWrapper query = QueryWrapper.create().orderBy("start_time", false);
		if (appKey != null && !appKey.isBlank()) {
			query.eq("app_key", appKey.trim());
		}
		if (hasError != null) {
			query.eq("has_error", hasError == 1 ? 1 : 0);
		}
		Page<TrackReplay> page = replayMapper.paginate(pageNum, Math.min(pageSize, TrackConstants.QUERY_PAGE_SIZE_MAX), query);
		List<Map<String, Object>> records = new ArrayList<>(page.getRecords().size());
		for (TrackReplay replay : page.getRecords()) {
			records.add(project(replay));
		}
		return new Page<>(records, page.getPageNumber(), page.getPageSize(), page.getTotalRow());
	}

	/** 回放元数据 + 块清单（块键按 seq ∈ [0, last_seq] 纯推导，见 TrackReplayStorage javadoc） */
	public Map<String, Object> detail(String sessionId) {
		TrackReplay replay = requireReplay(sessionId);
		List<Map<String, Object>> blocks = new ArrayList<>();
		int lastSeq = replay.getLastSeq() == null ? -1 : replay.getLastSeq();
		for (int seq = 0; seq <= lastSeq; seq++) {
			Map<String, Object> block = new LinkedHashMap<>();
			block.put("seq", seq);
			block.put("key", TrackReplayStorage.deriveKey(replay.getStorageKey(), seq));
			blocks.add(block);
		}
		Map<String, Object> result = new LinkedHashMap<>();
		result.put("replay", project(replay));
		result.put("blocks", blocks);
		return result;
	}

	/**
	 * 读取块内容：rrweb 事件数组 JSON 明文字节（服务端已解压）。
	 * 对象不存在/平台下线/解压失败一律「回放数据不存在或已过期」（不暴露存储内部错误形态）。
	 */
	public byte[] blockJson(String sessionId, int seq) {
		TrackReplay replay = requireReplay(sessionId);
		int lastSeq = replay.getLastSeq() == null ? -1 : replay.getLastSeq();
		if (seq < 0 || seq > lastSeq) {
			throw new ServiceException("回放块不存在");
		}
		byte[] gz;
		try {
			gz = storage.load(replay.getStoragePlatform(), replay.getStorageBasePath(), replay.getStorageKey(), seq);
		} catch (Exception e) {
			log.warn("回放块读取失败 session={} seq={}：{}", sessionId, seq, e.getMessage());
			throw new ServiceException("回放数据不存在或已过期");
		}
		try {
			return gunzip(gz);
		} catch (IOException e) {
			log.warn("回放块解压失败 session={} seq={}：{}", sessionId, seq, e.getMessage());
			throw new ServiceException("回放数据不存在或已过期");
		}
	}

	/** 按 session_id 取元数据（租户插件自动隔离；跨租户/已删 → 「不存在」） */
	private TrackReplay requireReplay(String sessionId) {
		if (sessionId == null || sessionId.isBlank() || sessionId.length() > TrackConstants.SESSION_ID_MAX_LEN) {
			throw new ServiceException("无效 sessionId");
		}
		TrackReplay replay = replayMapper.selectOneByQuery(QueryWrapper.create().eq("session_id", sessionId.trim()));
		if (replay == null || replay.getStorageKey() == null) {
			throw new ServiceException("回放不存在");
		}
		return replay;
	}

	/** 响应投影（startTime 转 epoch 毫秒 UTC；storage_key/平台坐标为内部实现细节，不下发前端） */
	private Map<String, Object> project(TrackReplay replay) {
		Map<String, Object> row = new LinkedHashMap<>();
		row.put("id", replay.getId());
		row.put("sessionId", replay.getSessionId());
		row.put("appKey", replay.getAppKey());
		row.put("tenantId", replay.getTenantId());
		row.put("distinctId", replay.getDistinctId());
		row.put("userId", replay.getUserId());
		row.put("startTime", replay.getStartTime() == null ? null : replay.getStartTime().toEpochSecond(ZoneOffset.UTC) * 1000);
		row.put("durationMs", replay.getDurationMs());
		row.put("pageCount", replay.getPageCount());
		row.put("rrwebEvents", replay.getRrwebEvents());
		row.put("sizeBytes", replay.getSizeBytes());
		row.put("hasError", replay.getHasError());
		row.put("entryPath", replay.getEntryPath());
		row.put("lastSeq", replay.getLastSeq());
		return row;
	}

	/** 有界解压（写时已约束 ≤{@value TrackConstants#REPLAY_BLOCK_MAX_BYTES}，读侧同界兜底） */
	private byte[] gunzip(byte[] raw) throws IOException {
		try (GZIPInputStream gzip = new GZIPInputStream(new ByteArrayInputStream(raw));
			 ByteArrayOutputStream out = new ByteArrayOutputStream()) {
			byte[] chunk = new byte[GZIP_CHUNK_BYTES];
			int total = 0;
			int n;
			while ((n = gzip.read(chunk)) != -1) {
				total += n;
				if (total > TrackConstants.REPLAY_BLOCK_MAX_BYTES) {
					throw new IOException("回放块解压后超界");
				}
				out.write(chunk, 0, n);
			}
			return out.toByteArray();
		}
	}
}
