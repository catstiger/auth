package com.github.catstiger.core.db.mysql;

import javax.annotation.Resource;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import com.github.catstiger.core.db.ColumnCreator;
import com.github.catstiger.core.db.model.ColumnModel;

@Service
public class MySqlColumnCreator implements ColumnCreator {
  @Resource
  private JdbcTemplate jdbcTemplate;
  
  @Override
  public ColumnModel addColumnIfNotExists(Class<?> entityClass, String field) {
    
    return null;
  }

  @Override
  public String getColumnSqlFragment(Class<?> entityClass, String field) {
    
    return null;
  }

  @Override
  public void addForeignKeyIfNotExists(Class<?> entityClass, String field, Class<?> refClass, String refField) {
    

  }

  @Override
  public void isColumnExists(Class<?> entityClass, String field) {

  }

}
