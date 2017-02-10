package com.github.catstiger.core.db.model;

import lombok.Data;

@Data
public class ColumnModel {
  private TableModel belongTo;
  private String columnName;
  private String dataType;
  private Boolean isNullable = true;
  private Integer maxCharacterLength;
  private Integer numericPrecision;
  private Integer numericScale;
  private Boolean isUnique = false;
  
}
