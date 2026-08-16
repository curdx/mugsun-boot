package com.mugsun.boot.gis;

import com.mugsun.boot.system.service.ParamService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * GIS 模块开关：yml 与 sys_param 同时为开才启用；默认都开。
 */
@Service
public class GisModuleService {

	private final ParamService paramService;

	@Value("${mugsun.gis.enabled:true}")
	private boolean yamlEnabled;

	public GisModuleService(ParamService paramService) {
		this.paramService = paramService;
	}

	public boolean isEnabled() {
		if (!yamlEnabled) {
			return false;
		}
		String v = paramService.getValue(GisConstants.PARAM_MODULE_ENABLED);
		if (v == null || v.isBlank()) {
			return true;
		}
		return "true".equalsIgnoreCase(v.trim()) || "1".equals(v.trim());
	}

	public void requireEnabled() {
		if (!isEnabled()) {
			throw new com.mugsun.core.tool.exception.ServiceException(GisConstants.MSG_DISABLED);
		}
	}
}
