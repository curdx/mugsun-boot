package com.mugsun.boot.system.service;

import com.alicp.jetcache.anno.CacheInvalidate;
import com.alicp.jetcache.anno.CacheType;
import com.alicp.jetcache.anno.Cached;
import com.mugsun.boot.system.entity.SysParam;
import com.mugsun.boot.system.mapper.SysParamMapper;
import com.mybatisflex.core.query.QueryWrapper;
import org.springframework.stereotype.Service;

/**
 * 参数服务：参数取值走本地缓存，写操作按键失效
 */
@Service
public class ParamService {

	private final SysParamMapper paramMapper;

	public ParamService(SysParamMapper paramMapper) {
		this.paramMapper = paramMapper;
	}

	/** 按键取参数值（缓存） */
	@Cached(name = "param:", key = "#paramKey", cacheType = CacheType.LOCAL)
	public String getValue(String paramKey) {
		SysParam param = paramMapper.selectOneByQuery(QueryWrapper.create().eq("param_key", paramKey));
		return param == null ? null : param.getParamValue();
	}

	/** 失效指定键的参数缓存 */
	@CacheInvalidate(name = "param:", key = "#paramKey")
	public void evict(String paramKey) {
	}
}
