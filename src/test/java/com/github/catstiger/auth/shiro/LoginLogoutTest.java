package com.github.catstiger.auth.shiro;

import java.util.Arrays;

import org.apache.shiro.SecurityUtils;
import org.apache.shiro.authc.AuthenticationException;
import org.apache.shiro.authc.UsernamePasswordToken;
import org.apache.shiro.config.IniSecurityManagerFactory;
import org.apache.shiro.subject.Subject;
import org.apache.shiro.util.Assert;
import org.apache.shiro.util.Factory;
import org.junit.Test;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class LoginLogoutTest {
  @Test
  public void testLogin() {
    Factory<org.apache.shiro.mgt.SecurityManager> factory = new IniSecurityManagerFactory("classpath:shiro.properties");
    org.apache.shiro.mgt.SecurityManager securityManager = factory.getInstance();
    SecurityUtils.setSecurityManager(securityManager);
    Subject subject = SecurityUtils.getSubject();
    UsernamePasswordToken token = new UsernamePasswordToken("lee", "123");
    try {
      subject.login(token);
    } catch (AuthenticationException e) {
      log.warn(e.getMessage());
    }
    Assert.isTrue(subject.hasRole("role1"));
    Assert.isTrue(subject.hasAllRoles(Arrays.asList("role1", "role2")));
    Assert.isTrue(subject.isPermitted("user:create"));
    Assert.isTrue(subject.isPermitted("user:create", "user:update")[0]);
    Assert.isTrue(subject.isPermitted("user:create", "user:update")[1]);
    Assert.isTrue(!subject.isPermitted("user:create", "user:update", "user.delete")[2]);
    subject.checkPermission("user:create");
    subject.checkPermissions("system:user:*");
    subject.checkPermissions("system:user:create,update,delete,view");
    
    Assert.isTrue(subject.isAuthenticated()); //断言用户已经登录  
    subject.logout();  
  }
}
