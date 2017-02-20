package com.github.catstiger.core.db.mapper;

import java.beans.PropertyDescriptor;

public interface PropertyValueWriter {
  /**
   * 根据给出的Bean对象和PropertyDescriptor，向owner对象写入一个属性
   * @param owner 给出宿主对象
   * @param propertyDescriptor 属性描述对象
   * @return 写入的属性值
   */
  public Object writeProperty(Object owner, PropertyDescriptor propertyDescriptor);
}
