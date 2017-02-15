package com.github.catstiger.core.db;

import javax.annotation.Resource;

import org.junit.Test;
import org.springframework.util.Assert;

import com.github.catstiger.core.BaseSpringTest;
import com.github.catstiger.core.db.model.TestDbModel;

public class ColumnCreatorTest extends BaseSpringTest {
  @Resource
  private ColumnCreator colCreator;
  
  @Test
  public void testIsColumnExists() {
    Boolean b = colCreator.isColumnExists(TestDbModel.class, "id");
    Assert.isTrue(b);
  }
}
