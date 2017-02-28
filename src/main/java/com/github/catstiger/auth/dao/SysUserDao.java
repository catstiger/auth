package com.github.catstiger.auth.dao;

import java.util.List;

import javax.annotation.Resource;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.github.catstiger.auth.model.SysUser;
import com.github.catstiger.core.db.SQLReady;
import com.github.catstiger.core.db.SQLRequest;
import com.github.catstiger.core.db.id.IdGen;
import com.github.catstiger.core.db.mapper.BeanRowMapper;

import lombok.extern.slf4j.Slf4j;
import reactor.core.support.Assert;

@Repository @Slf4j
public class SysUserDao {
  @Resource
  private JdbcTemplate jdbcTemplate;
  @Resource
  private IdGen idGen;
  /**
   * 根据Mobile，查询单个SysUser对象，如果没有，返回 <code>null</code>
   * @param mobile 给定手机号码
   * @return Instance of SysUser
   */
  public SysUser byMobile(String mobile) {
    SQLReady sqlReady = new SQLRequest(SysUser.class).select().append(" WHERE mobile=? limit 1", mobile);
    log.debug("SysUserDao.byMobile: {}", sqlReady.getSql());
    List<SysUser> users = jdbcTemplate.query(sqlReady.getSql(), sqlReady.getArgs(), new BeanRowMapper<SysUser>(SysUser.class));
    
    return (users == null || users.isEmpty()) ? null : users.get(0);
  }
  
  /**
   * 使用这个Mobile的用户的数量
   * @param mobile 手机号
   * @param id ID，排除此ID的用户
   */
  public Long mobileCount(String mobile, Long id) {
    Assert.notNull(mobile, "手机号不可为空。");
    if(id == null) {
      id = -1L;
    }
    
    return jdbcTemplate.queryForObject("select count(*) from sys_user where mobile=? and id<>?", Long.class, mobile, id);
  }
  
  /**
   * 采用此用户名的人数量，用于判断用户名是否重复
   * @param username 给出用户名
   * @param id ID，排除此ID，用于修改的时候
   */
  public Long usernameCount(String username, Long id) {
    Assert.notNull(username, "用户名不可为空。");
    if(id == null) {
      id = -1L;
    }
    
    return jdbcTemplate.queryForObject("select count(*) from sys_user where username=? and id<>?", Long.class, username, id);
  }
  
  @Transactional
  public SysUser insert(SysUser entity) {
    entity.setId(idGen.nextId());
    SQLReady sqlReady = new SQLRequest(entity).insertNonNull();
    
    jdbcTemplate.update(sqlReady.getSql(), sqlReady.getArgs());
    return entity;
  }
}
