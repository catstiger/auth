package com.github.catstiger.core.db.mapper;

import java.util.Arrays;

import javax.annotation.Resource;

import org.junit.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import com.github.catstiger.auth.model.SysUser;
import com.github.catstiger.auth.test.SpringTestCase;
import com.github.catstiger.core.db.SQLFactory;
import com.github.catstiger.core.db.SQLFactory.SQLReady;
import com.github.catstiger.core.db.SQLFactory.SQLRequest;
import com.github.catstiger.core.db.id.IdGen;

public class BeanRowMapperTest extends SpringTestCase {
  @Resource
  JdbcTemplate jdbcTemplate;
  @Resource
  IdGen idGen;
  
  @Test
  public void testA() {
    SysUser sysUser = new SysUser();
    sysUser.setId(idGen.nextId());
    sysUser.setUsername("abc");
    sysUser.setMobile("13930119546");
    sysUser.setPassword("111111");
    SQLRequest sqlReq = new SQLRequest(sysUser).usingAlias(false);
    
    SQLReady sqlReady = SQLFactory.getInstance().insertDynamic(sqlReq);
    System.out.println(sqlReady.getSql());
    System.out.println(Arrays.toString(sqlReady.getArgs()));
    
    sqlReq = new SQLRequest(SysUser.class).usingAlias(false);
    String sql = SQLFactory.getInstance().update(sqlReq);
    System.out.println(sql);
  }
}
