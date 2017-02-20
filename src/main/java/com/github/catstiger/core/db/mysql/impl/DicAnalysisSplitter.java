package com.github.catstiger.core.db.mysql.impl;

import org.ansj.splitWord.Analysis;
import org.ansj.splitWord.analysis.DicAnalysis;

import com.github.catstiger.core.db.mysql.WordSplitter;

public class DicAnalysisSplitter extends AnsjIndexWordSplitter {

  public DicAnalysisSplitter(WordSplitter previousWordSplitter) {
    this.previousSplitter = previousWordSplitter;
  }
  
  public DicAnalysisSplitter() {
    this.previousSplitter = null;
  }
  
  @Override
  protected Analysis getAnalysis() {
    return new DicAnalysis();
  }

}
