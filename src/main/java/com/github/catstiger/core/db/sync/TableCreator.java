package com.github.catstiger.core.db.sync;

public interface TableCreator {
  /**
   * 创建EntityClass对应的表，如果已经存在，则忽略
   */
  public void createTableIfNotExists(Class<?> entityClass);
  
  /**
   * 如果对应的表已经存在，则返回true
   */
  public Boolean isTableExists(Class<?> entityClass);
  
  
  public void updateTable(Class<?> entityClass);
}
