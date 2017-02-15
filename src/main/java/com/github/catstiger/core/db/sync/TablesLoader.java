package com.github.catstiger.core.db.sync;

import java.util.Collection;
import java.util.Iterator;

import com.github.catstiger.core.db.model.TableModel;

public interface TablesLoader {
  /**
   * 根据Entity Class，加载所对应的TableModel
   * @param entityClasses
   * @return
   */
  public Collection<TableModel> loadTables(Iterator<Class<?>> entityClasses);
  /**
   * 根据EntityClass加载TableModel对象
   * @param entityClass
   */
  public TableModel loadTable(Class<?> entityClass);
}
