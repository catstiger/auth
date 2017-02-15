package com.github.catstiger.core.db.sync.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(value={ElementType.FIELD,ElementType.METHOD})
@Retention(value=RetentionPolicy.RUNTIME)
public @interface Index {
  /**
   * 索引名称，空字符串表示有系统自定义
   */
  String name() default "";
  
  /**
   * 索引涉及的列，不填表示当前列
   */
  String[] columnNames() default {};
  
  /**
   * 是否唯一索引
   */
  boolean unique() default false;
}
