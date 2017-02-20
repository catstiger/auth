package com.github.catstiger.core.db;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;


/**
 * 分页对象.包含数据及分页信息. 
 * 
 * @author Sam
 */
public final class Page implements Serializable {
	/**
	 * serialVersionUID
	 */
	private static final long serialVersionUID = 231152607479172128L;
  
  public static final int DEFAULT_PAGE_SIZE = 15;
  
  public static final int DEFAULT_STEPS = 10;

  private int start;
  
  private int limit;
  
  private long total;
  
  private Collection<?> rows;
  
  public Page() {
    this.start = 0;
    this.limit = DEFAULT_PAGE_SIZE;
  }
  
  public Page(int start, int limit) {
    this.start = start;
    this.limit = limit;
  }



  /**
   * 用于实现Google风格的分页
   */
  public Integer[] getSteps() {
    int startPage = calcStartPage();
    int stepSize = calcStepPageSize();
    List<Integer> steps = new ArrayList<Integer>();
    for(int i = startPage; i < startPage + stepSize; i ++) {
      steps.add(i);
    }
    return steps.toArray(new Integer[]{});
  }
  
  /**
   * calculate fist page No of fast step.
   */
  private int calcStartPage() {
    if(getPageNo() < (DEFAULT_STEPS / 2) ||
      (getPageNo() - (DEFAULT_STEPS / 2)) < 1) {
      return 1;
    }
    else {
      return getPageNo() - (DEFAULT_STEPS / 2);
    }
  }
  
  private int calcStepPageSize() {
    if((calcStartPage() + DEFAULT_STEPS) > getPages()) {
      return getPages() - calcStartPage() + 1;
    } else {
     return DEFAULT_STEPS;
    }
  }
  
  public int getPageNo() {
    return (start / limit) + 1;
  }
  
  /**
   * 取总页数
   */
  public int getPages() {
    if (((int) total) % limit == 0) {
      return ((int) total) / limit;
    } else {
      return ((int) total) / limit + 1;
    }
  }

  public int getStart() {
    return start;
  }

  public void setStart(int start) {
    this.start = start;
  }

  public int getLimit() {
    return limit;
  }

  public void setLimit(int limit) {
    this.limit = limit;
  }

  public long getTotal() {
    return total;
  }

  public void setTotal(long total) {
    this.total = total;
  }

  public Collection<?> getRows() {
    return rows;
  }

  public void setRows(Collection<?> rows) {
    this.rows = rows;
  }

}
