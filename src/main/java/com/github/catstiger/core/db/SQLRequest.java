package com.github.catstiger.core.db;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.github.catstiger.core.db.limit.LimitSql;
import com.github.catstiger.core.db.limit.MySqlLimitSql;
import com.github.catstiger.core.db.ns.CamelCaseNamingStrategy;
import com.github.catstiger.core.entity.BaseEntity;
import com.github.catstiger.utils.CollectionUtils;
import com.github.catstiger.utils.StringUtils;
import com.google.common.base.Joiner;

/**
 * 用于存放生成SQL所需的参数
 * @author catstiger
 *
 */
public final class SQLRequest {
  /**
   * 缺省的命名策略--字段名，表名使用下划线命名法，别名使用驼峰命名法
   */
  public static final NamingStrategy DEFAULT_NAME_STRATEGY = new CamelCaseNamingStrategy();
  public static final LimitSql DEFAULT_LIMIT_SQL = new MySqlLimitSql();
  
  Class<?> entityClass;
  BaseEntity entity;
  List<String> includes = new ArrayList<String>(10);
  List<String> excludes = new ArrayList<String>(10);
  boolean usingAlias = false;
  boolean namedParams = false;
  NamingStrategy namingStrategy = DEFAULT_NAME_STRATEGY;
  boolean includesNull = false;
  boolean byId = false;
  LimitSql limitSql = DEFAULT_LIMIT_SQL;
  Map<String, String> sorts = new LinkedHashMap<>(5);
  
  public SQLRequest(Class<?> entityClass) {
    this.entity = null;
    this.entityClass = entityClass;
    //includes = new ArrayList<String>(10);
    //excludes = new ArrayList<String>(10);
    usingAlias = false;
    namingStrategy = DEFAULT_NAME_STRATEGY;
    includesNull = false;
    byId = false;
    namedParams = false;
  }
  
  public SQLRequest(BaseEntity entity) {
    if(entity == null) {
      throw new RuntimeException("实体不可为null.");
    }
    this.entity = entity;
    this.entityClass = entity.getClass();
    //includes = new ArrayList<String>(10);
    //excludes = new ArrayList<String>(10);
    usingAlias = false;
    namingStrategy = DEFAULT_NAME_STRATEGY;
    includesNull = false;
    byId = false;
    namedParams = false;
  }
  
  /**
   * 设置SQL对应的实体类的Class
   * @param entityClass 实体类
   * @return 支持链式操作
   */
  public SQLRequest entityClass(Class<?> entityClass) {
    this.entityClass = entityClass;
    return this;
  }
  
  /**
   * 设置SQL对应的实体对象，通常用于生成SQL
   * @param entity 实体对象
   * @return 支持链式操作
   */
  public SQLRequest entity(BaseEntity entity) {
    if(entity == null) {
      throw new RuntimeException("实体不可为null.");
    }
    this.entity = entity;
    this.entityClass = entity.getClass();
    
    return this;
  }
  /**
   * 设置必须包含的属性名
   * @param includes 属性名
   * @return  支持链式操作
   */
  public SQLRequest includes(String... includes) {
    if(includes != null && includes.length > 0) {
      this.includes.addAll(Arrays.asList(includes));
    }
    return this;
  }
  
  /**
   * 设置必须排除的属性名
   * @param excludes 属性名
   * @return  支持链式操作
   */
  public SQLRequest excludes(String... excludes) {
    if(excludes != null && excludes.length > 0) {
      this.excludes.addAll(Arrays.asList(excludes));
    } 
    return this;
  }
  
  /**
   * 是否将属性名作为字段的别名，或者，在insert和update的时候，用属性名代替?
   * @param usingAlias 如果为<code>true</code>，则将属性名作为字段名的别名，例如 user_id AS userId， 缺省为<code>false</code>
   * @return  支持链式操作
   */
  public SQLRequest usingAlias(boolean usingAlias) {
    this.usingAlias = usingAlias;
    return this;
  }
  
  /**
   * 设置命名规则
   * @param namingStrategy 命名规则的实例
   * @return  支持链式操作
   */
  public SQLRequest namingStrategy(NamingStrategy namingStrategy) {
    this.namingStrategy = namingStrategy;
    return this;
  }
  
  /**
   * 在Update和Insert的时候，是否包含为<code>null</code>的字段，缺省值为<code>false</code>
   */
  public SQLRequest includesNull(boolean includesNull) {
    this.includesNull = includesNull;
    return this;
  }
  
  /**
   * 在Update和SELECT的情形下，是否生成WHERE id=?，缺省为false
   */
  public SQLRequest byId(boolean byId) {
    this.byId = byId;
    return this;
  }
  
  /**
   * 是否命名参数，如果为<code>false</code>,生成的SQL使用?作为占位符， 否则使用属性名作为占位符。
   */
  public SQLRequest namedParams(boolean namedParams) {
    this.namedParams = namedParams;
    return this;
  }
  /**
   * 设定本SQLRequest使用的LimitSql对象
   */
  public SQLRequest withLimitSql(LimitSql limitSql) {
    if(limitSql != null) {
      this.limitSql = limitSql;
    }
    return this;
  }
  
  /**
   * 新增一组排序，Key为字段名，Value为desc/asc
   */
  public SQLRequest setSorts(Map<String, String> sorts) {
    if(sorts != null && !sorts.isEmpty()) {
      this.sorts.putAll(sorts);
    }
    return this;
  }
  
