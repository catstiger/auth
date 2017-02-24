package com.github.catstiger.core.db.ns;

import java.beans.PropertyDescriptor;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.Iterator;

import com.github.catstiger.utils.StringUtils;
import com.google.common.base.Splitter;

public class AbbreviationNamingStrategy extends AbstractNamingStrategy {

  @Override
  public String columnLabel(PropertyDescriptor propDesc) {
    if(propDesc == null || StringUtils.isBlank(propDesc.getName())) {
      return null;
    }
    return columnLabel(propDesc.getName());
  }

  @Override
  public String columnLabel(ResultSet rs, int columnIndex) {
    try {
      ResultSetMetaData metaData = rs.getMetaData();
      String label = metaData.getColumnLabel(columnIndex);
      
      if(StringUtils.isBlank(label)) {
        return null;
      }
      
      return columnLabel(label);
    } catch (SQLException e) {
      e.printStackTrace();
      throw new RuntimeException(e);
    }
  }

  @Override
  public String columnLabel(String column) {
    if(StringUtils.isBlank(column)) {
      return StringUtils.EMPTY;
    }
    
    String name = StringUtils.toSnakeCase(column);
    Iterable<String> iterable = Splitter.on("_").split(name);
    StringBuilder abbr = new StringBuilder(20);
    for(Iterator<String> itr = iterable.iterator(); itr.hasNext();) {
      String split = itr.next();
      if(StringUtils.isBlank(split)) {
        continue;
      }
      abbr.append(split.charAt(0));
    }
    return abbr.toString().toLowerCase();
  }
  
  @Override
  public String tableAlias(Class<?> entityClass) {
    if(aliasCache.containsKey(entityClass.getName())) {
      return aliasCache.get(entityClass.getName());
    } else {
      Iterable<String> iterable = Splitter.on("_").split(tablename(entityClass));
      StringBuilder alias = new StringBuilder(10);
      for(Iterator<String> itr = iterable.iterator(); itr.hasNext();) {
        alias.append(itr.next().charAt(0));
      }
      
      String a = alias.toString().toLowerCase();
      aliasCache.put(entityClass.getName(), a);
      return a;
    }
    
    
  }

}
