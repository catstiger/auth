package com.github.catstiger.core.db.limit;

public interface LimitSql {
  /**
   * 根据原始SQL，创建分页查询SQL
   * @param sql 原始SQL
   * @param start 起始位置，第一位为0
   * @param limit 抓取最多行数
   * @return Limit Sql
   */
  String getLimitSql(String sql, int start, int limit);
}
