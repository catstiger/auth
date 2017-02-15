package com.github.catstiger.core.db.sync;

public interface IndexCreator {
  /**
   * 根据@Index, 创建一个数据库索引，如果索引存在则忽略
   */
  public void addIndexIfNotExists(Class<?> entityClass, String field);
}
