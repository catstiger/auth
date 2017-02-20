package com.github.catstiger.core.db.mysql;

public final class WordSpliterFactory {
  /**
   * 得到一个分词器
   * @param splitType 分词类型
   * @see {@link SplitTypes#SPLIT_NONE}
   * @see {@link SplitTypes#SPLIT_DIC}
   * @see {@link SplitTypes#SPLIT_SINGLE_WORD}
   * @see {@link SplitTypes#SPLIT_SPACE_COMMA}
   * @see {@link SplitTypes#SPLIT_STD}
   * @return
   */
  public static WordSplitter getSpliter(String splitType) {
    if(splitType == null) {
      throw new RuntimeException("必须给出SplitType");
    }
    if(SplitTypes.spliters.containsKey(splitType)) {
      return SplitTypes.spliters.get(splitType);
    }
    return SplitTypes.spliters.get(SplitTypes.SPLIT_DIC);
  }
  
  private WordSpliterFactory() {
    
  }

}
