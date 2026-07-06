package com.mugsun.boot.system.service;

import com.alicp.jetcache.anno.CacheInvalidate;
import com.alicp.jetcache.anno.CacheType;
import com.alicp.jetcache.anno.Cached;
import com.mugsun.boot.system.entity.SysDict;
import com.mugsun.boot.system.mapper.SysDictMapper;
import com.mybatisflex.core.query.QueryWrapper;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 字典服务：字典项查询走本地缓存，写操作按编码失效
 */
@Service
public class DictService {

	private final SysDictMapper dictMapper;

	public DictService(SysDictMapper dictMapper) {
		this.dictMapper = dictMapper;
	}

	/** 按编码查字典项（缓存，排除分类节点） */
	@Cached(name = "dict:item:", key = "#code", cacheType = CacheType.LOCAL)
	public List<SysDict> listItems(String code) {
		return dictMapper.selectListByQuery(QueryWrapper.create()
			.eq("code", code).ne("parent_id", 0L).orderBy("sort", true));
	}

	/** 失效指定编码的字典项缓存 */
	@CacheInvalidate(name = "dict:item:", key = "#code")
	public void evict(String code) {
	}

	/** 按编码+键翻译为字典标签（找不到原样返回值） */
	public String translate(String code, String value) {
		if (value == null) {
			return null;
		}
		for (SysDict item : listItems(code)) {
			if (value.equals(item.getDictKey())) {
				return item.getDictValue();
			}
		}
		return value;
	}
}
