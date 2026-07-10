package com.mugsun.boot.datasource;

import cn.dev33.satoken.annotation.SaCheckLogin;
import com.mugsun.boot.datasource.entity.SysTenantDatasource;
import com.mugsun.boot.datasource.mapper.SysTenantDatasourceMapper;
import com.mugsun.core.tool.api.R;
import com.mugsun.core.tool.exception.ServiceException;
import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.query.QueryWrapper;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 租户独立数据源配置（平台级，超管操作）：CRUD + 保存即运行时注册/注销。
 */
@RestController
@RequestMapping("/system/tenant-datasource")
@SaCheckLogin
public class TenantDatasourceController {

	private final SysTenantDatasourceMapper datasourceMapper;
	private final TenantDataSourceRegistry registry;

	public TenantDatasourceController(SysTenantDatasourceMapper datasourceMapper, TenantDataSourceRegistry registry) {
		this.datasourceMapper = datasourceMapper;
		this.registry = registry;
	}

	@GetMapping("/page")
	public R<Page<SysTenantDatasource>> page(@RequestParam(defaultValue = "1") long pageNum,
											 @RequestParam(defaultValue = "10") long pageSize) {
		Page<SysTenantDatasource> page = datasourceMapper.paginate(pageNum, pageSize,
			QueryWrapper.create().orderBy("id", false));
		page.getRecords().forEach(c -> c.setDsPassword("******"));
		return R.data(page);
	}

	@PostMapping("/submit")
	public R<Void> submit(@RequestBody SysTenantDatasource param) {
		if (param.getTenantCode() == null || param.getTenantCode().isBlank()) {
			throw new ServiceException("租户编号不能为空");
		}
		if (param.getDsUrl() == null || param.getDsUrl().isBlank()) {
			throw new ServiceException("数据源 URL 不能为空");
		}
		if (param.getId() == null) {
			if (param.getStatus() == null) {
				param.setStatus(1);
			}
			datasourceMapper.insertSelective(param);
		} else {
			// 密码占位符时不覆盖原密码
			if ("******".equals(param.getDsPassword())) {
				SysTenantDatasource db = datasourceMapper.selectOneById(param.getId());
				param.setDsPassword(db == null ? param.getDsPassword() : db.getDsPassword());
			}
			datasourceMapper.update(param);
		}
		// 保存即注册/注销
		SysTenantDatasource full = datasourceMapper.selectOneByQuery(
			QueryWrapper.create().eq("tenant_code", param.getTenantCode()));
		if (full != null && Integer.valueOf(1).equals(full.getStatus())) {
			registry.register(full);
		} else if (full != null) {
			registry.unregister(full.getTenantCode());
		}
		return R.success("保存成功");
	}

	@PostMapping("/remove")
	public R<Void> remove(@RequestBody List<Long> ids) {
		ids.forEach(id -> {
			SysTenantDatasource cfg = datasourceMapper.selectOneById(id);
			if (cfg != null) {
				registry.unregister(cfg.getTenantCode());
			}
		});
		datasourceMapper.deleteBatchByIds(ids);
		return R.success("删除成功");
	}
}
