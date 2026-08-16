package com.mugsun.boot.gis;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mugsun.boot.gis.entity.GisMapProvider;
import com.mugsun.boot.gis.mapper.GisMapProviderMapper;
import com.mugsun.core.tool.exception.ServiceException;
import com.mybatisflex.core.query.QueryWrapper;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * 地名搜索与逆地理编码：走天地图服务端接口，密钥不进浏览器。
 */
@Service
public class GisSearchService {

	private static final Duration TIMEOUT = Duration.ofSeconds(8);
	private static final HttpClient HTTP = HttpClient.newBuilder()
		.connectTimeout(TIMEOUT)
		.followRedirects(HttpClient.Redirect.NORMAL)
		.build();

	private final GisMapProviderMapper providerMapper;
	private final ObjectMapper objectMapper;

	public GisSearchService(GisMapProviderMapper providerMapper, ObjectMapper objectMapper) {
		this.providerMapper = providerMapper;
		this.objectMapper = objectMapper;
	}

	public List<Map<String, Object>> search(String keyword, Double lon, Double lat) {
		String q = keyword == null ? "" : keyword.trim();
		if (q.length() < 2 || q.length() > 64) {
			throw new ServiceException(GisConstants.MSG_SEARCH_KEYWORD);
		}
		String key = requireTiandituKey();
		List<Map<String, Object>> out = new ArrayList<>();
		try {
			merge(out, searchPois(q, lon, lat, key));
			merge(out, geocodeForward(q, key));
		} catch (ServiceException e) {
			throw e;
		} catch (Exception e) {
			throw new ServiceException(GisConstants.MSG_SEARCH_UPSTREAM);
		}
		return out.size() > 12 ? out.subList(0, 12) : out;
	}

	public Map<String, Object> reverse(double lon, double lat) {
		if (lon < -180 || lon > 180 || lat < -90 || lat > 90) {
			throw new ServiceException("坐标超出范围");
		}
		String key = requireTiandituKey();
		try {
			String ds = URLEncoder.encode(
				"{\"lon\":" + lon + ",\"lat\":" + lat + ",\"ver\":1}", StandardCharsets.UTF_8);
			JsonNode root = getJson("https://api.tianditu.gov.cn/geocoder?postStr=" + ds
				+ "&type=geocode&tk=" + key);
			JsonNode result = root.path("result");
			Map<String, Object> data = new LinkedHashMap<>();
			data.put("lon", lon);
			data.put("lat", lat);
			data.put("address", firstNonBlank(text(result, "formatted_address"), text(result, "address")));
			JsonNode comp = result.path("addressComponent");
			if (comp.isObject()) {
				data.put("province", text(comp, "province"));
				data.put("city", text(comp, "city"));
				data.put("county", text(comp, "county"));
				data.put("poi", text(comp, "poi"));
			}
			return data;
		} catch (ServiceException e) {
			throw e;
		} catch (Exception e) {
			throw new ServiceException(GisConstants.MSG_SEARCH_UPSTREAM);
		}
	}

	private String requireTiandituKey() {
		GisMapProvider cfg = providerMapper.selectOneByQuery(
			QueryWrapper.create().eq("provider", GisConstants.PROVIDER_TIANDITU));
		if (cfg == null || cfg.getApiKey() == null || cfg.getApiKey().isBlank()
			|| (cfg.getEnabled() != null && cfg.getEnabled() != GisConstants.STATUS_ENABLE)) {
			throw new ServiceException(GisConstants.MSG_SEARCH_NO_KEY);
		}
		return cfg.getApiKey();
	}

	private List<Map<String, Object>> geocodeForward(String keyword, String key) throws Exception {
		String ds = URLEncoder.encode("{\"keyWord\":\"" + escapeJson(keyword) + "\"}", StandardCharsets.UTF_8);
		JsonNode root = getJson("https://api.tianditu.gov.cn/geocoder?ds=" + ds + "&tk=" + key);
		JsonNode loc = root.path("location");
		List<Map<String, Object>> list = new ArrayList<>();
		if (loc.has("lon") && loc.has("lat")) {
			list.add(poi(keyword, "", loc.path("lon").asDouble(), loc.path("lat").asDouble(), "geocode"));
		}
		return list;
	}

