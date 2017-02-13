package com.github.catstiger.core.db.mysql;

import java.util.Iterator;

import javax.annotation.Resource;

import com.github.catstiger.core.db.DatabaseInfo;
import com.github.catstiger.core.db.DbSync;
import com.github.catstiger.core.db.ModelClassLoader;
import com.github.catstiger.core.db.ORMHelper;
import com.github.catstiger.core.db.TableCreator;

public class MySqlSync implements DbSync {
  @Resource
  private ModelClassLoader modelClassLoader;
  @Resource
  private DatabaseInfo databaseInfo;
  @Resource
  private TableCreator tableCreator;
  
  @Override
  public void sync() {
    Iterator<Class<?>> entityItr = modelClassLoader.getEntityClasses();
    //创建所有表和字段
    while(entityItr.hasNext()) {
      Class<?> entityClass = entityItr.next();
      if(ORMHelper.isEntity(entityClass)) {
        syncEntity(entityClass);
      }
    }
    
    //创建所有表的外键
    while(entityItr.hasNext()) {
      Class<?> entityClass = entityItr.next();
      if(ORMHelper.isEntity(entityClass)) {
        syncEntity(entityClass); //TODO:syncFK
      }
    }
    
  }
  
  private void syncEntity(Class<?> entityClass) {
    String table = ORMHelper.tableNameByEntity(entityClass);
    if(!databaseInfo.isTableExists(table)) {
      tableCreator.createTableIfNotExists(entityClass);
    } else {
      tableCreator.updateTable(entityClass);
    }
  }

}
