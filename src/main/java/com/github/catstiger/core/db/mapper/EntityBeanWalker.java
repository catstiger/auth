package com.github.catstiger.core.db.mapper;

import java.beans.PropertyDescriptor;
import java.lang.reflect.Method;
import java.util.Collection;

import javax.persistence.Entity;
import javax.persistence.Table;
import javax.persistence.Transient;

import com.github.catstiger.utils.ClassUtils;
import com.github.catstiger.utils.ReflectUtils;

class EntityBeanWalker {
  
  void walk(Object bean, PropertyValueWriter propertyValueWriter) {
    walk(bean, propertyValueWriter, 0);
  }
  
  private void walk(Object bean, PropertyValueWriter propertyValueWriter, int depth) {
    PropertyDescriptor[] propDescs = ReflectUtils.getPropertyDescriptors(bean.getClass());
    for(PropertyDescriptor propDesc : propDescs) {
      if(propDesc == null) {
        continue;
      }
      Method reader = propDesc.getReadMethod();
      if(reader == null || reader.getAnnotation(Transient.class) != null) {
        continue;
      }
      
      Method writer = propDesc.getWriteMethod();
      if(writer == null || writer.getAnnotation(Transient.class) != null) {
        continue;
      }
      //不支持OneToMany
      if(ClassUtils.isAssignable(propDesc.getPropertyType(), Collection.class)) {
        continue;
      }
      //写入属性
      propertyValueWriter.writeProperty(bean, propDesc);
      //发现引用的实体，继续walk
      Class<?> propClass = propDesc.getPropertyType();
      //ManyToOne
      if(propClass.getAnnotation(Table.class) != null || propClass.getAnnotation(Entity.class) != null) {
        if(depth >= 3) { //控制递归深度
          continue;
        }
        Object value = ReflectUtils.invokeMethod(reader, bean);
        if(value == null) {
          value = ReflectUtils.instantiate(propClass);
          ReflectUtils.invokeMethod(writer, bean, value);
        }
        
        walk(value, propertyValueWriter, depth + 1);
      } 
    }
  }
}
