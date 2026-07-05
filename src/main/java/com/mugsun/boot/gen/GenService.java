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
			result.put("vue", vuePage(tableName, tablePrefix));
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

	/** 由表名与列生成前端 CRUD 页脚手架 */
	private String vuePage(String tableName, String tablePrefix) {
		String entity = toCamel(stripPrefix(tableName, tablePrefix), true);
		String path = "/system/" + toCamel(stripPrefix(tableName, tablePrefix), false).toLowerCase();
		return """
			<!-- %1$s 管理页（生成脚手架，按需调整字段与接口） -->
			<template>
			  <div class="art-full-height">
			    <ElCard class="art-table-card">
			      <ElButton @click="showDialog('add')">新增</ElButton>
			      <ElTable :data="tableData" border>
			        <ElTableColumn type="index" label="序号" width="60" />
			        <!-- TODO: 按列补充 ElTableColumn -->
			        <ElTableColumn label="操作" width="160">
			          <template #default="{ row }">
			            <ElButton link type="primary" @click="showDialog('edit', row)">编辑</ElButton>
			            <ElButton link type="danger" @click="deleteRow(row)">删除</ElButton>
			          </template>
			        </ElTableColumn>
			      </ElTable>
			    </ElCard>
			  </div>
			</template>

			<script setup lang="ts">
			  import { ref, onMounted } from 'vue'
			  import request from '@/utils/http'

			  defineOptions({ name: '%1$s' })

			  const tableData = ref<any[]>([])
			  const loadData = async () => {
			    const resp = await request.get<any>({ url: '/api%2$s/page', params: { pageNum: 1, pageSize: 20 } })
			    tableData.value = resp?.records ?? []
			  }
			  onMounted(loadData)

			  const showDialog = (_type: string, _row?: any) => {}
			  const deleteRow = async (row: any) => {
			    await request.post<void>({ url: '/api%2$s/remove', data: [row.id] })
			    loadData()
			  }
			</script>
			""".formatted(entity, path);
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
