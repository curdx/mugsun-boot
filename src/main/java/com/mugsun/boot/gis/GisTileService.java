package com.mugsun.boot.gis;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mugsun.boot.gis.entity.GisMapProvider;
import com.mugsun.boot.gis.mapper.GisMapProviderMapper;
import com.mugsun.core.tool.exception.ServiceException;
import com.mybatisflex.core.query.QueryWrapper;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Base64;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 瓦片反代：仅请求白名单主机，密钥留在服务端。
 */
@Service
public class GisTileService {

	private static final Duration TIMEOUT = Duration.ofSeconds(8);
	private static final HttpClient HTTP = HttpClient.newBuilder()
		.connectTimeout(TIMEOUT)
		.followRedirects(HttpClient.Redirect.NORMAL)
		.build();

	/** 1×1 透明 PNG：密钥缺失/上游失败时仍返回图片，避免 OL/Cesium 把 JSON 当瓦片解码后停渲 */
	private static final byte[] EMPTY_PNG = Base64.getDecoder().decode(
		"iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mNkYAAAAAYAAjCB0C8AAAAASUVORK5CYII=");

	private static ResponseEntity<byte[]> emptyTile() {
		return ResponseEntity.ok()
			.contentType(MediaType.IMAGE_PNG)
			.header(HttpHeaders.CACHE_CONTROL, "no-store")
			.body(EMPTY_PNG);
	}

	private final GisMapProviderMapper providerMapper;
	private final ObjectMapper objectMapper;
	private final Map<String, GoogleSession> googleSessions = new ConcurrentHashMap<>();

	public GisTileService(GisMapProviderMapper providerMapper, ObjectMapper objectMapper) {
		this.providerMapper = providerMapper;
		this.objectMapper = objectMapper;
	}

	public ResponseEntity<byte[]> fetch(String provider, String layer, int z, int x, int y) {
		if (!GisConstants.PROVIDERS.contains(provider)) {
			throw new ServiceException(GisConstants.MSG_PROVIDER_UNKNOWN);
		}
		if (z < GisConstants.TILE_MIN_Z || z > GisConstants.TILE_MAX_Z) {
			throw new ServiceException("缩放级别超出范围");
		}
		GisMapProvider cfg = providerMapper.selectOneByQuery(QueryWrapper.create().eq("provider", provider));
		if (cfg == null || cfg.getApiKey() == null || cfg.getApiKey().isBlank()) {
			return emptyTile();
		}
		if (cfg.getEnabled() != null && cfg.getEnabled() != GisConstants.STATUS_ENABLE) {
			return emptyTile();
		}
		try {
			URI uri = URI.create(buildUrl(provider, layer, z, x, y, cfg));
			assertHostAllowed(uri.getHost());
			HttpRequest req = HttpRequest.newBuilder(uri)
				.timeout(TIMEOUT)
				.header("User-Agent", "Mozilla/5.0")
				.GET()
				.build();
			HttpResponse<byte[]> resp = HTTP.send(req, HttpResponse.BodyHandlers.ofByteArray());
			if (resp.statusCode() < 200 || resp.statusCode() >= 300) {
				return emptyTile();
			}
			String ctype = resp.headers().firstValue("Content-Type").orElse(MediaType.IMAGE_PNG_VALUE);
			String parsed = ctype.contains("/") ? ctype.split(";")[0].trim() : MediaType.IMAGE_PNG_VALUE;
			if (!parsed.toLowerCase(Locale.ROOT).startsWith("image/")) {
				return emptyTile();
			}
			if ("image/jpg".equalsIgnoreCase(parsed)) {
				parsed = MediaType.IMAGE_JPEG_VALUE;
			}
			return ResponseEntity.ok()
				.contentType(MediaType.parseMediaType(parsed))
				.header(HttpHeaders.CACHE_CONTROL, "private, max-age=86400")
				.body(resp.body());
		} catch (ServiceException e) {
			return emptyTile();
		} catch (Exception e) {
			return emptyTile();
		}
	}

