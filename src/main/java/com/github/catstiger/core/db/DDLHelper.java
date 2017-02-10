package com.github.catstiger.core.db;

import java.lang.reflect.Field;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.Table;

import com.github.catstiger.utils.ReflectUtils;
import com.github.catstiger.utils.StringUtils;

import lombok.NonNull;

public abstract class DDLHelper {
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
  
  public static String columnNameByField(@NonNull Class<?> entity, @NonNull String fieldName) {
    @NonNull Field field = ReflectUtils.findField(entity, fieldName);
    Column column = field.getAnnotation(Column.class);
    JoinColumn joinCol = field.getAnnotation(JoinColumn.class);
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
