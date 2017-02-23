package com.github.catstiger.core.db;

import java.beans.PropertyDescriptor;
import java.lang.reflect.Field;
import java.sql.ResultSet;

public interface NamingStrategy {
  /**
   * 根据实体类，得到表名
   */
  public String tablename(Class<?> entityClass);
  
  /**
   * 根据实体名，得到表名别名
   */
  public String tableAlias(Class<?> entityClass);
 
  /**
   * 根据PropertyDescriptor，得到columnLabel, 用于从ResultSet中获取数据
   */
  public String columnLabel(PropertyDescriptor propDesc);
  
  /**
   * 根据原始的Column Label，获得规则中的的ColumnLabel
   */
  public String columnLabel(ResultSet rs, int columnIndex);
  
  /**
   * 根据原始的Column Name，获得规则中的的ColumnLabel
   */
  public String columnLabel(String column);
  
  /**
   * 根据Field Name，得到columnName, 用于生成SQL
   */
  public String columnName(Class<?> entityClass, String field);
  
  /**
   * 根据Field，得到columnName, 用于生成SQL
   */
  public String columnName(Class<?> entityClass, Field field);
}
