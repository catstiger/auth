package com.github.catstiger.core.db;

import java.sql.Connection;
import java.sql.SQLException;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import javax.sql.DataSource;

import org.springframework.stereotype.Component;

import com.github.catstiger.utils.StringUtils;

/**
 * 用于探测数据库类型
 *
 */
@Component
public class DatabaseDetector {
  @Resource
  private DataSource ds;
  private String dbName;
  
  @PostConstruct
  void init() {
    if(StringUtils.isBlank(dbName)) {
      Connection conn = null;
      try {
        conn = ds.getConnection();
        dbName = conn.getMetaData().getDatabaseProductName();
      } catch (SQLException e) {
        throw new RuntimeException(e);
      } finally {
        try {
          conn.close();
        } catch (SQLException e) {
          throw new RuntimeException(e);
        }
      }
      
      if(StringUtils.isBlank(dbName)) {
        throw new RuntimeException("Do not find database name!");
      }
    }
  }
  
  public String getDatabaseName() {
    return dbName;
  }
  
  public Boolean isH2() {
    return dbName.toUpperCase().indexOf("H2") >= 0;
  }
  
  public Boolean isMySql() {
    return dbName.toUpperCase().indexOf("MYSQL") >= 0;
  }
  
  public Boolean isOracle() {
    return dbName.toUpperCase().indexOf("ORACLE") >= 0;
  }
}
