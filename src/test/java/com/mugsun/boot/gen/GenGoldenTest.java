package com.mugsun.boot.gen;

import com.mugsun.boot.gen.entity.GenColumn;
import com.mugsun.boot.gen.entity.GenTable;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 代码生成 golden-file 快照回归：固定元数据（覆盖 input/number/select+dict/textarea/switch/datetime 全控件）
 * → 渲染全栈产物 → 比对已存快照，守模板改动不破产物。
 * <p>模板有意变更后执行 {@code mvn test -Dtest=GenGoldenTest -Dgen.updateGolden=true} 重写快照并提交。
 */
class GenGoldenTest {

	private static final Path GOLDEN_DIR = Path.of("src/test/resources/generator/golden");

	@Test
	void fullStackSnapshotMatchesGolden() throws IOException {
		GenRenderService render = new GenRenderService(null, null);
		List<GenRenderService.Product> products = render.generate(fixtureTable(), fixtureColumns());
		boolean update = Boolean.getBoolean("gen.updateGolden");
		for (GenRenderService.Product p : products) {
			Path golden = GOLDEN_DIR.resolve(p.category() + ".txt");
			if (update) {
				Files.createDirectories(GOLDEN_DIR);
				Files.writeString(golden, p.content(), StandardCharsets.UTF_8);
			} else {
				assertTrue(Files.exists(golden), "缺少 golden 快照：" + golden);
				assertEquals(Files.readString(golden, StandardCharsets.UTF_8), p.content(),
					"产物与 golden 不一致（模板回归）：" + p.category());
			}
		}
	}

	private GenTable fixtureTable() {
		GenTable t = new GenTable();
		t.setId(1000000000000000001L);
		t.setTableName("gen_product");
		t.setTableComment("商品");
		t.setEntityName("Product");
		t.setModuleName("demo");
		t.setBusinessName("product");
		t.setFunctionName("商品");
		t.setFunctionAuthor("mugsun");
		t.setBasePackage("com.mugsun.boot");
		t.setTablePrefix("gen_");
		return t;
	}

	private List<GenColumn> fixtureColumns() {
		List<GenColumn> cols = new ArrayList<>();
		cols.add(col(1, "id", "商品ID", "int8", "Long", "id", 1, 0, 0, 0, "input", null));
		cols.add(col(2, "product_name", "商品名称", "varchar", "String", "productName", 0, 1, 1, 1, "input", null));
		cols.add(col(3, "price", "价格", "numeric", "BigDecimal", "price", 0, 1, 1, 0, "number", null));
		cols.add(col(4, "status", "状态", "int4", "Integer", "status", 0, 1, 1, 1, "select", "product_status"));
		cols.add(col(5, "remark", "备注", "varchar", "String", "remark", 0, 0, 1, 0, "textarea", null));
		cols.add(col(6, "online", "上架", "int4", "Integer", "online", 0, 1, 1, 0, "switch", null));
		cols.add(col(7, "publish_time", "发布时间", "timestamp", "LocalDateTime", "publishTime", 0, 1, 1, 0, "datetime", null));
		cols.add(col(8, "create_time", "创建时间", "timestamp", "LocalDateTime", "createTime", 0, 1, 0, 0, "datetime", null));
		cols.add(col(9, "is_deleted", "删除标记", "int4", "Integer", "isDeleted", 0, 0, 0, 0, "switch", null));
		return cols;
	}

	private GenColumn col(int sort, String name, String comment, String colType, String javaType, String field,
						  int isPk, int isList, int isEdit, int isQuery, String htmlType, String dict) {
		GenColumn c = new GenColumn();
		c.setColumnName(name);
		c.setColumnComment(comment);
		c.setColumnType(colType);
		c.setJavaType(javaType);
		c.setJavaField(field);
		c.setIsPk(isPk);
		c.setIsList(isList);
		c.setIsEdit(isEdit);
		c.setIsInsert(isEdit);
		c.setIsQuery(isQuery);
		c.setQueryType("String".equals(javaType) ? "LIKE" : "EQ");
		c.setHtmlType(htmlType);
		c.setDictType(dict);
		c.setSort(sort);
		return c;
	}
}
