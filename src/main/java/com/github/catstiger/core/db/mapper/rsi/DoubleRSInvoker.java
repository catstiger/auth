package com.github.catstiger.core.db.mapper.rsi;

import java.sql.ResultSet;
import java.sql.SQLException;

import com.github.catstiger.core.db.mapper.ResultSetInvoker;

public class DoubleRSInvoker implements ResultSetInvoker<Double> {

  @Override
  public Double get(ResultSet rs, int index) throws SQLException {
    return rs.getDouble(index);
  }
}
