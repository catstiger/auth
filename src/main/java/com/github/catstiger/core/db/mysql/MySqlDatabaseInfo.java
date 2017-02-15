package com.github.catstiger.core.db.mysql;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.annotation.Resource;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.ColumnMapRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import com.github.catstiger.core.db.DatabaseInfo;
import com.github.catstiger.utils.StringUtils;
import com.google.common.base.Joiner;

import lombok.NonNull;

@Component
public class MySqlDatabaseInfo implements DatabaseInfo {
  @Resource
  private JdbcTemplate jdbcTemplate;
  @Value("${jdbc.url}")
  private String url;
  
  private String catalog = null;
  private Set<String> tables = new HashSet<String>(100);
  private Set<String> columns = new HashSet<String>(500);
  private Set<String> fks = new HashSet<String>(100);
  private Set<String> indexes = new HashSet<String>(100);

  @Override
  public String getCatalog() {
    if(catalog != null) {
      return catalog;
    }
    Connection conn = null;
    try {
      conn = jdbcTemplate.getDataSource().getConnection();
      DatabaseMetaData dbMetaData = conn.getMetaData();
      ResultSet rs = dbMetaData.getCatalogs();
      catalog = "def";
      
      while (rs.next()) {
        catalog = rs.getString("TABLE_CAT"); //CATALOG
        if(url.indexOf(catalog) > 0) {
          break;
        }
      }
      return catalog;
      
    } catch (SQLException e) {
      e.printStackTrace();
    } finally {
      try {
        conn.close();
      } catch (SQLException e) {
        e.printStackTrace();
      }
    }
    return null;
  }

  @Override
  public String getSchema() {
    return null;
  }
  
  @Override
  public Boolean isTableExists(String table) {
    if(StringUtils.isBlank(table)) {
      return false;
    }
    if(tables.contains(table.toLowerCase())) {
      return true;
    }
    Connection conn = null;
    try {
      conn = jdbcTemplate.getDataSource().getConnection();
      DatabaseMetaData dbMetaData = conn.getMetaData();
      
      ResultSet rs = dbMetaData.getTables(getCatalog(), getSchema(), table, null);
      
      if(rs.next()) {
        tables.add(table.toLowerCase());
        return true;
      }
    } catch (SQLException e) {
      e.printStackTrace();
    } finally {
      try {
        conn.close();
      } catch (SQLException e) {
        e.printStackTrace();
      }
    }
    
    return false;
  }

  @Override
  public Boolean isColumnExists(String table, String column) {
    if(StringUtils.isBlank(table) || StringUtils.isBlank(column)) {
      return false;
    }
    String tableColumn = (table + "/" + column).toLowerCase();
    if(columns.contains(tableColumn)) {
      return true;
    }
    
    Connection conn = null;
    try {
      conn = jdbcTemplate.getDataSource().getConnection();
      DatabaseMetaData dbMetaData = conn.getMetaData();
            
      ResultSet rs = dbMetaData.getColumns(getCatalog(), getSchema(), table, column);
      if(rs.next()) {
        columns.add(tableColumn);
        return true;
      }
      
    } catch (SQLException e) {
      e.printStackTrace();
    } finally {
      try {
        conn.close();
      } catch (SQLException e) {
        e.printStackTrace();
      }
    }
    return false;
  }

  @Override
  public Boolean isForeignKeyExists(String table, String column, String refTable, String refColumn) {
    Connection conn = null;
    String[] args = {"FK", table, column, refTable, refColumn};
    String key = Joiner.on("_").join(args);
    if(fks.contains(key.toLowerCase())) {
      return true;
    }
    try {
      conn = jdbcTemplate.getDataSource().getConnection();
      DatabaseMetaData dbMetaData = conn.getMetaData();
      ResultSet rs = dbMetaData.getExportedKeys(getCatalog(), getSchema(), refTable);
      int index = 0;
      while(rs.next()) {
        ColumnMapRowMapper cmrm = new ColumnMapRowMapper();
        Map<String, Object> map = cmrm.mapRow(rs, index);
        if(getCatalog().equals(map.get("FKTABLE_CAT")) && table.equals(map.get("FKTABLE_NAME")) && column.equals(map.get("FKCOLUMN_NAME"))) {
          fks.add(key.toLowerCase());
          return true;
        }
        index++;
      }
    }  catch (SQLException e) {
      e.printStackTrace();
    } finally {
      try {
        conn.close();
      } catch (SQLException e) {
        e.printStackTrace();
      }
    }
    return false;
  }

  @Override
  public Boolean isIndexExists(String tableName, @NonNull String indexName, boolean unique) {
    Connection conn = null;
    String key = (getCatalog() + "_" + indexName).toLowerCase();
    if(indexes.contains(key)) {
      return true;
    }
    
    try {
      conn = jdbcTemplate.getDataSource().getConnection();
      DatabaseMetaData dbMetaData = conn.getMetaData();
      ResultSet rs = dbMetaData.getIndexInfo(getCatalog(), getSchema(), tableName, unique, true);
      int index = 0;
      while(rs.next()) {
        ColumnMapRowMapper cmrm = new ColumnMapRowMapper();
        Map<String, Object> map = cmrm.mapRow(rs, index);
        if(indexName.equalsIgnoreCase((String) map.get("INDEX_NAME"))) {
          indexes.add(key);
          return true;
        }
        index++;
      }
    }  catch (SQLException e) {
      e.printStackTrace();
    } finally {
      try {
        conn.close();
      } catch (SQLException e) {
        e.printStackTrace();
      }
    }
    return false;
  }

}
