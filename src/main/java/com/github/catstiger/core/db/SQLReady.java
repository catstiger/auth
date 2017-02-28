package com.github.catstiger.core.db;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.github.catstiger.core.db.limit.LimitSql;

/**
 * 用于存放生成的SQL，以及对应的参数
 */
public final class SQLReady {
  private String sql;
  private List<Object> args = new ArrayList<>(10);
  private Map<String, Object> namedParameters = new HashMap<>(0);
  private LimitSql limitSql = SQLRequest.DEFAULT_LIMIT_SQL;
  private List<String> appended = new ArrayList<>(10);
    
  public SQLReady(String sql, Object[] args) {
    this.sql = sql;
    if(args != null) {
      for(Object arg : args) {
        this.args.add(arg);
      }
    }
  }
  
  public SQLReady(String sql, Map<String, Object> namedParameters) {
    if(sql == null) {
      throw new IllegalArgumentException("SQL must not be null.");
    }
    this.sql = sql;
    this.namedParameters = namedParameters;
  }
  
  public SQLReady(String sql, Object[] args, LimitSql limitSql) {
    if(sql == null) {
      throw new IllegalArgumentException("SQL must not be null.");
    }
    
    this.sql = sql;
    if(args != null) {
      for(Object arg : args) {
        this.args.add(arg);
      }
    }
    this.limitSql = limitSql;
  }
  
  public SQLReady(String sql, Map<String, Object> namedParameters, LimitSql limitSql) {
    this.sql = sql;
    this.namedParameters = namedParameters;
    this.limitSql = limitSql;
  }
  
  public String getSql() {
    if(!appended.isEmpty()) {
      StringBuilder sqlTemp = ((sql != null) ? new StringBuilder(sql.length() + 100).append(sql) : new StringBuilder(200));
      for(String sqlSegment : appended) {
        sqlTemp.append(sqlSegment);
      }
      return sqlTemp.toString();
    }
    return sql;
  }
  
  public Object[] getArgs() {
    Object[] objs = new Object[args.size()];
    return args.toArray(objs);
  }
  
  public void setArgs(Object[] args) {
    if(args != null) {
      for(Object arg : args) {
        this.args.add(arg);
      }
    }
  }
  
  public SQLReady addArg(Object arg) {
    this.args.add(arg);
    return this;
  }

  public Map<String, Object> getNamedParameters() {
    return namedParameters;
  }

  public void setNamedParameters(Map<String, Object> namedParameters) {
    this.namedParameters = namedParameters;
  }
  
  /**
   * 追加一段SQL
   * @param sqlSegment 追加一段SQL
   */
  public SQLReady append(String sqlSegment) {
    if(sqlSegment == null) {
      throw new IllegalArgumentException("Sql Segment must not be null.");
    }
    appended.add(sqlSegment);
    return this;
  }
  /**
   * 追加一段SQL，及其参数
   * @param sqlSegment SQL片段
   * @param args 参数
   */
  public SQLReady append(String sqlSegment, Object...args) {
    if(sqlSegment == null) {
      throw new IllegalArgumentException("Sql Segment must not be null.");
    }
    appended.add(sqlSegment);
    if(args != null && args.length > 0) {
      for(Object arg : args) {
        this.args.add(arg);
      }
    }
    return this;
  }
  
  /**
   * 追加一段SQL和一个命名参数以及参数值
   * @param sqlSegment SQL片段
   * @param name 参数名称
   * @param value 参数值
   */
  public SQLReady append(String sqlSegment, String name, Object value) {
    if(sqlSegment == null) {
      throw new IllegalArgumentException("Sql Segment must not be null.");
    }
    appended.add(sqlSegment);
    if(name != null) {
      this.namedParameters.put(name, value);
    }
    return this;
  }
  
  /**
   * 追加一段SQL，和命名参数
   * @param sqlSegment SQL片段
   * @param namedParams 命名参数，KEY为参数名称，Value为参数值
   */
  public SQLReady append(String sqlSegment, Map<String, Object> namedParams) {
    if(sqlSegment == null) {
      throw new IllegalArgumentException("Sql Segment must not be null.");
    }
    appended.add(sqlSegment);
    if(namedParams != null) {
      this.namedParameters.putAll(namedParams);
    }
    return this;
  }
  
  
  
  public String countSql() {
    return SQLFactory.getInstance().countSql(sql);
  }
  
  public String limitSql(int start, int limit) {
    return SQLFactory.getInstance().limitSql(sql, start, limit, limitSql);
  }

  public SQLReady withLimitSql(LimitSql limitSql) {
    this.limitSql = limitSql;
    return this;
  }
}

