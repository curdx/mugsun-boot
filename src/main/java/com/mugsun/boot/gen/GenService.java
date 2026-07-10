package com.mugsun.boot.gen;

import com.mybatisflex.annotation.KeyType;
import com.mybatisflex.codegen.Generator;
import com.mybatisflex.codegen.config.ColumnConfig;
import com.mybatisflex.codegen.config.GlobalConfig;
import com.mybatisflex.codegen.entity.Column;
import com.mybatisflex.codegen.entity.Table;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

/**
 * 在线代码生成：读表元数据 + 逆向生成 Entity/Mapper/Service/Controller（复用官方 codegen，
 * 落临时目录后读回为字符串供预览），并按列生成前端 CRUD 页脚手架。
 */
@Service
public class GenService {

	private final DataSource dataSource;

	public GenService(DataSource dataSource) {
		this.dataSource = dataSource;
	}

	/** 列出数据源全部表及列元数据 */
	public List<Map<String, Object>> tables() {
		GlobalConfig config = new GlobalConfig();
		config.getPackageConfig().setBasePackage("com.mugsun.preview");
		List<Table> tables = new Generator(dataSource, config).getTables();
		List<Map<String, Object>> result = new ArrayList<>();
		for (Table t : tables) {
			result.add(tableMeta(t));
		}
		result.sort(Comparator.comparing(m -> String.valueOf(m.get("name"))));
		return result;
	}

	/** 单表列元数据 */
	public Map<String, Object> columns(String tableName) {
		GlobalConfig config = baseConfig(tableName, "com.mugsun.preview", "");
		for (Table t : new Generator(dataSource, config).getTables()) {
			if (t.getName().equalsIgnoreCase(tableName)) {
				return tableMeta(t);
			}
		}
		return Map.of("name", tableName, "columns", List.of());
	}

	private Map<String, Object> tableMeta(Table t) {
		List<Map<String, Object>> cols = new ArrayList<>();
		if (t.getColumns() != null) {
			for (Column c : t.getColumns()) {
				Map<String, Object> col = new LinkedHashMap<>();
				col.put("name", c.getName());
				col.put("property", c.getProperty());
				col.put("type", c.getPropertyType());
				col.put("comment", c.getComment());
				cols.add(col);
			}
		}
		Map<String, Object> map = new LinkedHashMap<>();
		map.put("name", t.getName());
		map.put("comment", t.getComment());
		map.put("columns", cols);
		return map;
	}

	/** 生成指定表的后端代码 + 前端页，返回各文件内容字符串 */
	public Map<String, String> preview(String tableName, String basePackage, String tablePrefix) {
		String pkg = (basePackage == null || basePackage.isBlank()) ? "com.mugsun.gen" : basePackage.trim();
		Path tempDir = null;
		try {
			tempDir = Files.createTempDirectory("mugsun-gen-");
			GlobalConfig config = baseConfig(tableName, pkg, tablePrefix);
			config.getPackageConfig().setSourceDir(tempDir.toString());
			config.enableEntity().setOverwriteEnable(true);
			config.enableMapper().setOverwriteEnable(true);
			config.enableService().setOverwriteEnable(true);
			config.enableServiceImpl().setOverwriteEnable(true);
			config.enableController().setOverwriteEnable(true);
			new Generator(dataSource, config).generate();

			Map<String, String> result = new LinkedHashMap<>();
			try (Stream<Path> walk = Files.walk(tempDir)) {
				walk.filter(p -> p.toString().endsWith(".java")).forEach(p -> {
					String parent = p.getParent().getFileName().toString();
					String key = switch (parent) {
						case "entity" -> "entity";
						case "mapper" -> "mapper";
						case "service" -> "service";
						case "impl" -> "serviceImpl";
						case "controller" -> "controller";
						default -> parent;
					};
					try {
						result.put(key, Files.readString(p));
					} catch (IOException ignored) {
					}
				});
			}
			@SuppressWarnings("unchecked")
			List<Map<String, Object>> cols = (List<Map<String, Object>>) columns(tableName).getOrDefault("columns", List.of());
			result.put("vue", vuePage(tableName, tablePrefix, cols));
			return result;
		} catch (IOException e) {
			throw new RuntimeException("代码生成失败：" + e.getMessage(), e);
		} finally {
			deleteQuietly(tempDir);
		}
	}

