package com.github.catstiger.core.db;

import java.io.Serializable;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import com.github.catstiger.core.db.id.IdGen;
import com.github.catstiger.core.db.limit.LimitSql;
import com.github.catstiger.core.db.mapper.BeanRowMapper;
import com.github.catstiger.core.entity.BaseEntity;

import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

@Repository @Slf4j
public class JdbcDao {
  @Autowired
  protected JdbcTemplate jdbcTemplate;
  
  protected IdGen idGen = SQLFactory.DEF_IDGEN;
  
  protected NamingStrategy namingStrategy = SQLRequest.DEFAULT_NAME_STRATEGY;
  
  protected LimitSql limitSql = SQLRequest.DEFAULT_LIMIT_SQL;
  
  /**
   * 根据ID查询，将查询结果转换为响应的对象
   * @param entityClass 实体类的Class
   * @param id Identifier of the entity
   * @return Instance of entity.
   */
  public <T> T get(Class<T> entityClass, @NonNull Serializable id) {
    SQLReady sqlReady = new SQLRequest(entityClass)
        .namingStrategy(namingStrategy)
        .withLimitSql(limitSql)
        .byId(true).selectById();
    
    log.debug("\n{}", sqlReady.getSql());
    
    return jdbcTemplate.queryForObject(sqlReady.getSql(), new Object[]{id}, new BeanRowMapper<T>(entityClass));
  }
  
  public <T> T get(Class<T> entityClass, @NonNull BaseEntity entity) {
    //TODO
    return null;
  }
  
  /**
   * Insert一条数据，包括值为<code>null</code>的数据，如果主键值为<code>null<code>，自动生成一个主键
   * 使用{@link #idGen}提供的算法。生成的id会回填到给出的Entity中。
   * @param entity 给出实体对象
   * @return the number of rows affected
   */
  public int insert(@NonNull BaseEntity entity) {
    if(entity.getId() == null) {
      entity.setId(idGen.nextId());
    }
    
    SQLReady sqlReady =  new SQLRequest(entity)
        .namingStrategy(namingStrategy)
        .includesNull(true)
        .namedParams(false).insert();
    
    log.debug("\n{}", sqlReady.getSql());
    
    return jdbcTemplate.update(sqlReady.getSql(), sqlReady.getArgs());  
  }
  /**
   * Insert一条数据，<strong>不</strong>包括值为<code>null</code>的数据，如果主键值为<code>null<code>，自动生成一个主键
   * 使用{@link #idGen}提供的算法。生成的id会回填到给出的Entity中。
   * @param entity 给出实体对象
   * @return the number of rows affected
   */
  public int insertNonNull(@NonNull BaseEntity entity) {
    if(entity.getId() == null) {
      entity.setId(idGen.nextId());
    }
    
    SQLReady sqlReady =  new SQLRequest(entity)
        .namingStrategy(namingStrategy)
        .includesNull(true)
        .namedParams(false).insertNonNull();
    
    log.debug("\n{}", sqlReady.getSql());
    
    return jdbcTemplate.update(sqlReady.getSql(), sqlReady.getArgs());  
  }
  
  
  public void update(@NonNull BaseEntity entity) {
    
  }
  
  public void setNamingStrategy(NamingStrategy namingStrategy) {
    this.namingStrategy = namingStrategy;
  }


  public void setIdGen(IdGen idGen) {
    this.idGen = idGen;
  }


  public void setLimitSql(LimitSql limitSql) {
    this.limitSql = limitSql;
  }
}
