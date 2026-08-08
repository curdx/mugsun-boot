package com.mugsun.boot.track.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mugsun.boot.common.constant.TrackConstants;
import com.mugsun.boot.track.TrackAppService;
import com.mugsun.boot.track.TrackCollectException;
import com.mugsun.boot.track.TrackIngestService;
import com.mugsun.boot.track.TrackReplayService;
import com.mugsun.boot.track.entity.TrackApp;
import com.mugsun.core.tool.api.R;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.GZIPInputStream;

/**
 * 埋点采集公开端点（SDK 直连，/track 非 /system 路径天然匿名，无需鉴权登记）：
 * POST /track/collect 批量摄入（gzip/明文 JSON）；POST /track/replay 回放块摄入（G100，base64+gzip 块）；
 * GET /track/config SDK 配置下发。
 * <p>刻意不用 Spring 全局 ObjectMapper 与 @RequestBody：XssJacksonConfig 的净化反序列化器会篡改
 * props 文本（事件属性须原样留存、截断入库，渲染侧由前端 DOMPurify 防存储型 XSS）；
 * 请求体经 XssRequestWrapper 缓存后原样读出（压缩体 ≤2MB 由过滤器 413 兜底），自行解压/解析。
 * <p>appKey 非机密（浏览器必然暴露），安全不依赖其保密——防护靠存在性校验 + 限流 + 背压 + 事件级校验。
 */
@RestController
@RequestMapping("/track")
public class TrackCollectController {

	/** 独立于 Spring 全局实例的原始解析器（规避 XSS 净化反序列化器对 props 文本的改写） */
	private static final ObjectMapper PLAIN_MAPPER = new ObjectMapper();
	/** gzip 解压分块大小 */
	private static final int GZIP_CHUNK_BYTES = 8192;

	private final TrackIngestService ingestService;
	private final TrackAppService appService;
	private final TrackReplayService replayService;

	public TrackCollectController(TrackIngestService ingestService, TrackAppService appService,
								  TrackReplayService replayService) {
		this.ingestService = ingestService;
		this.appService = appService;
		this.replayService = replayService;
	}

	/**
	 * 批量摄入：同步路径只做校验/限流/幂等/入队即返回（R 信封 code=200，无需响应体语义）。
	 * 协议：{app_key, schema_version, sdk:{platform,version}, sent_at, events:[...]}（Content-Encoding: gzip 可选）。
	 */
	@PostMapping("/collect")
	public ResponseEntity<R<?>> collect(HttpServletRequest request) throws IOException {
		try {
			byte[] body = request.getInputStream().readAllBytes();
			if (isGzip(request)) {
				body = gunzip(body);
			}
			if (body.length == 0) {
				throw new TrackCollectException(400, "请求体为空");
			}
			if (body.length > TrackConstants.COLLECT_PAYLOAD_MAX_BYTES) {
				throw new TrackCollectException(413, "请求体过大");
			}
			JsonNode root;
			try {
				root = PLAIN_MAPPER.readTree(body);
			} catch (JsonProcessingException e) {
				// 畸形 JSON 属客户端错误，转 400（不落入全局 500 兜底刷错误日志）
				throw new TrackCollectException(400, "请求体 JSON 非法");
			}
			if (root == null || !root.isObject()) {
				throw new TrackCollectException(400, "请求体须为 JSON 对象");
			}
			int accepted = ingestService.ingest(root, request.getRemoteAddr(), request.getHeader("User-Agent"));
			return ResponseEntity.ok(R.data(Map.of("received", accepted)));
		} catch (TrackCollectException e) {
			R<Object> body = R.fail(e.getMessage());
			body.setCode(e.getStatus());
			return ResponseEntity.status(e.getStatus()).body(body);
		}
	}

