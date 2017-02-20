package com.github.catstiger.core.db.mysql.impl;

import java.util.ArrayList;
import java.util.List;

import com.github.catstiger.core.db.mysql.WordSplitter;
import com.google.common.base.CharMatcher;
import com.google.common.base.Joiner;

public class SingleWordSplitter implements WordSplitter {

  @Override
  public String[] split(String... strs) {
    if(strs == null || strs.length == 0) {
      return new String[]{};
    }
    
    String str = Joiner.on("").join(strs);
    str = CharMatcher.whitespace().collapseFrom(str, ' ');
    List<String> list = new ArrayList<String>(str.length());
    for(int i = 0; i < str.length(); i++) {
      String sub = String.valueOf(str.charAt(i));
      list.add(sub);
    }
    return list.toArray(new String[]{});
  }

}
