package com.github.catstiger.auth.dao;

import java.time.Duration;
import java.util.Date;

import javax.annotation.Resource;

import org.junit.Test;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.transaction.annotation.Transactional;

import com.alibaba.fastjson.JSON;
import com.github.catstiger.auth.model.SysUser;
import com.github.catstiger.auth.test.SpringTestCase;
import com.github.catstiger.core.db.SQLReady;
import com.github.catstiger.core.db.SQLRequest;
import com.github.catstiger.core.db.mapper.BeanRowMapper;

import reactor.core.support.Assert;

@Transactional
public class SysUserDaoTest extends SpringTestCase {
  @Resource
  private SysUserDao dao;
  @Resource
  private JdbcTemplate jdbcTemplate;
  
  @Test
  public void testByMobile() {
    SysUser user = dao.byMobile("13930119546");
    System.out.println(JSON.toJSONString(user));
    Assert.notNull(user);
  }
  
  @Test
  public void testInsert() {
    SysUser user = new SysUser();
    user.setUsername("jtigers");
    user.setPassword("111111");
    user.setMobile("15833211116");
    user.setRegistTime(new Date(System.currentTimeMillis()));
    
    user = dao.insert(user);
    Assert.notNull(user.getId());
  }
  
  @Test
  public void testBeanProperty() {
    SQLReady sqlReady = new SQLRequest(SysUser.class).select();
    RowMapper<SysUser> rm = new BeanPropertyRowMapper<>(SysUser.class);
    long begin = System.currentTimeMillis();
    for(int i = 0; i < 10000; i++) {
      jdbcTemplate.query(sqlReady.getSql(), rm);
    }
    long end = System.currentTimeMillis();
    System.out.println("Spring : " + Duration.ofMillis(end - begin).toString());
  }
  
  @Test
  public void testBeanRowMapper() {
    SQLReady sqlReady = new SQLRequest(SysUser.class).select();
    RowMapper<SysUser> rmb = new BeanRowMapper<>(SysUser.class);
    long begin = System.currentTimeMillis();
    for(int i = 0; i < 10000; i++) {
      jdbcTemplate.query(sqlReady.getSql(), rmb);
    }
    long end = System.currentTimeMillis();
    System.out.println("Mine : " + Duration.ofMillis(end - begin).toString());
  }
}
