package com.github.catstiger.core.db;

import com.github.catstiger.core.db.model.ColumnModel;

public interface ColumnCreator {
  /**
   * 添加一个字段
   */
  public ColumnModel addColumnIfNotExists(Class<?> entityClass, String field);
  
  /**
   * 得到创建Column的SQL片段
   */
  public String getColumnSqlFragment(Class<?> entityClass, String field);
  
  /**
   * 添加一个外键
   *
   */
  public void addForeignKeyIfNotExists(Class<?> entityClass, String field, Class<?> refClass, String refField);
  
  /**
   * 判断表里面有没有对应的字段
   */
  public void isColumnExists(Class<?> entityClass, String field);
  
  
}
