package com.github.catstiger.auth.service;

import java.util.Date;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.github.catstiger.auth.dao.SysUserDao;
import com.github.catstiger.auth.model.SysUser;
import com.github.catstiger.mvc.RequestObjectHolder;
import com.github.catstiger.mvc.annotation.API;
import com.github.catstiger.mvc.annotation.Domain;
import com.github.catstiger.mvc.exception.Exceptions;
import com.github.catstiger.utils.StringUtils;

import lombok.NonNull;

@Service @Domain("/sys_user")
public class SysUserService {
  
  @Resource
  private SysUserDao sysUserDao;
  @Resource
  private SmsService smsService;
  
  /**
   * 注册
   * @param sysUser
   * @return
   */
  @Transactional
  @API
  public SysUser register(@NonNull SysUser sysUser) {
    if(StringUtils.isBlank(sysUser.getUsername())) {
      throw Exceptions.readable("用户名为必填项。");
    }
    
    if(StringUtils.isBlank(sysUser.getMobile())) {
      throw Exceptions.readable("手机号为必填项。");
    }
    
    if(StringUtils.isBlank(sysUser.getPassword())) {
      throw Exceptions.readable("密码为必填项。");
    }
    
    if(!this.isMobileNotExists(sysUser)) {
      throw Exceptions.readable("手机号不可重复！");
    }
    
    if(!this.isUsernameNotExists(sysUser)) {
      throw Exceptions.readable("用户名不可重复！");
    }
    HttpServletRequest request = RequestObjectHolder.getRequest();
    if(!smsService.isCorrect(sysUser.getMobile(), request.getParameter("captcha"))) {
      throw Exceptions.readable("验证码错误！");
    }
    sysUser.setRegistTime(new Date());
    sysUser.setIsAvailable(true);
    sysUser.setId(null); //判断重复的时候，会被赋值为-1，因此要恢复
    
    return sysUserDao.insert(sysUser);
  }
  
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
