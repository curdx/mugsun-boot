package com.mugsun.boot.datasource;

import com.mugsun.boot.datasource.entity.SysTenantDatasource;
import com.mugsun.boot.datasource.mapper.SysTenantDatasourceMapper;
import com.mybatisflex.core.datasource.FlexDataSource;
import com.mybatisflex.core.query.QueryWrapper;
import com.mugsun.boot.tenant.TenantContext;
import com.zaxxer.hikari.HikariDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 租户独立数据源注册中心：运行时把租户数据源加入 FlexDataSource，
 * 并按当前租户解析路由键（DataSourceKey）。启用配置的租户其业务数据落独立库。
 */
@Component
public class TenantDataSourceRegistry {

	private static final Logger log = LoggerFactory.getLogger(TenantDataSourceRegistry.class);
	public static final String DEFAULT_KEY = "primary";
	private static final String KEY_PREFIX = "tenant_";

	private final DataSource dataSource;
	private final SysTenantDatasourceMapper datasourceMapper;
	/** 已注册且启用的租户编号 */
	private final Set<String> activeTenants = ConcurrentHashMap.newKeySet();

	public TenantDataSourceRegistry(DataSource dataSource, SysTenantDatasourceMapper datasourceMapper) {
		this.dataSource = dataSource;
		this.datasourceMapper = datasourceMapper;
	}

	/** 应用就绪后加载全部启用配置并注册（此时 Flyway 已建表） */
	@EventListener(ApplicationReadyEvent.class)
	public void loadAll() {
		if (!(dataSource instanceof FlexDataSource)) {
			log.warn("当前 dataSource 非 FlexDataSource，租户独立数据源不可用");
			return;
		}
		Set<SysTenantDatasource> configs = TenantContext.ignore(() ->
			Set.copyOf(datasourceMapper.selectListByQuery(QueryWrapper.create().eq("status", 1))));
		configs.forEach(this::register);
		log.info("租户独立数据源加载完成，共 {} 个", activeTenants.size());
	}

	/** 注册（或重建）某租户数据源 */
	public void register(SysTenantDatasource cfg) {
		if (!(dataSource instanceof FlexDataSource flex)) {
			return;
		}
		String key = KEY_PREFIX + cfg.getTenantCode();
		// 先移除旧的，避免 stale
		removeQuietly(flex, key);
		HikariDataSource ds = new HikariDataSource();
		ds.setJdbcUrl(cfg.getDsUrl());
		ds.setUsername(cfg.getDsUsername());
		ds.setPassword(cfg.getDsPassword());
		ds.setMaximumPoolSize(5);
		ds.setPoolName(key);
		flex.addDataSource(key, ds);
		activeTenants.add(cfg.getTenantCode());
		log.info("注册租户独立数据源 {} -> {}", key, cfg.getDsUrl());
	}

	/** 注销某租户数据源，回落主库 */
	public void unregister(String tenantCode) {
		if (dataSource instanceof FlexDataSource flex) {
			removeQuietly(flex, KEY_PREFIX + tenantCode);
		}
		activeTenants.remove(tenantCode);
	}

	/** 解析当前租户的数据源路由键：启用独立源的租户返回其键，否则主库 */
	public String resolveDsKey(String tenantCode) {
		if (tenantCode != null && activeTenants.contains(tenantCode)) {
			return KEY_PREFIX + tenantCode;
		}
		return DEFAULT_KEY;
	}

	private void removeQuietly(FlexDataSource flex, String key) {
		try {
			if (flex.getDataSourceMap().containsKey(key)) {
				DataSource old = flex.getDataSourceMap().get(key);
				flex.removeDatasource(key);
				if (old instanceof HikariDataSource h) {
					h.close();
				}
			}
		} catch (Exception e) {
			log.warn("移除旧数据源 {} 失败：{}", key, e.getMessage());
		}
	}
}
