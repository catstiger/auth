package com.github.catstiger.core.db.mysql;

public abstract class WordEncoder {
  /**
   * 将字符串转换为unicode，但是，没有unicode标记\\u
   * @param str 给定字符串
   * @param isOnlyCN 是否只对中文编码
   * @return
   */
  public static String encode(String str, boolean isOnlyCN) {
    StringBuilder result = new StringBuilder(1000);
    for (int i = 0; i < str.length(); i++) {
      char c = (char) str.charAt(i);
      if (isOnlyCN) {
        if (isChinese(c)) {// 汉字范围 \u4e00-\u9fa5 (中文) 即chr1>=19968&&chr1<=17194
          result.append(Integer.toHexString(c));
        } else {
          result.append(str.charAt(i));
        }
      } else {
        result.append(Integer.toHexString(c));
      }
    }
    return result.toString();

  }

  static boolean isChinese(char c) {
    Character.UnicodeBlock ub = Character.UnicodeBlock.of(c);
    if (ub == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS || ub == Character.UnicodeBlock.CJK_COMPATIBILITY_IDEOGRAPHS
        || ub == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS_EXTENSION_A || ub == Character.UnicodeBlock.GENERAL_PUNCTUATION
        || ub == Character.UnicodeBlock.CJK_SYMBOLS_AND_PUNCTUATION || ub == Character.UnicodeBlock.HALFWIDTH_AND_FULLWIDTH_FORMS) {
      return true;
    }
    return false;
  }
}
