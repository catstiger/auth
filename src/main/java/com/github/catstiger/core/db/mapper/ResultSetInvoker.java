package com.github.catstiger.core.db.mapper;

import java.sql.ResultSet;
import java.sql.SQLException;

public interface ResultSetInvoker<T> {
  T get(ResultSet rs, int index) throws SQLException;
}
