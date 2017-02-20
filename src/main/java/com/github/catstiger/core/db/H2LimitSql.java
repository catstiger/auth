package com.github.catstiger.core.db;

public class H2LimitSql implements LimitSql {

  @Override
  public String getLimitSql(String sql, int start, int limit) {
    return new StringBuffer(sql.length() + 20) // hasOffset ? " limit ? offset ?" : " limit ?" 
    .append(sql)
    .append(" limit ").append(limit).append(" offset ").append(start)
    .toString();
  }

}
