package com.github.catstiger.core.db;

public interface DbSync {
  /**
   * 判断给定的实体类对应的table是否存在
   * @param modelClass 给出实体类
   * @return 如果实体类存在，返回true， 否则返回false
   */
  public boolean isTableExists(Class<?> modelClass);
  /**
   * 判断给定的field对应的column是否存在
   * @param modelClass 给出实体类
   * @param fieldName 给出属性名，如果属性被@java.beans.Transient标注，则不处理
   * @return 如果字段存在，返回true，否则返回false
   */
  public boolean isColumnExists(Class<?> modelClass, String fieldName);
  /**
   * 判断索引是否存在
   * @param modelClass 给出实体类
   * @param indexName 给出字段名
   * @return 如果字段被@Index标注，则执行判断，根据@Index的名称和表名，判断索引是否建立
   */
  public boolean isIndexExists(Class<?> modelClass, String indexName);
  
  /**
   * 根据modelClass，创建一个table
   */
  public void createTable(Class<?> modelClass);
  /**
   * 根据字段的@Index标注，创建索引
   */
  public void createIndex(Class<?> modelClass, String fieldName);
  
  /**
   * 为给定的表，创建一个字段，支持Unique，索引，Not Null；支持的类型包括int, bigint, double, varchar, text, time, date, boolean
   */
  public void addColumn(Class<?> modelClass, String fieldName);
  
  /**
   * 创建一个外键
   * @param modelClass 给出实体类
   * @param refClass 给出引用的实体类
   * @param fieldName 给出实体类的字段名
   */
  public void addForeignKey(Class<?> modelClass, Class<?> refClass, String fieldName);
}
