package com.github.catstiger.core.db.mysql;

import java.util.HashMap;
import java.util.Map;

import com.github.catstiger.core.db.mysql.impl.DicAnalysisSplitter;
import com.github.catstiger.core.db.mysql.impl.NoneSplitter;
import com.github.catstiger.core.db.mysql.impl.SingleWordSplitter;
import com.github.catstiger.core.db.mysql.impl.SpaceCommaSplitter;
import com.github.catstiger.core.db.mysql.impl.StdAnalysisSplitter;

/**
 * 分词类型常量类
 * @author leesam
 *
 */
public final class SplitTypes {
  /**
   * 不进行任何分词操作
   */
  public static final String SPLIT_NONE = "none";
  
  /**
   * 按照空格或者逗号分词，将给定的字符串，按照空格和逗号分隔
   */
  public static final String SPLIT_SPACE_COMMA = "split_space_comma_split";
  
  /**
   * 用户字典优先分词
   */
  public static final String SPLIT_DIC = "split_dic_analysis";
  
  /**
   * 标准分词
   */
  public static final String SPLIT_STD = "split_index_analysis";
  
  /**
   * 每一个字一个分词单元，例如：您好2017，分词结果为 您-好-2-0-1-7，通常适用于"全文检索替代LIKE查询"的场景
   */
  public static final String SPLIT_SINGLE_WORD = "split_single_word";
  
  
  static Map<String, WordSplitter> spliters = new HashMap<String, WordSplitter>(5);
  
  static {
    spliters.put(SPLIT_NONE, new NoneSplitter());
    spliters.put(SPLIT_SINGLE_WORD, new SingleWordSplitter());
    spliters.put(SPLIT_SPACE_COMMA, new SpaceCommaSplitter());
    spliters.put(SPLIT_DIC, new DicAnalysisSplitter());
    spliters.put(SPLIT_STD, new StdAnalysisSplitter());
  }
  
  
  private SplitTypes() {
  }
}
