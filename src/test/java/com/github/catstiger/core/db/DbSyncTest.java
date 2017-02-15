package com.github.catstiger.core.db;

import javax.annotation.Resource;

import org.junit.Test;

import com.github.catstiger.core.BaseSpringTest;

public class DbSyncTest extends BaseSpringTest {
  @Resource
  private DbSync dbSync;
  
  @Test
  public void testDbSync() {
    dbSync.sync();
  }
}