	/**
	 * 回放块摄入（G100）：同步路径只做校验/限流/幂等/体积累计/入队即返回（R 信封 code=200）。
	 * 协议：{app_key, session_id, seq, event_count, gzip, payload:&lt;base64&gt;}——gzip=true 时 payload=base64(gzip(rrweb 事件数组 JSON))；
	 * gzip=false 时 payload=base64(明文 JSON)（SDK pagehide 收尾块：异步 gzip 活不过卸载，同步明文编码）。
	 * （application/json；seq 会话内自 0 连续递增）。响应 data：{accepted, duplicated}——
	 * duplicated=true 为同 session+seq 重发被幂等丢弃（视为成功，SDK 无需重试）。
	 */
	@PostMapping("/replay")
	public ResponseEntity<R<?>> replay(HttpServletRequest request) throws IOException {
		try {
			byte[] body = request.getInputStream().readAllBytes();
			if (body.length == 0) {
				throw new TrackCollectException(400, "请求体为空");
			}
			if (body.length > TrackConstants.REPLAY_ENVELOPE_MAX_BYTES) {
				throw new TrackCollectException(413, "请求体过大");
			}
			JsonNode root;
			try {
				root = PLAIN_MAPPER.readTree(body);
			} catch (JsonProcessingException e) {
				// 畸形 JSON 属客户端错误，转 400（不落入全局 500 兜底刷错误日志）
				throw new TrackCollectException(400, "请求体 JSON 非法");
			}
			if (root == null || !root.isObject()) {
				throw new TrackCollectException(400, "请求体须为 JSON 对象");
			}
			return ResponseEntity.ok(R.data(replayService.ingest(root, request.getRemoteAddr())));
		} catch (TrackCollectException e) {
			R<Object> body = R.fail(e.getMessage());
			body.setCode(e.getStatus());
			return ResponseEntity.status(e.getStatus()).body(body);
		}
	}

	/**
	 * SDK 配置下发：{enabled, sampleRate, maskSelectors, replayEnabled, replaySampleRate}。
	 * replayEnabled 读 track_app.replay_enabled（G100 放开：关时 SDK 不启动录制）。
	 * 本地缓存 30s（与 appKey 校验共用 {@link TrackAppService} 缓存，多副本生效延迟见其 javadoc）。
	 */
	@GetMapping("/config")
	public ResponseEntity<R<?>> config(@RequestParam("app_key") String appKey) {
		try {
			TrackApp app = appService.findByAppKey(appKey)
				.orElseThrow(() -> new TrackCollectException(400, "应用不存在"));
			Map<String, Object> data = new HashMap<>();
			data.put("enabled", app.getEnabled() != null && app.getEnabled() == 1);
			data.put("sampleRate", app.getSampleRate() == null ? 100 : app.getSampleRate());
			data.put("maskSelectors", app.getMaskSelectors() == null ? "" : app.getMaskSelectors());
			data.put("replayEnabled", app.getReplayEnabled() != null && app.getReplayEnabled() == 1);
			data.put("replaySampleRate", app.getReplaySampleRate() == null ? 0 : app.getReplaySampleRate());
			return ResponseEntity.ok(R.data(data));
		} catch (TrackCollectException e) {
			R<Object> body = R.fail(e.getMessage());
			body.setCode(e.getStatus());
			return ResponseEntity.status(e.getStatus()).body(body);
		}
	}

	private boolean isGzip(HttpServletRequest request) {
		String encoding = request.getHeader("Content-Encoding");
		return encoding != null && encoding.toLowerCase().contains("gzip");
	}

	/** 有界解压：解压后 ≤{@value TrackConstants#COLLECT_PAYLOAD_MAX_BYTES} 字节（防 zip 炸弹；超限 413） */
	private byte[] gunzip(byte[] raw) {
		try (GZIPInputStream gzip = new GZIPInputStream(new ByteArrayInputStream(raw));
			 ByteArrayOutputStream out = new ByteArrayOutputStream()) {
			byte[] chunk = new byte[GZIP_CHUNK_BYTES];
			int total = 0;
			int n;
			while ((n = gzip.read(chunk)) != -1) {
				total += n;
				if (total > TrackConstants.COLLECT_PAYLOAD_MAX_BYTES) {
					throw new TrackCollectException(413, "请求体解压后超限");
				}
				out.write(chunk, 0, n);
			}
			return out.toByteArray();
		} catch (TrackCollectException e) {
			throw e;
		} catch (IOException e) {
			throw new TrackCollectException(400, "gzip 解压失败");
		}
	}
}
