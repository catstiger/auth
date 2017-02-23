package com.github.catstiger.core.db;

import java.util.HashMap;
import java.util.Map;

import com.github.catstiger.core.db.limit.LimitSql;

/**
 * 用于存放生成的SQL，以及对应的参数
 */
public final class SQLReady {
  private String sql;
  private Object[] args = new Object[]{};
  private Map<String, Object> namedParameters = new HashMap<>(0);
  private LimitSql limitSql = SQLRequest.DEFAULT_LIMIT_SQL;
  
  public SQLReady() {
    
  }
  
  public SQLReady(String sql, Object[] args) {
    this.sql = sql;
    this.args = args;
  }
  
  public SQLReady(String sql, Map<String, Object> namedParameters) {
    this.sql = sql;
    this.namedParameters = namedParameters;
  }
  
  public SQLReady(String sql, Object[] args, LimitSql limitSql) {
    this.sql = sql;
    this.args = args;
    this.limitSql = limitSql;
  }
  
  public SQLReady(String sql, Map<String, Object> namedParameters, LimitSql limitSql) {
    this.sql = sql;
    this.namedParameters = namedParameters;
    this.limitSql = limitSql;
  }
  
  public String getSql() {
    return sql;
  }
  
  public void setSql(String sql) {
    this.sql = sql;
  }
  
  public Object[] getArgs() {
    return args;
  }
  
  public void setArgs(Object[] args) {
    this.args = args;
  }

  public Map<String, Object> getNamedParameters() {
    return namedParameters;
  }

  public void setNamedParameters(Map<String, Object> namedParameters) {
    this.namedParameters = namedParameters;
  }
  
  public String countSql() {
    return SQLFactory.getInstance().countSql(sql);
  }
  
  public String limitSql(int start, int limit) {
    return SQLFactory.getInstance().limitSql(sql, start, limit, limitSql);
  }

  public void setLimitSql(LimitSql limitSql) {
    this.limitSql = limitSql;
  }
}

