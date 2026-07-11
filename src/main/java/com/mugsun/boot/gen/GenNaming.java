package com.mugsun.boot.gen;

/**
 * 代码生成命名约定：下划线↔驼峰↔短横线，表前缀剥离。
 */
public final class GenNaming {

	private GenNaming() {
	}

	/** 剥离表前缀（大小写不敏感），如 gen_product / gen_ → product */
	public static String stripPrefix(String name, String prefix) {
		if (name != null && prefix != null && !prefix.isBlank()
			&& name.regionMatches(true, 0, prefix, 0, prefix.length())) {
			return name.substring(prefix.length());
		}
		return name;
	}

	/** 下划线转驼峰；upperFirst 控制首字母大小写（sys_dict_type → SysDictType / sysDictType） */
	public static String toCamel(String name, boolean upperFirst) {
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

	/** 下划线转短横线（dict_type → dict-type），用于前端路由/接口路径 */
	public static String toKebab(String name) {
		return name == null ? null : name.replace('_', '-');
	}
}
