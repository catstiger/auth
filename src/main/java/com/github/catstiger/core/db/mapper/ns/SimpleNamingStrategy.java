package com.github.catstiger.core.db.mapper.ns;

import java.beans.PropertyDescriptor;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;

import com.github.catstiger.utils.StringUtils;

/**
 * 字段名全小写，作为ColumnLabel
 * @author catstiger
 *
 */
public class SimpleNamingStrategy  extends AbstractNamingStrategy {

  @Override
  public String columnLabel(PropertyDescriptor propDesc) {
    if(propDesc == null || StringUtils.isBlank(propDesc.getName())) {
      return null;
    }
    
    return propDesc.getName().toLowerCase();
  }

  @Override
  public String columnLabel(ResultSet rs, int columnIndex) {
    try {
      ResultSetMetaData metaData = rs.getMetaData();
      String label = metaData.getColumnLabel(columnIndex);
      
      if(StringUtils.isBlank(label)) {
        return null;
      }
      
      return StringUtils.toCamelCase(label).toLowerCase();
    } catch (SQLException e) {
      e.printStackTrace();
      throw new RuntimeException(e);
    }
  }

}
