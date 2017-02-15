package com.github.catstiger.core.db.sync;

import javax.persistence.ManyToMany;

public interface ManyToManyCreator {
  /**
   * 创建ManyToMany交叉表, 字段必须为一个Collection的子类，使用@ManyToMany标注，其属性
   * {@link ManyToMany#targetEntity()} 指向另一个实体类， {@link ManyToMany#mappedBy()}指向另一实体类的对应
   * 的属性名。
   * “另一个”实体类中对应的字段，<b>不可以</b>指明{@link ManyToMany#mappedBy()}属性！
   * @param entityClass 主导Entity
   * @param fieldName 字段名
   */
  public void createCrossTable(Class<?> entityClass, String fieldName);
}
