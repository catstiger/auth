package com.github.catstiger.core.db.sync.mysql;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import javax.annotation.Resource;
import javax.persistence.JoinColumn;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.github.catstiger.core.db.NamingStrategy;
import com.github.catstiger.core.db.ORMHelper;
import com.github.catstiger.core.db.sync.ColumnCreator;
import com.github.catstiger.core.db.sync.DatabaseInfo;
import com.github.catstiger.core.db.sync.DbSync;
import com.github.catstiger.core.db.sync.IndexCreator;
import com.github.catstiger.core.db.sync.ManyToManyCreator;
import com.github.catstiger.core.db.sync.ModelClassLoader;
import com.github.catstiger.core.db.sync.TableCreator;
import com.github.catstiger.mvc.exception.Exceptions;
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
  
  private NamingStrategy namingStrategy;
  
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
    ORMHelper ormHelper = ORMHelper.getInstance(namingStrategy);
    Set<String> tables = new HashSet<String>();
    Iterator<Class<?>> entityItr = modelClassLoader.getEntityClasses();
    //创建所有表和字段
    while(entityItr.hasNext()) {
      Class<?> entityClass = entityItr.next();
      
      if(ormHelper.isEntity(entityClass)) {
        String table = ormHelper.tableNameByEntity(entityClass);
        if(tables.contains(table)) {
          throw Exceptions.readable("表名重复，请检查实体类 " + entityClass.getName() );
        }
        logger.debug("同步字段 {}", entityClass);
        syncEntity(entityClass);
        tables.add(table);
      }
    }
    
    //创建所有表的外键
    entityItr = modelClassLoader.getEntityClasses();
    while(entityItr.hasNext()) {
      Class<?> entityClass = entityItr.next();
      
      if(ormHelper.isEntity(entityClass)) {
        logger.debug("同步外键 {}", entityClass);
        syncForeignKey(entityClass);
      }
    }
    
    //创建索引
    entityItr = modelClassLoader.getEntityClasses();
    while(entityItr.hasNext()) {
      Class<?> entityClass = entityItr.next();
      
      if(ormHelper.isEntity(entityClass)) {
        logger.debug("同步索引 {}", entityClass);
        syncIndexes(entityClass);
      }
    }
    //多对多
    entityItr = modelClassLoader.getEntityClasses();
    while(entityItr.hasNext()) {
      Class<?> entityClass = entityItr.next();
      
      if(ormHelper.isEntity(entityClass)) {
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
    ORMHelper ormHelper = ORMHelper.getInstance(namingStrategy);
    String table = ormHelper.tableNameByEntity(entityClass);
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
    ORMHelper ormHelper = ORMHelper.getInstance(namingStrategy);
    Field[] fields = ReflectUtils.getFields(entityClass); //.getFields();
    for(Field field : fields) {
      if(ormHelper.isFieldIgnore(field)) {
        continue;
      }
      JoinColumn joinColumn = field.getAnnotation(JoinColumn.class);
      if(joinColumn == null) {
        Method getter = ormHelper.getAccessMethod(entityClass, field.getName());
        joinColumn = getter.getAnnotation(JoinColumn.class);
      }
      if(joinColumn == null) {
        continue;
      }
      
      columnCreator.addForeignKeyIfNotExists(entityClass, field.getName(), field.getType(), "id");
    }
  }
  
  private void syncIndexes(Class<?> entityClass) {
    ORMHelper ormHelper = ORMHelper.getInstance(namingStrategy);
    Field[] fields = ReflectUtils.getFields(entityClass);
    for(Field field : fields) {
      if(ormHelper.isFieldIgnore(field)) {
        continue;
      }
      JoinColumn joinColumn = field.getAnnotation(JoinColumn.class);
      if(joinColumn == null) {
        Method getter = ormHelper.getAccessMethod(entityClass, field.getName());
        joinColumn = getter.getAnnotation(JoinColumn.class);
      }
      if(joinColumn != null) { //外键不重复建立索引
        continue;
      }
      indexCreator.addIndexIfNotExists(entityClass, field.getName());
    }
  }

  public void setNamingStrategy(NamingStrategy namingStrategy) {
    this.namingStrategy = namingStrategy;
  }
}
