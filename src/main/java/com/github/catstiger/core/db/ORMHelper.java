package com.github.catstiger.core.db;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.Table;

import org.springframework.util.ReflectionUtils;

import com.github.catstiger.utils.StringUtils;

import lombok.NonNull;

public abstract class ORMHelper {
  /**
   * 判断给定的Class是否是一个实体类
   */
  public static Boolean isEntity(Class<?> entityClass) {
    if(entityClass == null) {
      return false;
    }
    
    return entityClass.getAnnotation(Entity.class) != null;
  }
  
  /**
   * 根据实体类，得到对应的表名：
   * <ul>
   *     <li>如果实体类被@javax.persistence.Table标注，并且进行命名，则取@Table命名</li>
   *     <li>如果实体类被@org.beetl.sql.core.annotatoin.Table标注，并且进行命名，则取@Table的命名</li>
   *     <li>否则，取类名小写，单词之间用下划线分割作为表名</li>
   * </ul>
   * @param entityClass 给出实体类
   * @return 表名
   */
  public static String tableNameByEntity(@NonNull Class<?> entityClass) {
    if(entityClass.getAnnotation(Entity.class) == null) {
      return null;
    }
    
    Table table = entityClass.getAnnotation(Table.class);
    org.beetl.sql.core.annotatoin.Table beetlTable = entityClass.getAnnotation(org.beetl.sql.core.annotatoin.Table.class);
    String tablename;
    
    if(table != null && StringUtils.isNotBlank(table.name())) {
      tablename = table.name();
    } else if (beetlTable != null && StringUtils.isNotBlank(beetlTable.name())) {
      tablename = beetlTable.name();
    } else {
      tablename = StringUtils.toSnakeCase(entityClass.getSimpleName());
    }
    
    return tablename;
  }
  
  /**
   * 根据实体类的属性，获取字段名：
   * <ul>
   *     <li>如果属性被@javax.persistence.Column或者@javax.persistence.JoinColumn标注，并且进行命名，则取其作为字段名</li>
   *     <li>否则，取字段名小写，单词之间用下划线分割作为字段名</li>
   * </ul>
   * @param entityClass 实体类
   * @param fieldName 属性名
   * @return 字段名
   */
  public static String columnNameByField(@NonNull Class<?> entityClass, @NonNull String fieldName) {
    @NonNull Field field = ReflectionUtils.findField(entityClass, fieldName);
    @NonNull Method getter  = getAccessMethod(entityClass, fieldName);
    Column column = getter.getAnnotation(Column.class);
    JoinColumn joinCol = getter.getAnnotation(JoinColumn.class);
    Entity refEntityAnn = field.getType().getAnnotation(Entity.class); //外键
    String columnName;
    if(column != null && StringUtils.isNotBlank(column.name())) {
      columnName = column.name();
    } else if (joinCol != null && StringUtils.isNotBlank(joinCol.name())) {
      columnName = joinCol.name();
    } else {
      columnName = StringUtils.toSnakeCase(field.getName());
      if (refEntityAnn != null) {
        columnName = columnName + "_id";
      } 
    }
    
    return columnName;
  }
  /**
   * 根据实体类，和field，获取对应的GETTER方法
   * @param entityClass 实体类
   * @param fieldName 属性名
   * @return Getter Meth
   */
  public static Method getAccessMethod(@NonNull Class<?> entityClass, @NonNull String fieldName) {
    Method getter = ReflectionUtils.findMethod(entityClass, "get" + StringUtils.upperFirst(fieldName));
    return getter;
  }
  
  /**
   * 字段是否被忽略
   * @param field
   * @return
   */
  public static Boolean isFieldIgnore(Field field) {
    javax.persistence.Transient transientAnn = field.getAnnotation(javax.persistence.Transient.class);
    if(transientAnn != null) {
      return true;
    }
    
    java.beans.Transient trAnn = field.getAnnotation(java.beans.Transient.class);
    if(trAnn != null) {
      return true;
    }
    
    Method getter = getAccessMethod(field.getDeclaringClass(), field.getName());
    if(getter == null) {
      return true;
    }
    transientAnn = getter.getAnnotation(javax.persistence.Transient.class);
    if(transientAnn != null) {
      return true;
    }
    
    trAnn = getter.getAnnotation(java.beans.Transient.class);
    if(trAnn != null) {
      return true;
    }
    
    return false;
    
  }
}
