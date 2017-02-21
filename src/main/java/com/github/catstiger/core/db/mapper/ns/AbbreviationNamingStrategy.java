package com.github.catstiger.core.db.mapper.ns;

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
    String name = StringUtils.toStudlyCase(propDesc.getName());
    Iterable<String> iterable = Splitter.on("-").split(name);
    StringBuilder abbr = new StringBuilder(20);
    for(Iterator<String> itr = iterable.iterator(); itr.hasNext();) {
      String split = itr.next();
      if(StringUtils.isBlank(split)) {
        continue;
      }
      abbr.append(split.charAt(0));
    }
    return abbr.toString();
  }

  @Override
  public String columnLabel(ResultSet rs, int columnIndex) {
    try {
      ResultSetMetaData metaData = rs.getMetaData();
      String label = metaData.getColumnLabel(columnIndex);
      
      if(StringUtils.isBlank(label)) {
        return null;
      }
      
      String name = StringUtils.toStudlyCase(label);
      Iterable<String> iterable = Splitter.on("-").split(name);
      StringBuilder abbr = new StringBuilder(20);
      for(Iterator<String> itr = iterable.iterator(); itr.hasNext();) {
        String split = itr.next();
        if(StringUtils.isBlank(split)) {
          continue;
        }
        abbr.append(split.charAt(0));
      }
      return abbr.toString();
    } catch (SQLException e) {
      e.printStackTrace();
      throw new RuntimeException(e);
    }
  }

}
