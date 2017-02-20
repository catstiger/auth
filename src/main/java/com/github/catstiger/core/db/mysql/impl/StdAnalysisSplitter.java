package com.github.catstiger.core.db.mysql.impl;

import org.ansj.splitWord.Analysis;
import org.ansj.splitWord.analysis.IndexAnalysis;

import com.github.catstiger.core.db.mysql.WordSplitter;

public class StdAnalysisSplitter extends AnsjIndexWordSplitter {

  public StdAnalysisSplitter(WordSplitter previousWordSplitter) {
    this.previousSplitter = previousWordSplitter;
  }
  
  public StdAnalysisSplitter() {
    this.previousSplitter = null;
  }
  
  @Override
  protected Analysis getAnalysis() {
    return new IndexAnalysis();
  }
  
}