	private String buildUrl(String provider, String layer, int z, int x, int y, GisMapProvider cfg) throws Exception {
		String key = cfg.getApiKey();
		String layerKey = normalizeLayer(layer);
		int sub = Math.floorMod(x + y, 8);
		return switch (provider) {
			case GisConstants.PROVIDER_TIANDITU -> {
				String t = tiandituType(layerKey);
				yield "https://t" + (sub % 8) + ".tianditu.gov.cn/DataServer?T=" + t
					+ "&x=" + x + "&y=" + y + "&l=" + z + "&tk=" + key;
			}
			case GisConstants.PROVIDER_AMAP -> {
				int s = (sub % 4) + 1;
				if ("img".equals(layerKey)) {
					yield "https://webst0" + s + ".is.autonavi.com/appmaptile?style=6&x=" + x + "&y=" + y
						+ "&z=" + z + "&key=" + key;
				}
				if ("cia".equals(layerKey) || "cva".equals(layerKey)) {
					yield "https://webst0" + s + ".is.autonavi.com/appmaptile?style=8&x=" + x + "&y=" + y
						+ "&z=" + z + "&key=" + key;
				}
				yield "https://webrd0" + s + ".is.autonavi.com/appmaptile?lang=zh_cn&size=1&scale=1&style=8&x="
					+ x + "&y=" + y + "&z=" + z + "&key=" + key;
			}
			case GisConstants.PROVIDER_BAIDU -> {
				int s = sub % 4;
				if ("img".equals(layerKey)) {
					yield "https://maponline" + s + ".bdimg.com/starpic/?qt=satepc&u=x=" + x + ";y=" + y
						+ ";z=" + z + ";v=009;type=sate&fm=46&ak=" + key;
				}
				yield "https://maponline" + s + ".bdimg.com/tile/?qt=vtile&x=" + x + "&y=" + y + "&z=" + z
					+ "&styles=pl&scaler=1&ak=" + key;
			}
			case GisConstants.PROVIDER_GOOGLE -> googleTileUrl(layerKey, z, x, y, key);
			default -> throw new ServiceException(GisConstants.MSG_PROVIDER_UNKNOWN);
		};
	}

	private String googleTileUrl(String layer, int z, int x, int y, String key) throws Exception {
		String mapType = "img".equals(layer) ? "satellite" : "roadmap";
		String session = googleSession(key, mapType);
		return "https://tile.googleapis.com/v1/2dtiles/" + z + "/" + x + "/" + y
			+ "?session=" + session + "&key=" + key;
	}

	private String googleSession(String key, String mapType) throws Exception {
		String cacheKey = key.hashCode() + ":" + mapType;
		GoogleSession cached = googleSessions.get(cacheKey);
		if (cached != null && cached.expireAt > System.currentTimeMillis() + 60_000) {
			return cached.token;
		}
		String body = "{\"mapType\":\"" + mapType + "\",\"language\":\"zh-CN\",\"region\":\"US\"}";
		HttpRequest req = HttpRequest.newBuilder(
				URI.create("https://tile.googleapis.com/v1/createSession?key=" + key))
			.timeout(TIMEOUT)
			.header("Content-Type", "application/json")
			.POST(HttpRequest.BodyPublishers.ofString(body))
			.build();
		HttpResponse<String> resp = HTTP.send(req, HttpResponse.BodyHandlers.ofString());
		if (resp.statusCode() < 200 || resp.statusCode() >= 300) {
			throw new ServiceException("Google 会话创建失败");
		}
		JsonNode n = objectMapper.readTree(resp.body());
		String token = n.path("session").asText(null);
		if (token == null || token.isBlank()) {
			throw new ServiceException("Google 会话创建失败");
		}
		long expire = System.currentTimeMillis() + Duration.ofDays(1).toMillis();
		googleSessions.put(cacheKey, new GoogleSession(token, expire));
		return token;
	}

	private static String tiandituType(String layer) {
		return switch (layer) {
			case "img" -> "img_w";
			case "cia" -> "cia_w";
			case "cva" -> "cva_w";
			default -> "vec_w";
		};
	}

	private static String normalizeLayer(String layer) {
		if (layer == null || layer.isBlank()) {
			return "vec";
		}
		return layer.trim().toLowerCase(Locale.ROOT);
	}

	private static void assertHostAllowed(String host) {
		if (host == null) {
			throw new ServiceException("非法底图地址");
		}
		String h = host.toLowerCase(Locale.ROOT);
		boolean ok = h.endsWith("tianditu.gov.cn")
			|| h.endsWith("amap.com")
			|| h.endsWith("autonavi.com")
			|| h.endsWith("bdimg.com")
			|| h.endsWith("googleapis.com");
		if (!ok) {
			throw new ServiceException("非法底图地址");
		}
	}

	private record GoogleSession(String token, long expireAt) {
	}
}
