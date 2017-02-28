package com.github.catstiger.auth.security.realm;

import org.apache.shiro.authc.AuthenticationException;
import org.apache.shiro.authc.AuthenticationInfo;
import org.apache.shiro.authc.AuthenticationToken;
import org.apache.shiro.realm.Realm;

public class SysUserJdbcRealm implements Realm {

  @Override
  public String getName() {
    
    return null;
  }

  @Override
  public boolean supports(AuthenticationToken token) {
    
    return false;
  }

  @Override
  public AuthenticationInfo getAuthenticationInfo(AuthenticationToken token) throws AuthenticationException {
    
    return null;
  }

}
