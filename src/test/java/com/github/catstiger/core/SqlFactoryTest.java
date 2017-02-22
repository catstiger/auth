package com.github.catstiger.core;

import java.util.Arrays;
import java.util.Date;

import org.junit.Test;
import org.springframework.util.Assert;

import com.github.catstiger.auth.model.SysUser;
import com.github.catstiger.core.db.SQLFactory;
import com.github.catstiger.core.db.SQLFactory.SQLReady;
import com.github.catstiger.core.db.SQLFactory.SQLRequest;
import com.github.catstiger.core.db.model.TestDbModel;
import com.github.catstiger.core.db.ns.AbbreviationNamingStrategy;

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
  
  @Test
  public void testConditions() {
    TestDbModel tdm = new TestDbModel();
    tdm.setId(22143L);
    tdm.setLastModified(new Date());
    tdm.setLastModifiedEnd(new Date(System.currentTimeMillis() + 1000000000L));
    tdm.setPrice(8895.3D);
    tdm.setPriceEnd(87594.32D);
    tdm.setPriceStart(848.03D);
    tdm.setRadis(89.988F);
    tdm.setDescn("datasdlksfdl乐山大佛乐山大佛");
    tdm.setUsername("sam");
    tdm.setRealName("abc");
    
    SQLReady sqlReady = SQLFactory.getInstance().conditions(new SQLRequest(tdm));
    System.out.println(sqlReady.getSql());
    System.out.println(Arrays.toString(sqlReady.getArgs()));
    
    int count = 0;
    for(int i = 0; i < sqlReady.getSql().length(); i++) {
      char c = sqlReady.getSql().charAt(i);
      if(c == '?') {
        count++;
      }
    }
    Assert.isTrue(count == sqlReady.getArgs().length);
  }
  
  @Test
  public void testConditions2() {
    TestDbModel tdm = new TestDbModel();
    tdm.setId(22143L);
    tdm.setLastModified(new Date());
    tdm.setLastModifiedEnd(new Date(System.currentTimeMillis() + 1000000000L));
    tdm.setPrice(8895.3D);
    tdm.setPriceEnd(87594.32D);
    tdm.setPriceStart(848.03D);
    tdm.setRadis(89.988F);
    tdm.setDescn("datasdlksfdl乐山大佛乐山大佛");
    tdm.setUsername("sam");
    tdm.setRealName("abc");
    
    SQLReady sqlReady = SQLFactory.getInstance().conditions(new SQLRequest(tdm).usingAlias(true).excludes("radis"));
    System.out.println(sqlReady.getSql());
    System.out.println(sqlReady.getNamedParameters());
    
    int count = 0;
    for(int i = 0; i < sqlReady.getSql().length(); i++) {
      char c = sqlReady.getSql().charAt(i);
      if(c == ':') {
        count++;
      }
    }
    Assert.isTrue(count == sqlReady.getNamedParameters().size());
  }
  
  @Test
  public void testUpdate() {
    TestDbModel tdm = new TestDbModel();
    tdm.setId(586L);
    tdm.setLastModified(new Date());
    tdm.setLastModifiedEnd(new Date(System.currentTimeMillis() + 1000000000L));
    tdm.setPrice(8895.3D);
    tdm.setPriceEnd(87594.32D);
    tdm.setPriceStart(848.03D);
    tdm.setRadis(89.988F);
    tdm.setDescn("datasdlksfdl乐山大佛乐山大佛");
    tdm.setUsername("sam");
    
    SQLReady sr = SQLFactory.getInstance().updateDynamic(new SQLRequest(tdm), false);
    System.out.println(sr.getSql());
    System.out.println(Arrays.toString(sr.getArgs()));
    
    sr = SQLFactory.getInstance().updateDynamic(new SQLRequest(tdm), true);
    System.out.println(sr.getSql());
    System.out.println(Arrays.toString(sr.getArgs()));
    
    sr = SQLFactory.getInstance().updateDynamic(new SQLRequest(tdm).usingAlias(true), true);
    System.out.println(sr.getSql());
    System.out.println(sr.getNamedParameters());
  }
  
  @Test
  public void testInsert2() {
    TestDbModel tdm = new TestDbModel();
    tdm.setId(SQLFactory.DEF_IDGEN.nextId());
    tdm.setLastModified(new Date());
    tdm.setLastModifiedEnd(new Date(System.currentTimeMillis() + 1000000000L));
    tdm.setPrice(8895.3D);
    tdm.setPriceEnd(87594.32D);
    tdm.setPriceStart(848.03D);
    tdm.setRadis(89.988F);
    tdm.setDescn("datasdlksfdl乐山大佛乐山大佛");
    tdm.setUsername("sam");
    
    SQLReady sr = SQLFactory.getInstance().insertDynamic(new SQLRequest(tdm));
    System.out.println(sr.getSql());
    System.out.println(Arrays.toString(sr.getArgs()));
  }
  
}
