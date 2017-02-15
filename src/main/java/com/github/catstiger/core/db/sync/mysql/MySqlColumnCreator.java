package com.github.catstiger.core.db.sync.mysql;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.sql.Timestamp;
import java.util.Date;

import javax.annotation.Resource;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.Lob;

import org.beetl.sql.core.annotatoin.AutoID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.ReflectionUtils;

import com.github.catstiger.core.db.sync.ColumnCreator;
import com.github.catstiger.core.db.sync.DatabaseInfo;
import com.github.catstiger.core.db.sync.IndexCreator;
import com.github.catstiger.core.db.sync.ORMHelper;
import com.github.catstiger.utils.StringUtils;

import lombok.NonNull;

@Service
public class MySqlColumnCreator implements ColumnCreator {
  private static Logger logger = LoggerFactory.getLogger(MySqlColumnCreator.class);
  @Value("${jdbc.strongReferences}")
  private Boolean strongReferences;
  @Resource
  private JdbcTemplate jdbcTemplate;
  @Resource
  private DatabaseInfo databaseInfo;
  @Resource
  private IndexCreator indexCreator;
  
  @Override
  public void addColumnIfNotExists(Class<?> entityClass, String field) {
    if(ORMHelper.isFieldIgnore(entityClass, field)) {
      return;
    }
    
    if(!isColumnExists(entityClass, field)) {
      StringBuilder sqlBuilder = new StringBuilder(100)
          .append("alter table ")
          .append(ORMHelper.tableNameByEntity(entityClass))
          .append(" add column (")
          .append(getColumnSqlFragment(entityClass, field))
          .append(")");
      logger.debug("新增字段 : {}", sqlBuilder);
      jdbcTemplate.execute(sqlBuilder.toString());
    }
  }

  @Override
  public String getColumnSqlFragment(@NonNull Class<?> entityClass, @NonNull String fieldName) {
    if(!ORMHelper.isEntity(entityClass)) {
      throw new RuntimeException(entityClass.getName() + " 不是实体类！");
    }
    Field field = ReflectionUtils.findField(entityClass, fieldName); //属性
    if(ORMHelper.isFieldIgnore(field)) {
      return "";
    }
    
    String name = ORMHelper.columnNameByField(entityClass, fieldName); //对应的字段名
    @NonNull Method getter = ORMHelper.getAccessMethod(entityClass, fieldName); //对应的getter方法
    Column colAnn = getter.getAnnotation(Column.class); // Column标注
    JoinColumn joinColAnn = getter.getAnnotation(JoinColumn.class); //外键标注
    Lob lobAnn = getter.getAnnotation(Lob.class); //Lob标注
    AutoID autoId = getter.getAnnotation(AutoID.class); //是否自增
    Id id = getter.getAnnotation(Id.class); //是否主键
    Entity refEntityAnn = field.getType().getAnnotation(Entity.class); //外键
    
    int length = 255, precision = 0, scale = 0;
    boolean nullable = true;
    boolean unique = false;
    String columnDef = "";
    
    if(colAnn != null) {
      length = colAnn.length();
      precision = colAnn.precision();
      scale = colAnn.scale();
      nullable = colAnn.nullable();
      unique = colAnn.unique();
      columnDef = colAnn.columnDefinition();
    } else if (joinColAnn != null) {
      nullable = joinColAnn.nullable();
      unique = joinColAnn.unique();
      columnDef = joinColAnn.columnDefinition();
    } 
    
    StringBuilder sql = new StringBuilder(100).append(name).append(" ");
    Class<?> type = field.getType();
    
    //字符串类型
    if(StringUtils.isNotBlank(columnDef)) {
      sql.append(columnDef);
    }
    else if(type == String.class) {
      if(lobAnn != null) {
        sql.append("text");
      } else {
        sql.append("varchar(").append(length).append(")");
      }
    } 
    //浮点类型
    else if (type == Double.class || type == double.class || type == Float.class || type == float.class) {
      if (precision > 0 && scale == 0) {
        sql.append("numeric(").append(precision).append(")");
      } else if (precision > 0 && scale > 0) {
        sql.append("numeric(").append(precision).append(",").append(scale).append(")");
      } else if (type == double.class || type == Double.class) {
        sql.append("double");
      } else if (type == Float.class || type == float.class) {
        sql.append("float");
      }
    }
    //长整型
    else if (type == Long.class || type == long.class) {
      if(precision > 0) {
        sql.append("numeric(").append(precision).append(")");
      } else {
        sql.append("bigint");
      }
    }
    //整型
    else if (type == Integer.class || type == int.class) {
      if(precision > 0) {
        sql.append("numeric(").append(precision).append(")");
      } else {
        sql.append("int");
      }
    }
    //短整型
    else if (type == Short.class || type == short.class) {
      if(precision > 0) {
        sql.append("numeric(").append(precision).append(")");
      } else {
        sql.append("tinyint");
      }
    }
    //boolean
    else if (type == Boolean.class || type == boolean.class) {
      sql.append("tinyint(1)");
    }
    //日期
    else if (type == Date.class || type == Timestamp.class) {
      sql.append("datetime");
    } 
    //外键
    else if (refEntityAnn != null) {
      sql.append("bigint");
    }
    
    if(!nullable) {
      sql.append(" not null ");
    }
    
    if(unique) {
      sql.append(" unique ");
    }
    
    if(autoId != null) {
      sql.append(" auto_increment ");
    }
    
    if(id != null) {
      sql.append(" primary key ");
    }
    
    return sql.toString();
  }

  @Override
  public void addForeignKeyIfNotExists(Class<?> entityClass, String field, Class<?> refClass, String refField) {
    if(ORMHelper.isEntityIgnore(entityClass)) {
      return;
    }
    
    if(ORMHelper.isFieldIgnore(entityClass, field)) {
      return;
    }
    
    if(!this.strongReferences) { //非强关联，不建立外键
      indexCreator.addIndexIfNotExists(entityClass, field); //外键需要创建索引
      return;
    }
    String table = ORMHelper.tableNameByEntity(entityClass);
    String refTable = ORMHelper.tableNameByEntity(refClass);
    String column = ORMHelper.columnNameByField(entityClass, field);
    String refColumn = ORMHelper.columnNameByField(refClass, refField);
    
    if(!databaseInfo.isForeignKeyExists(table, column, refTable, refColumn)) {
      String fkName = new StringBuilder(30).append("fk_").append(table).append("_").append(column).append("_").append(refTable).toString();
      StringBuilder sqlBuilder = new StringBuilder(200)
          .append("ALTER TABLE ")
          .append(table)
          .append(" ADD CONSTRAINT ")
          .append(fkName)
          .append(" FOREIGN KEY (")
          .append(column)
          .append(") REFERENCES ")
          .append(refTable)
          .append("(").append(refColumn).append(")");
      logger.debug("新增外键 {}", sqlBuilder.toString());
      jdbcTemplate.execute(sqlBuilder.toString());
    }
  }

  @Override
  public Boolean isColumnExists(Class<?> entityClass, String field) {
    String table = ORMHelper.tableNameByEntity(entityClass);
    String colname = ORMHelper.columnNameByField(entityClass, field);
    return databaseInfo.isColumnExists(table, colname);
  }
}
