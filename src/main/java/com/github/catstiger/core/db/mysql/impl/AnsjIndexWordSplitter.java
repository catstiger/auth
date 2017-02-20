package com.github.catstiger.core.db.mysql.impl;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.ansj.domain.Result;
import org.ansj.domain.Term;
import org.ansj.splitWord.Analysis;

import com.github.catstiger.core.db.mysql.IndexWordSplitter;
import com.github.catstiger.core.db.mysql.WordSplitter;
import com.github.catstiger.utils.StringUtils;

public abstract class AnsjIndexWordSplitter implements IndexWordSplitter {
  protected WordSplitter previousSplitter;
  
  @Override
  public String[] split(String... strs) {
    //首先要进行前置分词处理
    if(previousSplitter != null && !(previousSplitter instanceof IndexWordSplitter)) {
      strs = previousSplitter.split(strs);
    }
    
    List<String> list = new ArrayList<String>(strs.length);
    for(String str : strs) {
      Result result = getAnalysis().parseStr(str);
      for(Iterator<Term> itr = result.iterator(); itr.hasNext();) {
        Term term = itr.next();
        
        String key = StringUtils.trim(term.getName()); 
        //过滤不必要的字符
        if(term == null || StringUtils.isBlank(key) || term.getNatureStr().equals("null")) {
          continue;
        }
        list.add(key);
      }
    }
    
    return list.toArray(new String[]{});
  }
  
  protected abstract Analysis getAnalysis();

}
