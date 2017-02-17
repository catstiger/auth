package com.github.catstiger.auth.service;

import javax.annotation.Resource;

import org.springframework.stereotype.Service;

import com.github.catstiger.auth.dao.SysUserDao;
import com.github.catstiger.auth.model.SysUser;
import com.github.catstiger.mvc.annotation.API;
import com.github.catstiger.mvc.annotation.Domain;

@Service @Domain("/sys_user")
public class SysUserService {
  @Resource
  private SysUserDao sysUserDao;
  
  /**
   * FIXME 未完成。。。
   * @param sysUser
   * @return
   */
  @API
  public Boolean isMobileExists(SysUser sysUser) {
    return false;
  }
}
