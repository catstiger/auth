package com.github.catstiger.core.db.mysql.impl;

import com.github.catstiger.core.db.mysql.WordSplitter;

public class NoneSplitter implements WordSplitter {

  @Override
  public String[] split(String... strs) {
    return strs;
  }

}
