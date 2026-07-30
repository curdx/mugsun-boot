package com.mugsun.boot;

import com.alicp.jetcache.anno.config.EnableMethodCache;
import org.dromara.x.file.storage.spring.EnableFileStorage;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Mugsun 单体版启动入口
 *
 * <p>扫描根为 {@code com.mugsun.boot}，与 starter 的 {@code com.mugsun.core.*}
 * 隔离，确保 starter 能力仅经自动装配（AutoConfiguration.imports）生效，而非依赖组件扫描。
 */
@SpringBootApplication
@EnableAsync
@EnableScheduling
@EnableFileStorage
@EnableMethodCache(basePackages = "com.mugsun.boot")
@MapperScan("com.mugsun.boot.**.mapper")
public class MugsunApplication {

	public static void main(String[] args) {
		SpringApplication.run(MugsunApplication.class, args);
	}
}
