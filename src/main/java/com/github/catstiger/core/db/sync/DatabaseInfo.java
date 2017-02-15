package com.github.catstiger.core.db.sync;

public interface DatabaseInfo {
  String getCatalog();
  String getSchema();
  Boolean isTableExists(String table);
  Boolean isColumnExists(String table, String column);
  Boolean isForeignKeyExists(String table, String column, String refTable, String refColumn);
  Boolean isIndexExists(String tableName, String indexName, boolean unique);
}
