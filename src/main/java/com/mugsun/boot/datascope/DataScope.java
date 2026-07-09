package com.mugsun.boot.datascope;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 数据权限注解：标注在查询方法上，配合 {@link DataScopeAspect} 按当前用户角色的数据范围自动构建过滤条件。
 * 方法内以 {@link DataScopeHolder#apply} 将条件应用到 QueryWrapper。
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface DataScope {

	/** 部门归属列（本部门/及子/自定义部门范围按此列过滤） */
	String deptColumn() default "dept_id";

	/** 本人归属列（仅本人范围按此列过滤，通常为主键或创建人列） */
	String userColumn() default "id";
}
