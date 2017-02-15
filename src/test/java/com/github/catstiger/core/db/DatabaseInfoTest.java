package com.github.catstiger.core.db;

import javax.annotation.Resource;

import org.junit.Test;

import com.github.catstiger.core.BaseSpringTest;

public class DatabaseInfoTest extends BaseSpringTest {
  @Resource
  private DatabaseInfo dbInfo;
  
  @Test
  public void testFk() {
    dbInfo.isForeignKeyExists("", "", "users", "");
  }
  
  @Test
  public void testIndex() {
    dbInfo.isIndexExists("users", "", false);
  }
}
