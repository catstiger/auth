package com.github.catstiger.core;

import java.util.Arrays;
import java.util.Date;

import org.junit.Test;
import org.springframework.util.Assert;

import com.github.catstiger.auth.model.SysUser;
import com.github.catstiger.auth.model.UserApp;
import com.github.catstiger.core.db.SQLFactory;
import com.github.catstiger.core.db.SQLReady;
import com.github.catstiger.core.db.SQLRequest;
import com.github.catstiger.core.db.model.TestDbModel;
import com.github.catstiger.core.db.model.TestRefModel;
import com.github.catstiger.core.db.ns.AbbreviationNamingStrategy;
import com.github.catstiger.utils.StringUtils;

public class SqlFactoryTest {
  @Test
  public void testSelect() {
    TestDbModel tdm = new TestDbModel();
    tdm.setLastModified(new Date());
    tdm.setPrice(8895.3D);
    tdm.setRadis(89.988F);
    tdm.setDescn("datasdlksfdl乐山大佛乐山大佛");
    tdm.setUsername("sam");
    tdm.setRealName("abc");
    
    TestRefModel ref = new TestRefModel();
    ref.setId(9985L);
    ref.setTitle("ESSEEESSSEESS");
    tdm.setRefModel(ref);
    
    
    SQLRequest sqlRequest = new SQLRequest(tdm).usingAlias(true);
    String sql = SQLFactory.getInstance().select(sqlRequest, true).getSql();
    System.out.println(sql);
    
    sqlRequest = new SQLRequest(tdm).usingAlias(false);
    sql = SQLFactory.getInstance().select(sqlRequest, true).getSql();
    System.out.println(sql);
    
    sqlRequest = new SQLRequest(tdm).usingAlias(false).byId(true);
    sql = SQLFactory.getInstance().select(sqlRequest, true).getSql();
    System.out.println(sql);
    
    
    SQLRequest sqlRequest1 = new SQLRequest(tdm).namingStrategy(new AbbreviationNamingStrategy()).usingAlias(true);
    sql = SQLFactory.getInstance().select(sqlRequest1,true).getSql();
    System.out.println(sql);
    
    UserApp app = new UserApp();
    SysUser su = new SysUser();
    su.setId(9884234L);
    app.setId(224554L);
    app.setOwner(su);
    app.setDescn(StringUtils.random(12));
    
    SQLRequest sr = new SQLRequest(app).usingAlias(true);
    sql = SQLFactory.getInstance().select(sr, false).getSql();
    System.out.println(sql);
  }
  @Test
  public void testInsert() {
    TestDbModel tdm = new TestDbModel();
    tdm.setLastModified(new Date());
    tdm.setPrice(8895.3D);
    tdm.setRadis(89.988F);
    tdm.setDescn("datasdlksfdl乐山大佛乐山大佛");
    tdm.setUsername("sam");
    tdm.setRealName("abc");
    
    SQLRequest sqlRequest = new SQLRequest(tdm).includesNull(true);
    SQLReady sr = SQLFactory.getInstance().insert(sqlRequest);
    System.out.println(sr.getSql());
    System.out.println(Arrays.toString(sr.getArgs()));
    
    sr = SQLFactory.getInstance().insert(sqlRequest.includesNull(false));
    System.out.println(sr.getSql());
    System.out.println(Arrays.toString(sr.getArgs()));
    
    sr = SQLFactory.getInstance().insert(sqlRequest.includesNull(true).namedParams(true));
    System.out.println(sr.getSql());
    System.out.println(sr.getNamedParameters());
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
    TestRefModel ref = new TestRefModel();
    ref.setId(9985L);
    ref.setTitle("ESSEEESSSEESS");
    tdm.setRefModel(ref);
    
    SQLReady sqlReady = SQLFactory.getInstance().conditions(new SQLRequest(tdm).usingAlias(false).namedParams(false), true);
    System.out.println("#######不使用别名，也不使用命名参数");
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
    System.out.println("#######使用别名，不使用命名参数");
    sqlReady = SQLFactory.getInstance().conditions(new SQLRequest(tdm).usingAlias(true).namedParams(false), true);
    System.out.println(sqlReady.getSql());
    System.out.println(Arrays.toString(sqlReady.getArgs()));
    
    System.out.println("#######不使用别名，使用命名参数");
    sqlReady = SQLFactory.getInstance().conditions(new SQLRequest(tdm).usingAlias(false).namedParams(true), true);
    System.out.println(sqlReady.getSql());
    System.out.println(Arrays.toString(sqlReady.getArgs()));
    
    System.out.println("#######使用别名+命名参数");
    sqlReady = SQLFactory.getInstance().conditions(new SQLRequest(tdm).usingAlias(true).namedParams(true), true);
    System.out.println(sqlReady.getSql());
    System.out.println(Arrays.toString(sqlReady.getArgs()));
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
    
    SQLReady sqlReady = SQLFactory.getInstance().conditions(new SQLRequest(tdm).usingAlias(true).excludes("radis").namedParams(true), false);
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
    
    SQLReady sr = SQLFactory.getInstance().update(new SQLRequest(tdm).byId(true));
    System.out.println("##########根据ID，不使用别名，不使用命名参数，不包括NULL");
    System.out.println(sr.getSql());
    System.out.println(Arrays.toString(sr.getArgs()));
    
    System.out.println("##########不使用别名，不使用命名参数，包括NULL");
    sr = SQLFactory.getInstance().update(new SQLRequest(tdm).includesNull(true));
    System.out.println(sr.getSql());
    System.out.println(Arrays.toString(sr.getArgs()));
    
    System.out.println("##########使用别名，使用命名参数，不包括NULL");
    sr = SQLFactory.getInstance().update(new SQLRequest(tdm).usingAlias(true).includesNull(false).byId(true).namedParams(true));
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
    
    SQLReady sr = SQLFactory.getInstance().insert(new SQLRequest(tdm).namedParams(false));
    System.out.println(sr.getSql());
    System.out.println(Arrays.toString(sr.getArgs()));
  }
  
}
