package com.github.catstiger.core.db.mapper;

import java.sql.ResultSet;

/**
 * 在RowMapper处理完一条记录（ResultSet的一行）之后，调用RowHandler，对操作Bean进行进一步的操作
 * @author catstiger
 *
 * @param <T>
 */
public interface RowHandler<T> {
  /**
   * 在RowMapper处理完一条记录（ResultSet的一行）之后调用
   * @param bean 当前ResultSet对应的Bean的实例
   * @param rs ResultSet
   * @param rowIndex Index of rs
   */
  public void handle(T bean, ResultSet rs, int rowIndex);
}