  /**
   * 新增一个排序条件
   * @param columnName 字段名、属性名
   * @param direction 方向，asc\desc
   */
  public SQLRequest addSort(String columnName, String direction) {
    this.sorts.put(columnName, direction);
    return this;
  }
  
  /**
   * 生成INSERT SQL及其对应的参数，不包括值为<code>null</code>的字段
   * @return Instance of SQLReady.
   */
  public SQLReady insert() {
    SQLFactory sqlFactory = SQLFactory.getInstance();
    return sqlFactory.insert(this);
  }
  
  /**
   * 生成INSERT SQL及其对应的参数
   * @param includesNull 指出是否包括为<code>null</code>的字段
   * @return Instance of SQLReady.
   */
  public SQLReady insertNonNull() {
    SQLFactory sqlFactory = SQLFactory.getInstance();
    this.includesNull(false);
    return sqlFactory.insert(this);
  }
  
  /**
   * 生成UPDATE SQL
   */
  public SQLReady update() {
    SQLFactory sqlFactory = SQLFactory.getInstance();
    return sqlFactory.update(this);
  }
  
  /**
   * 根据SQLRequest构造一个SQL UPDATE语句及其对应的参数数组。
   * 只处理不为空的字段，并且，在SET子句中忽略主键字段根据SQLRequest中的NamingStrategy，构造各个字段的名字
   * 如果参数byId为true，并且，SQLRequest#entity的主键不为空，则自动追加WHERE id=?子句，并且在参数中加入ID值
   */
  public SQLReady updateById() {
    SQLFactory sqlFactory = SQLFactory.getInstance();
    return sqlFactory.update(this.byId(true));
  }
  
  /**
   * 生成SQL语，如果{@link #byId}为false，则不生成WHERE及WHERE以后的部分，否则生成的语句包括WHERE id=?
   * @return SQLReady, 只有SQL，没有参数
   */
  public SQLReady select() {
    return SQLFactory.getInstance().select(this, false);
  }
  
  public SQLReady select(boolean supportsJoin) {
    return SQLFactory.getInstance().select(this, supportsJoin);
  }
  
  /**
   * 生成根据ID查询的SQL语句
   */
  public SQLReady selectById() {
    return SQLFactory.getInstance().select(this.byId(true), false);
  }
  
  /**
   * 根据给定的实体，生成查询SQL，生成的规则参考{@link #conditions(boolean)}
   * @param supportsJoin 是否支持外键关联
   */
  public SQLReady selectBySample(boolean supportsJoin) {
    SQLFactory sqlFactory = SQLFactory.getInstance();
    this.byId = false;
    String select = sqlFactory.select(this, supportsJoin).getSql();
    SQLReady sqlReady = SQLFactory.getInstance().conditions(this, supportsJoin);
    
    StringBuilder sql = new StringBuilder(300).append(select)
        .append(" WHERE ");
    if(StringUtils.isNotBlank(sqlReady.getSql())) {
      sql.append(sqlReady.getSql());
    } else {
      sql.append(" 1=1 ");
    }
  
    return new SQLReady(sql.toString(), sqlReady.getArgs()).withLimitSql(limitSql);
  }
  /**
   * 根据给定的实体，生成查询SQL，生成的规则参考{@link #conditions()}，不支持外键关联
   */
  public SQLReady selectBySample() {
    return selectBySample(false);
  }
  
  /**
   * 构建查询条件及其参数，不支持外键关联
   */
  public SQLReady conditions() {
    return SQLFactory.getInstance().conditions(this, false);
  }
  /**
   * 根据Entity中的实体类的实例，创建一个SQL查询的条件: 
   * <ul>
   *    <li>字符串查询都使用 LIKE查询，仅支持右LIKE，即varchar%的形式</li>
   *    <li>如果属性或者getter方法被@FullMatches标注，则使用MYSQL Locate函数代替 LOCATE(?, c.name) > 0</li>
   *    <li>如果属性或者getter方法被@FullText标注，则使用全文检索，对应的字段名为源字段由@FullText标注决定，如果没有设定，则采用本字段</li>
   *    <li>对于数字类型和日期类型，如果被@RangeQuery标注，则采用范围查询，对应的字段名由@RangeQuery设定，如果没有设定，则不采用范围查询</li>
   *    <li>所有查询条件之间的关系，都是AND</li> 
   * </ul>
   * @param supportsJoin 是否支持外键关联，如果支持，并且带有@JoinColumn的字段不为空，则创建关联查询。
   * @see SQLFactory#conditions(SQLRequest)
   * @return
   */
  public SQLReady conditions(boolean supportsJoin) {
    return SQLFactory.getInstance().conditions(this, supportsJoin);
  }

  @Override
  public String toString() {
    StringBuilder buf = new StringBuilder(200);
    if(entity != null) {
      entityClass = entity.getClass();
    }
    buf.append(entityClass.getName()).append(usingAlias);
    
    if(!CollectionUtils.isEmpty(includes)) {
      buf.append(Joiner.on("_").join(includes));
    }
    if(!CollectionUtils.isEmpty(excludes)) {
      buf.append(Joiner.on("_").join(excludes));
    }
    
    buf.append(includesNull).append(usingAlias).append(byId);
    buf.append(namingStrategy.getClass().getSimpleName());
    
    return buf.toString();
  }

}