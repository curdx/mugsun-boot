package com.mugsun.boot.datasource;

import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.stp.StpUtil;
import com.mugsun.boot.datasource.entity.BizCustomer;
import com.mugsun.boot.datasource.mapper.BizCustomerMapper;
import com.mugsun.core.tool.api.R;
import com.mugsun.core.tool.exception.ServiceException;
import com.mybatisflex.core.datasource.DataSourceKey;
import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.query.QueryWrapper;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 演示业务：客户 CRUD。按当前租户的独立数据源配置路由——
 * 启用独立源的租户其数据落独立库，其余落主库。用于验证动态多源隔离。
 */
@RestController
@RequestMapping("/system/customer")
@SaCheckLogin
public class BizCustomerController {

	private final BizCustomerMapper customerMapper;
	private final TenantDataSourceRegistry registry;

	public BizCustomerController(BizCustomerMapper customerMapper, TenantDataSourceRegistry registry) {
		this.customerMapper = customerMapper;
		this.registry = registry;
	}

	@GetMapping("/page")
	public R<Page<BizCustomer>> page(@RequestParam(defaultValue = "1") long pageNum,
									 @RequestParam(defaultValue = "10") long pageSize) {
		return R.data(routed(() ->
			customerMapper.paginate(pageNum, pageSize, QueryWrapper.create().orderBy("id", false))));
	}

	@PostMapping("/submit")
	public R<Void> submit(@RequestBody BizCustomer param) {
		if (param.getName() == null || param.getName().isBlank()) {
			throw new ServiceException("客户名称不能为空");
		}
		routed(() -> {
			if (param.getId() == null) {
				customerMapper.insertSelective(param);
			} else {
				customerMapper.update(param);
			}
			return null;
		});
		return R.success("保存成功");
	}

	@PostMapping("/remove")
	public R<Void> remove(@RequestBody List<Long> ids) {
		routed(() -> {
			customerMapper.deleteBatchByIds(ids);
			return null;
		});
		return R.success("删除成功");
	}

	/** 在当前租户对应数据源上执行 */
	private <T> T routed(java.util.function.Supplier<T> action) {
		return DataSourceKey.use(registry.resolveDsKey(currentTenantCode()), action);
	}

	private String currentTenantCode() {
		Object code = StpUtil.getSession().get("tenantId");
		return code == null ? null : code.toString();
	}
}
