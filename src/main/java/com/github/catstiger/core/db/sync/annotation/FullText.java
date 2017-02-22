package com.github.catstiger.core.db.sync.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(value={ElementType.METHOD, ElementType.FIELD})
@Retention(value=RetentionPolicy.RUNTIME)
public @interface FullText {
  /**
   * 全文检索有时候不能在本字段检索，需要将字段内容编码后存入另外一个字段，
   * relativeColumn用于指出该字段的字段名。
   */
  String relativeColumn() default "";
}
