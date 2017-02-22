package com.github.catstiger.core.db.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(value={ElementType.METHOD, ElementType.FIELD})
@Retention(value=RetentionPolicy.RUNTIME)
public @interface RangeQuery {
  /**
   * 范围查询，需要指出存储上限的属性名
   */
  String start() default "";
  
  /**
   * 范围查询，需要指出存储下线的属性名
   */
  String end() default "";
  
  /**
   * 是否使用大于或者等于，如果为false，则使用大于，缺省为true
   */
  boolean greatAndEquals() default true;
  
  /**
   * 是否使用小于或者等于，如果为false，则使用小于，缺省为true
   */
  boolean lessAndEquals() default true;
}