	private GlobalConfig baseConfig(String tableName, String basePackage, String tablePrefix) {
		GlobalConfig config = new GlobalConfig();
		config.getPackageConfig().setBasePackage(basePackage);
		config.getStrategyConfig().setGenerateTable(tableName);
		if (tablePrefix != null && !tablePrefix.isBlank()) {
			config.getStrategyConfig().setTablePrefix(tablePrefix);
		}
		// 保留主键列并配雪花，规避 controller 模板取不到主键（见 G14 教训）
		config.getStrategyConfig().setColumnConfig(tableName, ColumnConfig.create()
			.setColumnName("id").setKeyType(KeyType.Generator).setKeyValue("flexId"));
		return config;
	}

	/** 由表名与列元数据生成可用的前端 CRUD 页（useCrud 组合式 + ArtTable + 内联新增/编辑弹窗） */
	private String vuePage(String tableName, String tablePrefix, List<Map<String, Object>> columns) {
		String entity = toCamel(stripPrefix(tableName, tablePrefix), true);
		// 平台约定：多词表用 kebab 路径（如 sys_mail_template → /system/mail-template）
		String path = "/system/" + stripPrefix(tableName, tablePrefix).replace("_", "-");
		// 系统/审计列：不进表格展示 / 不进编辑表单
		Set<String> tableSkip = Set.of("id", "tenantId", "isDeleted", "createUser", "updateUser", "updateTime");
		Set<String> formSkip = Set.of("id", "tenantId", "isDeleted", "createUser", "updateUser", "createTime", "updateTime");
		StringBuilder cols = new StringBuilder();
		StringBuilder fields = new StringBuilder();
		for (Map<String, Object> c : columns) {
			String prop = String.valueOf(c.get("property"));
			Object cm = c.get("comment");
			String raw = (cm == null || cm.toString().isBlank()) ? prop : cm.toString();
			// 清洗标签：去换行/反引号/引号/尖括号，避免破坏生成的 SFC 字符串与模板
			String label = raw.replaceAll("[\\r\\n`<>\"']", " ").trim();
			if (!tableSkip.contains(prop)) {
				cols.append("    { prop: '").append(prop).append("', label: '").append(label).append("', minWidth: 140 },\n");
			}
			if (!formSkip.contains(prop)) {
				fields.append("          <ElFormItem label=\"").append(label).append("\">\n")
					.append("            <ElInput v-model=\"currentRow.").append(prop).append("\" placeholder=\"请输入").append(label).append("\" />\n")
					.append("          </ElFormItem>\n");
			}
		}
		return "<!-- " + entity + " 管理页（代码生成产物·useCrud 组合式）；后端需按平台约定暴露 " + path + " 的 GET /page、POST /submit、POST /remove(体为 id 数组) 接口 -->\n"
			+ "<template>\n"
			+ "  <div class=\"art-full-height\">\n"
			+ "    <ElCard class=\"art-table-card\">\n"
			+ "      <div style=\"margin-bottom: 12px\">\n"
			+ "        <ElButton @click=\"showDialog('add')\" v-ripple>新增</ElButton>\n"
			+ "      </div>\n"
			+ "      <ArtTable\n"
			+ "        :loading=\"loading\"\n"
			+ "        :data=\"data as any[]\"\n"
			+ "        :columns=\"columns\"\n"
			+ "        :pagination=\"pagination\"\n"
			+ "        border\n"
			+ "        @pagination:size-change=\"handleSizeChange\"\n"
			+ "        @pagination:current-change=\"handleCurrentChange\"\n"
			+ "      />\n"
			+ "      <ElDialog v-model=\"dialogVisible\" :title=\"dialogType === 'add' ? '新增' : '编辑'\" width=\"520px\" align-center>\n"
			+ "        <ElForm :model=\"currentRow\" label-width=\"100px\">\n"
			+ fields
			+ "        </ElForm>\n"
			+ "        <template #footer>\n"
			+ "          <ElButton @click=\"dialogVisible = false\">取消</ElButton>\n"
			+ "          <ElButton type=\"primary\" @click=\"handleSubmit(currentRow)\">提交</ElButton>\n"
			+ "        </template>\n"
			+ "      </ElDialog>\n"
			+ "    </ElCard>\n"
			+ "  </div>\n"
			+ "</template>\n\n"
			+ "<script setup lang=\"ts\">\n"
			+ "  import { h } from 'vue'\n"
			+ "  import { ElButton, ElDialog, ElForm, ElFormItem, ElInput } from 'element-plus'\n"
			+ "  import request from '@/utils/http'\n"
			+ "  import { useCrud } from '@/hooks/core/useCrud'\n"
			+ "  import type { ColumnOption } from '@/types/component'\n\n"
			+ "  defineOptions({ name: '" + entity + "' })\n\n"
			+ "  const columnsFactory = (): ColumnOption[] => [\n"
			+ "    { type: 'index', width: 60, label: '序号' },\n"
			+ cols
			+ "    {\n"
			+ "      prop: 'operation',\n"
			+ "      label: '操作',\n"
			+ "      width: 160,\n"
			+ "      fixed: 'right',\n"
			+ "      formatter: (row: any) =>\n"
			+ "        h('div', [\n"
			+ "          h(ElButton, { link: true, type: 'primary', size: 'small', onClick: () => showDialog('edit', row) }, () => '编辑'),\n"
			+ "          h(ElButton, { link: true, type: 'danger', size: 'small', onClick: () => handleDelete(row) }, () => '删除')\n"
			+ "        ])\n"
			+ "    }\n"
			+ "  ]\n\n"
			+ "  const {\n"
			+ "    columns, data, loading, pagination, handleSizeChange, handleCurrentChange,\n"
			+ "    dialogVisible, dialogType, currentRow, showDialog, handleDelete, handleSubmit\n"
			+ "  } = useCrud({\n"
			+ "    listApi: (params: any) => request.get({ url: '/api" + path + "/page', params }),\n"
			+ "    saveApi: (data: any) => request.post({ url: '/api" + path + "/submit', data }),\n"
			+ "    removeApi: (id: any) => request.post({ url: '/api" + path + "/remove', data: [id] }),\n"
			+ "    columnsFactory,\n"
			+ "    label: '" + entity + "'\n"
			+ "  })\n"
			+ "</script>\n";
	}

	private String stripPrefix(String name, String prefix) {
		return (prefix != null && !prefix.isBlank() && name.startsWith(prefix)) ? name.substring(prefix.length()) : name;
	}

	/** 下划线转驼峰；upperFirst 控制首字母大小写 */
	private String toCamel(String name, boolean upperFirst) {
		StringBuilder sb = new StringBuilder();
		boolean up = upperFirst;
		for (char c : name.toCharArray()) {
			if (c == '_') {
				up = true;
			} else if (up) {
				sb.append(Character.toUpperCase(c));
				up = false;
			} else {
				sb.append(c);
			}
		}
		return sb.toString();
	}

	private void deleteQuietly(Path dir) {
		if (dir == null) {
			return;
		}
		try (Stream<Path> walk = Files.walk(dir)) {
			walk.sorted(Comparator.reverseOrder()).forEach(p -> {
				try {
					Files.deleteIfExists(p);
				} catch (IOException ignored) {
				}
			});
		} catch (IOException ignored) {
		}
	}
}
