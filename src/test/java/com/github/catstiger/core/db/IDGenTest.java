package com.github.catstiger.core.db;

import java.time.Duration;

import org.beetl.sql.ext.SnowflakeIDAutoGen;
import org.junit.Test;

public class IDGenTest {
  @Test
  public void genIds() {
    SnowflakeIDAutoGen gen = new SnowflakeIDAutoGen();
    long id = gen.nextID(null);
    System.out.println(id);
    long start = System.currentTimeMillis();
    for(int i = 0; i < 40000; i++) {
      id = gen.nextID(null);
      ///System.out.println(id);
    }
    long end = System.currentTimeMillis();
    System.out.println(Duration.ofMillis(end - start).toString());
  }
}