	private List<Map<String, Object>> searchPois(String keyword, Double lon, Double lat, String key)
		throws Exception {
		Map<String, Object> post = new LinkedHashMap<>();
		post.put("keyWord", keyword);
		post.put("queryType", "1");
		post.put("start", "0");
		post.put("count", "10");
		post.put("show", "2");
		if (lon != null && lat != null) {
			post.put("level", "12");
			double d = 0.35;
			post.put("mapBound", (lon - d) + "," + (lat - d) + "," + (lon + d) + "," + (lat + d));
		} else {
			post.put("level", "5");
			post.put("mapBound", "73,18,135,54");
		}
		String postStr = URLEncoder.encode(objectMapper.writeValueAsString(post), StandardCharsets.UTF_8);
		JsonNode root = getJson("https://api.tianditu.gov.cn/v2/search?postStr=" + postStr
			+ "&type=query&tk=" + key);
		List<Map<String, Object>> list = new ArrayList<>();
		JsonNode pois = root.path("pois");
		if (pois.isArray()) {
			for (JsonNode n : pois) {
				double[] ll = parseLonLat(n.path("lonlat").asText(""));
				if (ll == null) {
					continue;
				}
				list.add(poi(text(n, "name"), text(n, "address"), ll[0], ll[1], "poi"));
			}
		}
		JsonNode area = root.path("area");
		if (area.isObject()) {
			double[] ll = parseLonLat(area.path("lonlat").asText(""));
			if (ll != null) {
				list.add(poi(text(area, "name"), text(area, "address"), ll[0], ll[1], "area"));
			}
		}
		return list;
	}

	private JsonNode getJson(String url) throws Exception {
		URI uri = URI.create(url);
		String host = uri.getHost() == null ? "" : uri.getHost().toLowerCase(Locale.ROOT);
		if (!host.endsWith("tianditu.gov.cn")) {
			throw new ServiceException("非法检索地址");
		}
		HttpRequest req = HttpRequest.newBuilder(uri)
			.timeout(TIMEOUT)
			.header("User-Agent", "Mozilla/5.0")
			.GET()
			.build();
		HttpResponse<String> resp = HTTP.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
		if (resp.statusCode() < 200 || resp.statusCode() >= 300) {
			throw new ServiceException(GisConstants.MSG_SEARCH_UPSTREAM);
		}
		String body = resp.body() == null ? "" : resp.body().trim();
		if (body.isEmpty() || body.startsWith("<")) {
			throw new ServiceException(GisConstants.MSG_SEARCH_UPSTREAM);
		}
		return objectMapper.readTree(body);
	}

	private static void merge(List<Map<String, Object>> into, List<Map<String, Object>> extra) {
		for (Map<String, Object> item : extra) {
			boolean dup = into.stream().anyMatch(exist ->
				Math.abs((Double) exist.get("lon") - (Double) item.get("lon")) < 1e-5
					&& Math.abs((Double) exist.get("lat") - (Double) item.get("lat")) < 1e-5
					&& String.valueOf(exist.get("name")).equals(String.valueOf(item.get("name"))));
			if (!dup) {
				into.add(item);
			}
		}
	}

	private static Map<String, Object> poi(String name, String address, double lon, double lat, String kind) {
		Map<String, Object> m = new LinkedHashMap<>();
		m.put("name", name == null || name.isBlank() ? kind : name);
		m.put("address", address == null ? "" : address);
		m.put("lon", lon);
		m.put("lat", lat);
		m.put("kind", kind);
		return m;
	}

	private static double[] parseLonLat(String raw) {
		if (raw == null || raw.isBlank()) {
			return null;
		}
		String[] parts = raw.trim().split("[,\\s]+");
		if (parts.length < 2) {
			return null;
		}
		try {
			return new double[] { Double.parseDouble(parts[0]), Double.parseDouble(parts[1]) };
		} catch (NumberFormatException e) {
			return null;
		}
	}

	private static String text(JsonNode n, String field) {
		JsonNode v = n.path(field);
		return v.isMissingNode() || v.isNull() ? "" : v.asText("");
	}

	private static String firstNonBlank(String a, String b) {
		if (a != null && !a.isBlank()) {
			return a;
		}
		return b == null ? "" : b;
	}

	private static String escapeJson(String s) {
		return s.replace("\\", "\\\\").replace("\"", "\\\"");
	}
}
