package com.github.catstiger.core.db.mapper.ns;

import java.beans.PropertyDescriptor;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;

import com.github.catstiger.utils.StringUtils;

public class CamelCaseNamingStrategy  extends AbstractNamingStrategy {

  @Override
  public String columnLabel(PropertyDescriptor propDesc) {
    if(propDesc == null || StringUtils.isBlank(propDesc.getName())) {
      return null;
    }
    return StringUtils.toCamelCase(propDesc.getName());
  }

  @Override
  public String columnLabel(ResultSet rs, int columnIndex) {
    try {
      ResultSetMetaData metaData = rs.getMetaData();
      String label = metaData.getColumnLabel(columnIndex);
      
      if(StringUtils.isBlank(label)) {
        return null;
      }
      if(label.toLowerCase().endsWith("_id")) { //说明是外键
        label = label.substring(0, label.length() - 3);
      }
      return StringUtils.toCamelCase(label);
    } catch (SQLException e) {
      e.printStackTrace();
      throw new RuntimeException(e);
    }
  }
}
