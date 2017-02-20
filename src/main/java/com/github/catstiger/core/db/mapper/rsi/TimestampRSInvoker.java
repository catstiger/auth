package com.github.catstiger.core.db.mapper.rsi;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;

import com.github.catstiger.core.db.mapper.ResultSetInvoker;

public class TimestampRSInvoker implements ResultSetInvoker<Timestamp> {

  @Override
  public Timestamp get(ResultSet rs, int index) throws SQLException {
    return rs.getTimestamp(index);
  }
}
