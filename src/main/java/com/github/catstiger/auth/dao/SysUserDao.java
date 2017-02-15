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
}
