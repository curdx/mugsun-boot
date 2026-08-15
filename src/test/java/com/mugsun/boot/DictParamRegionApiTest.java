package com.mugsun.boot;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 字典 / 参数 / 区划回归：CRUD + 参数缓存取值 + 区划懒加载。
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class DictParamRegionApiTest extends AbstractIntegrationTest {

	private static final String TS = String.valueOf(System.currentTimeMillis());
	private String adminToken;

	@BeforeAll
	void setup() {
		adminToken = loginAdmin();
	}

	@Test
	void dictTypeItemAndDictionaryLookup() {
		String code = "it_dict_" + TS;
		Map<String, Object> type = new HashMap<>();
		type.put("parentId", 0);
		type.put("code", code);
		type.put("dictKey", "");
		type.put("dictValue", "IT字典类型-" + TS);
		type.put("sort", 1);
		assertThat(readBody(post("/system/dict/submit", type, adminToken)).path("code").asInt()).isEqualTo(200);

		JsonNode tree = readBody(get("/system/dict/tree?code=" + code, adminToken));
		assertThat(tree.path("code").asInt()).isEqualTo(200);
		long typeId = findDictId(tree.path("data"), code, "IT字典类型-" + TS);
		assertThat(typeId).isPositive();

		Map<String, Object> item = new HashMap<>();
		item.put("parentId", typeId);
		item.put("code", code);
		item.put("dictKey", "k1");
		item.put("dictValue", "v1");
		item.put("sort", 1);
		assertThat(readBody(post("/system/dict/submit", item, adminToken)).path("code").asInt()).isEqualTo(200);

		JsonNode dict = readBody(get("/system/dict/dictionary?code=" + code, adminToken));
		assertThat(dict.path("code").asInt()).isEqualTo(200);
		assertThat(dict.path("data").isArray()).isTrue();
		assertThat(dict.path("data").size()).isGreaterThanOrEqualTo(1);

		JsonNode tree2 = readBody(get("/system/dict/tree?code=" + code, adminToken));
		long itemId = findDictItemId(tree2.path("data"), "k1");
		assertThat(itemId).isPositive();
		assertThat(readBody(post("/system/dict/remove", List.of(itemId), adminToken)).path("code").asInt()).isEqualTo(200);
		assertThat(readBody(post("/system/dict/remove", List.of(typeId), adminToken)).path("code").asInt()).isEqualTo(200);
	}

	@Test
	void paramSubmitValueAndEvict() {
		String key = "it.param." + TS;
		Map<String, Object> param = new HashMap<>();
		param.put("paramName", "IT参数-" + TS);
		param.put("paramKey", key);
		param.put("paramValue", "v1");
		param.put("remark", "集成测试");
		assertThat(readBody(post("/system/param/submit", param, adminToken)).path("code").asInt()).isEqualTo(200);

		JsonNode v1 = readBody(get("/system/param/value?paramKey=" + key, adminToken));
		assertThat(v1.path("code").asInt()).isEqualTo(200);
		assertThat(v1.path("data").asText()).isEqualTo("v1");

		JsonNode list = readBody(get("/system/param/list?paramKey=" + key, adminToken));
		long id = -1;
		for (JsonNode row : list.path("data")) {
			if (key.equals(row.path("paramKey").asText())) {
				id = row.path("id").asLong();
				break;
			}
		}
		assertThat(id).isPositive();

		Map<String, Object> update = new HashMap<>();
		update.put("id", id);
		update.put("paramName", "IT参数-" + TS);
		update.put("paramKey", key);
		update.put("paramValue", "v2");
		assertThat(readBody(post("/system/param/submit", update, adminToken)).path("code").asInt()).isEqualTo(200);
		JsonNode v2 = readBody(get("/system/param/value?paramKey=" + key, adminToken));
		assertThat(v2.path("data").asText()).isEqualTo("v2");

		assertThat(readBody(post("/system/param/remove", List.of(id), adminToken)).path("code").asInt()).isEqualTo(200);
	}

	@Test
	void regionLazyTreeSubmitAndRemove() {
		String code = "it" + TS.substring(TS.length() - 8);
		Map<String, Object> region = new HashMap<>();
		region.put("code", code);
		region.put("parentCode", "0");
		region.put("name", "IT区划-" + TS);
		region.put("level", 1);
		region.put("sort", 99);
		assertThat(readBody(post("/system/region/submit", region, adminToken)).path("code").asInt()).isEqualTo(200);

		JsonNode tree = readBody(get("/system/region/lazy-tree?parentCode=0", adminToken));
		assertThat(tree.path("code").asInt()).isEqualTo(200);
		long id = -1;
		for (JsonNode row : tree.path("data")) {
			if (code.equals(row.path("code").asText())) {
				id = row.path("id").asLong();
				break;
			}
		}
		assertThat(id).isPositive();
		assertThat(readBody(post("/system/region/remove/" + id, Map.of(), adminToken)).path("code").asInt()).isEqualTo(200);
	}

	private long findDictId(JsonNode nodes, String code, String dictValue) {
		if (nodes == null || !nodes.isArray()) {
			return -1;
		}
		for (JsonNode n : nodes) {
			if (code.equals(n.path("code").asText()) && dictValue.equals(n.path("dictValue").asText())) {
				return n.path("id").asLong();
			}
			long child = findDictId(n.path("children"), code, dictValue);
			if (child > 0) {
				return child;
			}
		}
		return -1;
	}

	private long findDictItemId(JsonNode nodes, String dictKey) {
		if (nodes == null || !nodes.isArray()) {
			return -1;
		}
		for (JsonNode n : nodes) {
			if (dictKey.equals(n.path("dictKey").asText())) {
				return n.path("id").asLong();
			}
			long child = findDictItemId(n.path("children"), dictKey);
			if (child > 0) {
				return child;
			}
		}
		return -1;
	}
}
