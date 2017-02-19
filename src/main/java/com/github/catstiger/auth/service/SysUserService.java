package com.github.catstiger.auth.service;

import javax.annotation.Resource;

import org.springframework.stereotype.Service;

import com.github.catstiger.auth.dao.SysUserDao;
import com.github.catstiger.auth.model.SysUser;
import com.github.catstiger.mvc.annotation.API;
import com.github.catstiger.mvc.annotation.Domain;

import lombok.NonNull;

@Service @Domain("/sys_user")
public class SysUserService {
  @Resource
  private SysUserDao sysUserDao;
  
  /**
   * 跟进Mobile和ID，判断用户是否存在
   */
  @API
  public Boolean isMobileNotExists(@NonNull SysUser sysUser) {
    if(sysUser.getId() == null) {
      sysUser.setId(-1L);
    }
    
    long count = sysUserDao.mobileCount(sysUser.getMobile(), sysUser.getId());
    return count == 0L;
  }
  
  /**
   * 判断用户名是否存在
   * @param sysUser
   * @return
   */
  @API
  public Boolean isUsernameNotExists(@NonNull SysUser sysUser) {
    if(sysUser.getId() == null) {
      sysUser.setId(-1L);
    }
    
    long count = sysUserDao.usernameCount(sysUser.getUsername(), sysUser.getId());
    return count == 0L;
  }
}
