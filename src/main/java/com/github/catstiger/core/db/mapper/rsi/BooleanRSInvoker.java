package com.github.catstiger.core.db.mapper.rsi;

import java.sql.ResultSet;
import java.sql.SQLException;

import com.github.catstiger.core.db.mapper.ResultSetInvoker;

public class BooleanRSInvoker implements ResultSetInvoker<Boolean> {

  @Override
  public Boolean get(ResultSet rs, int index) throws SQLException {
    return rs.getBoolean(index);
  }
}
