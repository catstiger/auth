package com.github.catstiger.core.db.mysql;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

import javax.annotation.Resource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import com.github.catstiger.core.db.ColumnCreator;
import com.github.catstiger.core.db.DatabaseInfo;
import com.github.catstiger.core.db.ORMHelper;
import com.github.catstiger.core.db.TableCreator;
import com.google.common.base.Joiner;

@Component
public class MySqlTableCreator implements TableCreator {
  private Logger logger = LoggerFactory.getLogger(MySqlTableCreator.class);
  
  @Resource
  private DatabaseInfo databaseInfo;
  @Resource
  private ColumnCreator columnCreator;
  @Resource
  private JdbcTemplate jdbcTemplate;
  
  
  @Override
  public void createTableIfNotExists(Class<?> entityClass) {
    String table = ORMHelper.tableNameByEntity(entityClass);
    Field[] fields = entityClass.getFields();
    
    if(!this.isTableExists(entityClass)) {
      StringBuilder sqlBuf = new StringBuilder(500)
          .append("create table ")
          .append(table)
          .append("(");
      List<String> sqls = new ArrayList<String>(fields.length); //SQL片段
      for(Field field : fields) {
        sqls.add(columnCreator.getColumnSqlFragment(entityClass, field.getName()));
      }
      sqlBuf.append(Joiner.on(",\n").join(sqls))
      .append(")");
      logger.debug("创建表{}, {}", entityClass.getName(), sqlBuf);
      jdbcTemplate.execute(sqlBuf.toString());
    } 
  }

  @Override
  public Boolean isTableExists(Class<?> entityClass) {
    String table = ORMHelper.tableNameByEntity(entityClass);
    return databaseInfo.isTableExists(table);
  }

  @Override
  public void updateTable(Class<?> entityClass) {
    Field[] fields = entityClass.getFields();
    if(this.isTableExists(entityClass)) {
      for(Field field : fields) {
        columnCreator.addColumnIfNotExists(entityClass, field.getName());
      }
    }

  }

}
