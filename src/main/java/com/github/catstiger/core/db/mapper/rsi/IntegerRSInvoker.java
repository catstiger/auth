package com.github.catstiger.core.db.mapper.rsi;

import java.sql.ResultSet;
import java.sql.SQLException;

import com.github.catstiger.core.db.mapper.ResultSetInvoker;

public class IntegerRSInvoker implements ResultSetInvoker<Integer> {

  @Override
  public Integer get(ResultSet rs, int index) throws SQLException {
    return rs.getInt(index);
  }
}
