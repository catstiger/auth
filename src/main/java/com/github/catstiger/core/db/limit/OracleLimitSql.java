package com.github.catstiger.core.db.limit;

public class OracleLimitSql implements LimitSql {

  @Override
  public String getLimitSql(String sql, int start, int limit) {
    sql = sql.trim();
    boolean isForUpdate = false;
    if (sql.toLowerCase().endsWith(" for update")) {
      sql = sql.substring(0, sql.length() - 11);
      isForUpdate = true;
    }

    StringBuffer pagingSelect = new StringBuffer(sql.length() + 100);
    if (start > 0) {
      pagingSelect.append("select * from ( select row_.*, rownum rownum_ from ( ");
    } else {
      pagingSelect.append("select * from ( ");
    }
    pagingSelect.append(sql);
    if (start > 0) {
      pagingSelect.append(" ) row_ where rownum <= ").append(start + limit).append(") where rownum_ > ").append(start);
    } else {
      pagingSelect.append(" ) where rownum <= ").append(limit);
    }

    if (isForUpdate) {
      pagingSelect.append(" for update");
    }

    return pagingSelect.toString();
  }

}
