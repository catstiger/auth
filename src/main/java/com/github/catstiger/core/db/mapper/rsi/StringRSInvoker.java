package com.github.catstiger.core.db.mapper.rsi;

import java.io.IOException;
import java.io.Reader;
import java.sql.Clob;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;

import com.github.catstiger.core.db.mapper.ResultSetInvoker;

public class StringRSInvoker implements ResultSetInvoker<String> {

  @Override
  public String get(ResultSet rs, int index) throws SQLException {
    int type = rs.getMetaData().getColumnType(index);
    if (type == Types.CLOB) {
      Clob clob = rs.getClob(index);
      StringBuilder stringBuilder = new StringBuilder(1000);
      if (clob != null) {
        char[] buf = new char[1000];
        Reader reader = null;
        try {
          reader = clob.getCharacterStream();
          int bytesRead = 0;
          while ((bytesRead = reader.read(buf)) != -1) {
            stringBuilder.append(buf, 0, bytesRead);
          }
          
        } catch (Exception e) {
          e.printStackTrace();
        } finally {
          try {
            reader.close();
          } catch (IOException e) {
          }
        }
      }
      return stringBuilder.toString();
    } else {
      return rs.getString(index);
    }
  }
}
