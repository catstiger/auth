package com.github.catstiger.core.db;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.Table;

import com.github.catstiger.utils.ReflectUtils;
import com.github.catstiger.utils.StringUtils;

import lombok.NonNull;

public abstract class ORMHelper {
  /**
   * 判断给定的Class是否是一个实体类
   */
  public static Boolean isEntity(Class<?> entity) {
    if(entity == null) {
      return false;
    }
    
    return entity.getAnnotation(Entity.class) != null;
  }
  
  /**
   * 根据实体类，得到对应的表名：
   * <ul>
   *     <li>如果实体类被@javax.persistence.Table标注，并且进行命名，则取@Table命名</li>
   *     <li>如果实体类被@org.beetl.sql.core.annotatoin.Table标注，并且进行命名，则取@Table的命名</li>
   *     <li>否则，取类名小写，单词之间用下划线分割作为表名</li>
   * </ul>
   * @param entity 给出实体类
   * @return 表名
   */
  public static String tableNameByEntity(@NonNull Class<?> entity) {
    if(entity.getAnnotation(Entity.class) == null) {
      return null;
    }
    
    Table table = entity.getAnnotation(Table.class);
    org.beetl.sql.core.annotatoin.Table beetlTable = entity.getAnnotation(org.beetl.sql.core.annotatoin.Table.class);
    String tablename;
    
    if(table != null && StringUtils.isNotBlank(table.name())) {
      tablename = table.name();
    } else if (beetlTable != null && StringUtils.isNotBlank(beetlTable.name())) {
      tablename = beetlTable.name();
    } else {
      tablename = StringUtils.toSnakeCase(entity.getSimpleName());
    }
    
    return tablename;
  }
  
  /**
   * 根据实体类的属性，获取字段名：
   * <ul>
   *     <li>如果属性被@javax.persistence.Column或者@javax.persistence.JoinColumn标注，并且进行命名，则取其作为字段名</li>
   *     <li>否则，取字段名小写，单词之间用下划线分割作为字段名</li>
   * </ul>
   * @param entity 实体类
   * @param fieldName 属性名
   * @return 字段名
   */
  public static String columnNameByField(@NonNull Class<?> entity, @NonNull String fieldName) {
    @NonNull Field field = ReflectUtils.findField(entity, fieldName);
    @NonNull Method getter  = ReflectUtils.findMethod(entity, "get" + StringUtils.upperFirst(fieldName));
    Column column = getter.getAnnotation(Column.class);
    JoinColumn joinCol = getter.getAnnotation(JoinColumn.class);
    String columnName;
    if(column != null && StringUtils.isNotBlank(column.name())) {
      columnName = column.name();
    } else if (joinCol != null && StringUtils.isNotBlank(joinCol.name())) {
      columnName = joinCol.name();
    } else {
      columnName = StringUtils.toSnakeCase(field.getName());
    }
    
    return columnName;
  }
}
