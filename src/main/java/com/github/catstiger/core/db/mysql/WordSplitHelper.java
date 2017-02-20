package com.github.catstiger.core.db.mysql;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;

import com.github.catstiger.core.db.mysql.impl.DicAnalysisSplitter;
import com.github.catstiger.core.db.mysql.impl.SingleWordSplitter;
import com.github.catstiger.core.db.mysql.impl.SpaceCommaSplitter;

public abstract class WordSplitHelper {
  /**
   * 将所有字符串按照字符分割，例如："Apple iphone"，分割之后为['A', 'p', 'p', 'l', 'e', ' ', 'i'...]
   * @param strings 被分割的字符串们
   * @return 分割后的字符串
   */
  public static String[] singleSplit(String...strings) {
    return new SingleWordSplitter().split(strings);  
  }
  
  /**
   * 按单个字符分割，并对中文字符进行unicode编码。
   */
  public static String[] sigleSplitAndEncodeCN(String...strings) {
    String[] strs = new SingleWordSplitter().split(strings);
    final List<String> encoded = new ArrayList<String>(strs.length);
    Arrays.asList(strs).forEach(new Consumer<String>() {
      @Override
      public void accept(String t) {
        encoded.add(WordEncoder.encode(t, true));
      }
    });
    
    return encoded.toArray(new String[]{});
  }
  
  /**
   * 按照空格或者半角逗号分割
   */
  public static String[] spaceSplit(String ...strings) {
    return new SpaceCommaSplitter().split(strings);
  }
  
  /**
   * 空格或者半角逗号分割，并对于中文字符进行unicode编码
   * @param strings
   * @return
   */
  public static String[] spaceSplitAndEncodeCN(String...strings) {
    String[] strs = new SpaceCommaSplitter().split(strings);
    final List<String> encoded = new ArrayList<String>(strs.length);
    Arrays.asList(strs).forEach(new Consumer<String>() {
      @Override
      public void accept(String t) {
        encoded.add(WordEncoder.encode(t, true));
      }
    });
    
    return encoded.toArray(new String[]{});
  }
  
  /**
   * 首先按照空格分割，然后进行全文检索分词
   */
  public static String[] indexSplit(String...strings) {
    return new DicAnalysisSplitter(new SpaceCommaSplitter()).split(strings);
  }
  
  /**首先按照空格分割，然后进行全文检索分词，并对中文进行unicode编码
   */
  public static String[] indexSplitAndEncodeCN(String...strings) {
    String[] strs = new DicAnalysisSplitter(new SpaceCommaSplitter()).split(strings);
    final List<String> encoded = new ArrayList<String>(strs.length);
    Arrays.asList(strs).forEach(new Consumer<String>() {
      @Override
      public void accept(String t) {
        encoded.add(WordEncoder.encode(t, true));
      }
    });
    
    return encoded.toArray(new String[]{});
  }
  
}
