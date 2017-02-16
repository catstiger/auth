package com.github.catstiger.core.db;

import javax.annotation.Resource;

import org.beetl.sql.core.SQLManager;
import org.junit.Test;
import org.springframework.util.Assert;

import com.github.catstiger.auth.dao.SysUserDao;
import com.github.catstiger.auth.model.SysUser;
import com.github.catstiger.auth.test.SpringTestCase;

public class SqlManageTest extends SpringTestCase {
  @Resource
  SQLManager sqlMgr;
  
  @Resource
  SysUserDao userDao;
  
  @Test
  public void testGenFile() {
    try {
      sqlMgr.genSQLFile("verify_code");
    } catch (Exception e) {
      e.printStackTrace();
    }
  }
  
  @Test
  public void testMapper() {
    SysUserDao dao = sqlMgr.getMapper(SysUserDao.class);
    dao.byMobile("13930119546");
  }
  
  @Test
  public void testDao() {
    SysUser sysUser = userDao.byMobile("13930119564");
    Assert.isNull(sysUser);
  }

}
