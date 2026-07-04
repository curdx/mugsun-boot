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
			.setTablePrefix("gen_");
		config.enableEntity();
		config.enableMapper();
		config.enableService();
		config.enableServiceImpl();
		config.enableController();

		new Generator(dataSource, config).generate();
		dataSource.close();
		System.out.println(">>> 代码生成完成");
	}
}
