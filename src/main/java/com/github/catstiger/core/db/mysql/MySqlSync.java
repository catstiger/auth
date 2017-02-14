package com.github.catstiger.core.db.mysql;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Iterator;

import javax.annotation.Resource;
import javax.persistence.JoinColumn;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.github.catstiger.core.db.ColumnCreator;
import com.github.catstiger.core.db.DatabaseInfo;
import com.github.catstiger.core.db.DbSync;
import com.github.catstiger.core.db.IndexCreator;
import com.github.catstiger.core.db.ManyToManyCreator;
import com.github.catstiger.core.db.ModelClassLoader;
import com.github.catstiger.core.db.ORMHelper;
import com.github.catstiger.core.db.TableCreator;
import com.github.catstiger.utils.ReflectUtils;

@Service
public class MySqlSync implements DbSync, InitializingBean {
  private static Logger logger = LoggerFactory.getLogger(MySqlSync.class);
  
  @Resource
  private ModelClassLoader modelClassLoader;
  @Resource
  private DatabaseInfo databaseInfo;
  @Resource
  private TableCreator tableCreator;
  @Resource
  private ColumnCreator columnCreator;
  @Resource
  private IndexCreator indexCreator;
  @Resource
  private ManyToManyCreator m2mCreator;
  
  private String[] packagesToScan;
  
  @Override
  public void afterPropertiesSet() throws Exception {
    modelClassLoader.scanPackages(packagesToScan);
  }
  
  @Value("com.github.catstiger.**.model")
  public void setPackagesToScan(String[] packagesToScan) {
    this.packagesToScan = packagesToScan;
  }
  
  @Override
  public void sync() {
    Iterator<Class<?>> entityItr = modelClassLoader.getEntityClasses();
    //创建所有表和字段
    while(entityItr.hasNext()) {
      Class<?> entityClass = entityItr.next();
      
      if(ORMHelper.isEntity(entityClass)) {
        logger.debug("同步字段 {}", entityClass);
        syncEntity(entityClass);
      }
    }
    
    //创建所有表的外键
    entityItr = modelClassLoader.getEntityClasses();
    while(entityItr.hasNext()) {
      Class<?> entityClass = entityItr.next();
      
      if(ORMHelper.isEntity(entityClass)) {
        logger.debug("同步外键 {}", entityClass);
        syncForeignKey(entityClass);
      }
    }
    
    //创建索引
    entityItr = modelClassLoader.getEntityClasses();
    while(entityItr.hasNext()) {
      Class<?> entityClass = entityItr.next();
      
      if(ORMHelper.isEntity(entityClass)) {
        logger.debug("同步索引 {}", entityClass);
        syncIndexes(entityClass);
      }
    }
    //多对多
    entityItr = modelClassLoader.getEntityClasses();
    while(entityItr.hasNext()) {
      Class<?> entityClass = entityItr.next();
      
      if(ORMHelper.isEntity(entityClass)) {
        logger.debug("many2many {}", entityClass);
        syncM2m(entityClass);
      }
    }
  }
  
  private void syncM2m(Class<?> entityClass) {
    Field[] fields = ReflectUtils.getFields(entityClass); //.getFields();
    for(Field field : fields) {
      m2mCreator.createCrossTable(entityClass, field.getName());
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
  
  /**
   * 创建外键，多对一
   */
  private void syncForeignKey(Class<?> entityClass) {
    Field[] fields = ReflectUtils.getFields(entityClass); //.getFields();
    for(Field field : fields) {
      if(ORMHelper.isFieldIgnore(field)) {
        continue;
      }
      JoinColumn joinColumn = field.getAnnotation(JoinColumn.class);
      if(joinColumn == null) {
        Method getter = ORMHelper.getAccessMethod(entityClass, field.getName());
        joinColumn = getter.getAnnotation(JoinColumn.class);
      }
      if(joinColumn == null) {
        continue;
      }
      
      columnCreator.addForeignKeyIfNotExists(entityClass, field.getName(), field.getType(), "id");
    }
  }
  
  private void syncIndexes(Class<?> entityClass) {
    Field[] fields = ReflectUtils.getFields(entityClass);
    for(Field field : fields) {
      if(ORMHelper.isFieldIgnore(field)) {
        continue;
      }
      JoinColumn joinColumn = field.getAnnotation(JoinColumn.class);
      if(joinColumn == null) {
        Method getter = ORMHelper.getAccessMethod(entityClass, field.getName());
        joinColumn = getter.getAnnotation(JoinColumn.class);
      }
      if(joinColumn != null) { //外键不重复建立索引
        continue;
      }
      indexCreator.addIndexIfNotExists(entityClass, field.getName());
    }
  }
}
