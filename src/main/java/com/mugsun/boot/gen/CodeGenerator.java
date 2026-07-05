package com.mugsun.boot.gen;

import com.mybatisflex.codegen.Generator;
import com.mybatisflex.codegen.config.GlobalConfig;
import com.zaxxer.hikari.HikariDataSource;

/**
 * 代码生成器：逆向数据库表生成 Entity/Mapper/Service/Controller。
 * 运行：mvn -pl mugsun-boot exec:java -Dexec.mainClass=com.mugsun.boot.gen.CodeGenerator
 */
public class CodeGenerator {

	public static void main(String[] args) {
		HikariDataSource dataSource = new HikariDataSource();
		dataSource.setDriverClassName("org.postgresql.Driver");
		dataSource.setJdbcUrl("jdbc:postgresql://localhost:5432/mugsun");
		dataSource.setUsername("mugsun");
		dataSource.setPassword("mugsun");

		GlobalConfig config = new GlobalConfig();
		config.getPackageConfig()
			.setSourceDir("/Users/wdx/opc/java/mugsun/mugsun-boot/src/main/java")
			.setBasePackage("com.mugsun.boot.gen");
		config.getStrategyConfig()
			.setGenerateTable("gen_product")
			.setTablePrefix("gen_")
			.setColumnConfig("gen_product", com.mybatisflex.codegen.config.ColumnConfig.create()
				.setColumnName("id")
				.setKeyType(com.mybatisflex.annotation.KeyType.Generator)
				.setKeyValue("flexId"));
		// 保留主键列（否则模板取不到主键，controller 生成为空）；id 配雪花 flexId，常规 save 不传 id 自动生成
		config.enableEntity().setOverwriteEnable(true);
		config.enableMapper().setOverwriteEnable(true);
		config.enableService().setOverwriteEnable(true);
		config.enableServiceImpl().setOverwriteEnable(true);
		config.enableController().setOverwriteEnable(true);

		new Generator(dataSource, config).generate();
		dataSource.close();
		System.out.println(">>> 代码生成完成");
	}
}
