package com.github.catstiger.core.db.model;

import java.util.ArrayList;
import java.util.List;

import lombok.Data;

@Data
public class TableModel {
  private String tableName;
  private String schema;
  private String tableType;
  
  private List<ColumnModel> cols = new ArrayList<>(16);
  
  public void addCols(ColumnModel columnModel) {
    if(columnModel != null) {
      cols.add(columnModel);
    }
  }
  
}
