package com.github.catstiger.core.db.mysql.impl;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import com.github.catstiger.core.db.mysql.WordSplitter;
import com.google.common.base.CharMatcher;
import com.google.common.base.Splitter;


public class SpaceCommaSplitter implements WordSplitter {

  @Override
  public String[] split(String... strs) {
    if(strs == null || strs.length == 0) {
      return new String[]{};
    }
    
    List<String> list = new ArrayList<String>(strs.length + 10);
    for(String str : strs) {
      Iterable<String> iterable = Splitter.onPattern("\\s+|,+").trimResults().omitEmptyStrings().split(CharMatcher.whitespace().collapseFrom(str, ' ')); 
      for(Iterator<String> itr = iterable.iterator(); itr.hasNext();) {
        list.add(itr.next());
      }
    }
    return list.toArray(new String[]{});
  }
  
}
