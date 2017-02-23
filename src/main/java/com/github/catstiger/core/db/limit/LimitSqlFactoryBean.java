package com.github.catstiger.core.db.limit;

import javax.annotation.Resource;

import org.springframework.beans.factory.FactoryBean;
import org.springframework.stereotype.Component;
/**
 * 创建LimitSql的工厂类
 */
@Component
public class LimitSqlFactoryBean implements FactoryBean<LimitSql> {
  @Resource
  private DatabaseDetector dbDetector;
  
  /**
   * 根据Databasename，创建LimitSql的实现。
   * 
 * @throws ApplicationException  如果没有从DataSource中找到databasename，或者
 * 没有相应的实现类
   */
  @Override
  public LimitSql getObject() throws Exception {
    if(dbDetector.isH2()) {
      return new H2LimitSql();
    } else if(dbDetector.isMySql()) {
      return new MySqlLimitSql();
    } else if(dbDetector.isOracle()) {
      return new OracleLimitSql();
    } else {
      throw new RuntimeException("No LimitSql implementaion for " + dbDetector.getDatabaseName());
    }
  }

  @Override
  public Class<?> getObjectType() {
    return LimitSql.class;
  }

  @Override
  public boolean isSingleton() {
    return true;
  }

}
