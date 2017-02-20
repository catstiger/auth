package com.github.catstiger.core.db.mysql;

public interface WordSplitter {
  /**
   * 对一组字符串进行分词操作
   * @param str 给定的字符串
   * @return 分词之后的结果
   */
  String[] split(String... strs);
}
