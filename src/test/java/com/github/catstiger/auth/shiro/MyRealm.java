package com.github.catstiger.auth.shiro;

import org.apache.shiro.authc.AuthenticationException;
import org.apache.shiro.authc.AuthenticationInfo;
import org.apache.shiro.authc.AuthenticationToken;
import org.apache.shiro.authc.IncorrectCredentialsException;
import org.apache.shiro.authc.SimpleAuthenticationInfo;
import org.apache.shiro.authc.UnknownAccountException;
import org.apache.shiro.authc.UsernamePasswordToken;
import org.apache.shiro.realm.Realm;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class MyRealm implements Realm {

  @Override
  public String getName() {
    return "MyRealm";
  }

  @Override
  public boolean supports(AuthenticationToken token) {
    return token instanceof UsernamePasswordToken;
  }

  @Override
  public AuthenticationInfo getAuthenticationInfo(AuthenticationToken token) throws AuthenticationException {
    String username = (String) token.getPrincipal();
    String password = new String((char[]) token.getCredentials());
    if(!"lee".equals(username)) {  
      throw new UnknownAccountException(); //如果用户名错误  
    }  
    if(!"123".equals(password)) {  
        throw new IncorrectCredentialsException(); //如果密码错误  
    }  
    log.debug("lee login.");
    return new SimpleAuthenticationInfo(token.getPrincipal(), token.getCredentials(), getName());
  }

}
