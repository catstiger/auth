package com.github.catstiger.auth.dao;

import org.beetl.sql.core.annotatoin.Param;
import org.beetl.sql.core.mapper.BaseMapper;

import com.github.catstiger.auth.model.SysUser;

public interface SysUserDao extends BaseMapper<SysUser> {
  /**
   * 根据Mobile，查询单个SysUser对象，如果没有，返回 <code>null</code>
   * @param mobile 给定手机号码
   * @return Instance of SysUser
   */
  public SysUser byMobile(@Param("mobile") String mobile);
  
  /**
   * 使用这个Mobile的用户的数量
   * @param mobile 手机号
   * @param id ID，排除此ID的用户
   */
  public Long mobileCount(@Param("mobile") String mobile, @Param("id") Long id);
  
  /**
   * 采用此用户名的人数量，用于判断用户名是否重复
   * @param username 给出用户名
   * @param id ID，排除此ID，用于修改的时候
   */
  public Long usernameCount(@Param("username") String username, @Param("id") Long id);
}
