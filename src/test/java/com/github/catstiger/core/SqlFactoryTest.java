package com.github.catstiger.core;

import org.junit.Test;

import com.github.catstiger.auth.model.SysUser;
import com.github.catstiger.core.db.SQLFactory;
import com.github.catstiger.core.db.SQLFactory.SQLRequest;
import com.github.catstiger.core.db.mapper.ns.AbbreviationNamingStrategy;

public class SqlFactoryTest {
  @Test
  public void testSelect() {
    SQLRequest sqlRequest = new SQLRequest(SysUser.class);
    String sql = SQLFactory.getInstance().select(sqlRequest);
    System.out.println(sql);
    
    sqlRequest = sqlRequest.usingAlias(true);
    sql = SQLFactory.getInstance().select(sqlRequest);
    System.out.println(sql);
    
    
    SQLRequest sqlRequest1 = new SQLRequest(SysUser.class).namingStrategy(new AbbreviationNamingStrategy()).usingAlias(true);
    sql = SQLFactory.getInstance().select(sqlRequest1);
    System.out.println(sql);
  }
  @Test
  public void testInsert() {
    SQLRequest sqlRequest = new SQLRequest(SysUser.class);
    String sql = SQLFactory.getInstance().insert(sqlRequest);
    System.out.println(sql);
    
    sqlRequest = sqlRequest.usingAlias(true);
    sql = SQLFactory.getInstance().insert(sqlRequest);
    System.out.println(sql);
  }
  
}
