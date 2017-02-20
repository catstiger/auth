package com.github.catstiger.core.db;

public class MySqlLimitSql implements LimitSql {

  @Override
  public String getLimitSql(String sql, int start, int limit) {
    return new StringBuilder(sql.length() + 20).append(sql)
        .append(" limit ").append(start).append(",").append(limit).toString();
  }

}
