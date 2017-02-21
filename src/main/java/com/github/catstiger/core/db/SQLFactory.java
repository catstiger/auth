package com.github.catstiger.core.db;

import java.beans.PropertyDescriptor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.persistence.Id;
import javax.persistence.Transient;

import org.springframework.util.Assert;
import org.springframework.util.DigestUtils;

import com.github.catstiger.core.db.id.IdGen;
import com.github.catstiger.core.db.id.SnowflakeIDWorker;
import com.github.catstiger.core.db.mapper.ns.CamelCaseNamingStrategy;
import com.github.catstiger.core.entity.BaseEntity;
import com.github.catstiger.utils.ClassUtils;
import com.github.catstiger.utils.CollectionUtils;
import com.github.catstiger.utils.ReflectUtils;
import com.google.common.base.Joiner;
import com.google.common.base.Predicate;
import com.google.common.collect.Collections2;


public final class SQLFactory {
  private static final String SQL_SELECT = "SELECT_";
  private static final String SQL_INSERT = "INSERT_";
  private static final String SQL_UPDATE = "UPDATE_";
  /**
   * 缺省的命名策略--字段名，表名使用下划线命名法，别名使用驼峰命名法
   */
  public static final NamingStrategy DEFAULT_NAME_STRATEGY = new CamelCaseNamingStrategy();
  
  //private static final String SQL_DELETE = "DELETE_";
  
  private static Map<String, String> sqlCache = new ConcurrentHashMap<String, String>();
  private static Map<String, Collection<ColField>> columnCache = new ConcurrentHashMap<String, Collection<ColField>>();
  
  /**
   * 缺省的IdGen实现，不支持集群模式，集群下请使用SnowFlakeIdGen.
   */
  public static final IdGen DEF_IDGEN = new IdGen() {
    public SnowflakeIDWorker worker = new SnowflakeIDWorker(0, 0);
    @Override
    public Long nextId() {
      return worker.nextId();
    }
    
  };
  
  private SQLFactory() {
  }
  
  private static SQLFactory instance;
  
  public static SQLFactory getInstance() {
    if(instance == null) {
      instance = new SQLFactory();
    }
    
    return instance;
  }
  
  /**
   * 根据实体类的属性，构造一个SELECT SQL语句：
   * <ul>
   *     <li>表名根据类名取得，如果被@Table标注，则取@Table规定的类名。</li>
   *     <li>字段名根据类的Field确定，如果Field的Reader方法被@Column,@JoinColumn标注，则取标注的字段名。</li>
   *     <li>如果Field的Reader方法被@Transient标注，则忽略此字段。</li>
   *     <li>如果Field是一个指向其他实体类的属性，则取@JoinColumn作为类名，如果没有@JoinColumn作为标注，则取引用的表名+id作为字段名，例如：User.getDept(),对应的字段为dept_id。</li>
   *     <li>字段别名就是实体类中的字段名，表的别名，就是实体类的类名各个单词的字头小写+下划线</li>
   *     <li>表名，字段名，全部小写，关键字大写。</li>
   * </ul>
   * @param sqlRequest SQL请求对象
   * @return SQL
   */
  public String select(SQLRequest sqlRequest) {
    String key = sqlKey(sqlRequest, SQL_SELECT);
    //从缓存中取得SQL
    String sqlObj = sqlCache.get(key);
    if(sqlObj != null) {
      return sqlObj; 
    }
    Collection<ColField> colFields = columns(sqlRequest, true);
    
    final String alias = sqlRequest.namingStrategy.tableAlias(sqlRequest.entityClass);
    String tablename = sqlRequest.namingStrategy.tablename(sqlRequest.entityClass) + " " + alias;
    
    final StringBuilder sqlBuf = new StringBuilder(1000).append("SELECT ");
    for(Iterator<ColField> itr = colFields.iterator(); itr.hasNext();) {
      ColField colField = itr.next();
      sqlBuf.append(alias).append(".").append(colField.col);
      
      if(itr.hasNext()) {
        sqlBuf.append(",\n");
      }
    }
    sqlBuf.append(" FROM ").append(tablename);
    sqlCache.put(key, sqlBuf.toString()); //装入缓存
    
    return sqlBuf.toString();
  }
  
  /**
   * 根据给定的实体类，构造一个SQL INSERT语句
   * @param sqlRequest 给定SQLRequest
   * @return SQL
   */
  public String insert(SQLRequest sqlRequest) {
    String key = sqlKey(sqlRequest, SQL_INSERT);
    //从缓存中取得SQL
    String sqlObj = sqlCache.get(key);
    if(sqlObj != null) {
      return sqlObj; 
    }
    String tablename = sqlRequest.namingStrategy.tablename(sqlRequest.entityClass);
    Collection<ColField> colFields = columns(sqlRequest, false);
    //字段列表
    StringBuilder prevValues = new StringBuilder(100);
    //占位符
    StringBuilder afterValues = new StringBuilder(100);
    
    for(Iterator<ColField> itr = colFields.iterator(); itr.hasNext();) {
      ColField colField = itr.next();
      prevValues.append(colField.col);
      if(itr.hasNext()) {
        prevValues.append(",\n");
      } 
      
      afterValues.append(sqlRequest.usingAlias ? (":" + colField.field) : "?");
      if(itr.hasNext()) {
        afterValues.append(sqlRequest.usingAlias ? ",\n" : "," );
      } 
    }
   
    //SQL
    StringBuilder sqlBuf = new StringBuilder(200).append("INSERT INTO ").append(tablename).append(" (\n")
        .append(prevValues.toString()).append(") VALUES (\n").append(afterValues.toString()).append(")");
    
    sqlCache.put(key, sqlBuf.toString()); //装入缓存
    return sqlBuf.toString();
  }
  /**
   * 生成Insert SQL，忽略为<code>null</code>的字段
   * @param entity 实体类
   * @return
   */
  public SQLReady insertDynamic(SQLRequest sqlRequest) {
    if(sqlRequest.entity == null) {
      throw new NullPointerException("给出的实体类不可为空。");
    }
    
    BaseEntity entity = sqlRequest.entity;
    if(entity.getId() == null) {
      entity.setId(DEF_IDGEN.nextId());
    }
    Class<?> entityClass = sqlRequest.entityClass;
    String tablename = sqlRequest.namingStrategy.tablename(entityClass);
    Collection<ColField> colFields = columns(sqlRequest, false);
    
    List<Object> args = new ArrayList<Object>(colFields.size());
    List<ColField> nonNullFields = new ArrayList<ColField>(colFields.size());
    try {
      for(Iterator<ColField> itr = colFields.iterator(); itr.hasNext();) {
        ColField colField = itr.next();
        Field field = ReflectUtils.findField(entityClass, colField.getField());
        field.setAccessible(true);
        Object arg = field.get(entity);
        if(arg != null) {
          args.add(arg);
          nonNullFields.add(colField);
        }
      }
    } catch (Exception e) {
      e.printStackTrace();
      throw new RuntimeException(e);
    }
    //字段列表
    StringBuilder prevValues = new StringBuilder(100);
    //占位符
    StringBuilder afterValues = new StringBuilder(100);
    
    for(Iterator<ColField> itr = nonNullFields.iterator(); itr.hasNext();) {
      ColField colField = itr.next();
      prevValues.append(colField.col);
      if(itr.hasNext()) {
        prevValues.append(",\n");
      } 
      
      afterValues.append(sqlRequest.usingAlias ? (":" + colField.field) : "?");
      if(itr.hasNext()) {
        afterValues.append(sqlRequest.usingAlias ? ",\n" : "," );
      } 
    }
    StringBuilder sqlBuf = new StringBuilder(200).append("INSERT INTO ").append(tablename).append(" (\n")
        .append(prevValues.toString()).append(") VALUES (\n").append(afterValues.toString()).append(")");
    
    return new SQLReady(sqlBuf.toString(), args.toArray(new Object[]{}));
  }
 
  /**
   * 根据给定的实体类，生成update语句
   * @param sqlRequest SQLRequest对象
   * @return
   */
  public String update(SQLRequest sqlRequest) {
    String key = sqlKey(sqlRequest, SQL_UPDATE);
    //从缓存中取得SQL
    String sqlObj = sqlCache.get(key);
    if(sqlObj != null) {
      return sqlObj; 
    }
    String tablename = sqlRequest.namingStrategy.tablename(sqlRequest.entityClass);
    
    Collection<ColField> colFields = columns(sqlRequest, false);
    final StringBuilder sqlBuf = new StringBuilder(100).append("UPDATE ").append(tablename).append(" SET ");
    //找到所有非主键
    Collection<ColField> commonCols = Collections2.filter(colFields, new Predicate<ColField>(){
      @Override
      public boolean apply(ColField cf) {
        return !cf.isPrimary;
      }
    });
    
    for(Iterator<ColField> itr = commonCols.iterator(); itr.hasNext();) {
      ColField colField = itr.next();
      sqlBuf.append(colField.col).append(" = ").append(sqlRequest.usingAlias ? (":" + colField.field) : "?");
      if(itr.hasNext()) {
        sqlBuf.append(",\n");
      }
    }
    
    sqlCache.put(key, sqlBuf.toString()); //装入缓存
    return sqlBuf.toString();
  }
  
  /**
   * 根据给定的SQLRequest，获取对应的列名-字段名列表
   * @param sqlRequest 给定实SQLRequest
   * @return List of {@link ColField}
   */
  public Collection<ColField> columns(SQLRequest sqlRequest, boolean forSelect) {
    String key = colKey(sqlRequest);
    Collection<ColField> colFields = columnCache.get(key);
    if(colFields != null) {
      return colFields;
    }
    
    colFields = getColFields(sqlRequest, forSelect);
    columnCache.put(key, colFields);
    return colFields;
  }
  
  /**
   * 删除SQL语句中的SELECT部分，例如，SELECT ID FROM MY_TABLE，会变成 FROM MY_TABLE
   * @param sql 原始SQL
   * @return 修改之后的SQL
   */
  public String removeSelect(String sql) {
    Assert.hasText(sql);
    int beginPos = sql.toLowerCase().indexOf("from");
    Assert.isTrue(beginPos != -1, " hql : " + sql
        + " must has a keyword 'from'");
    return sql.substring(beginPos);
  }
  
  /**
   * 去除SQL的order by 子句
   */
  public String removeOrders(String sql) {
    Assert.hasText(sql);
    Pattern p = Pattern.compile("order\\s*by[\\w|\\W|\\s|\\S]*",
        Pattern.CASE_INSENSITIVE);
    Matcher m = p.matcher(sql);
    StringBuffer sb = new StringBuffer();
    while (m.find()) {
      m.appendReplacement(sb, "");
    }
    m.appendTail(sb);
    return sb.toString();
  }
  
  /**
   * 删除SQL语句中limit子句
   * @param sql
   * @return
   */
  public String removeLimit(String sql) {
    Assert.hasText(sql);
    int index = sql.toLowerCase().lastIndexOf("limit");
    if(index > 0) {
      return sql.substring(0, index);
    }
    return sql;
  }
  
  /**
   * 将一个普通的SQL，转换为COUNT查询的SQL，去掉Select中的字段列表，和ORDER子句。
   * @param querySql 普通的SQL
   */
  public String countSql(String querySql) {
    return new StringBuilder(150).append("SELECT COUNT(*) FROM (").append(removeOrders(removeLimit(querySql))).append(") table_ ").toString();
  }
  
  /**
   * 返回基于MySQL limit语法的Limt SQL
   * @param sql 原始的SQL
   * @param start Start index of the rows, first is 0.
   * @param limit Max results
   * @return SQL with limit
   */
  public String limitSql(String sql, int start, int limit) {
    return new MySqlLimitSql().getLimitSql(sql, start, limit);
  }
  
  /**
   * 使用下划线命名法，取得实体类对应的表名
   */
  public String getTablename(Class<?> entityClass) {
    return DEFAULT_NAME_STRATEGY.tablename(entityClass);
  }
  
  /**
   * 获取SQL语句的缓存键值
   * @param sqlRequest SQLRequest对象
   * @param type SQL类型
   * @return 缓存Key
   */
  private String sqlKey(SQLRequest sqlRequest, String type) {
    StringBuilder keyBuilder = new StringBuilder(200).append(type).append(sqlRequest.toString());
    return DigestUtils.md5DigestAsHex(keyBuilder.toString().getBytes(Charset.forName("UTF-8")));
  }
  
  
  private String colKey(SQLRequest sqlRequest) {
    return DigestUtils.md5DigestAsHex(sqlRequest.toString().getBytes(Charset.forName("UTF-8")));
  }
  
 
  
  private List<ColField> getColFields(SQLRequest sqlRequest, boolean forSelect) {
    PropertyDescriptor[] propertyDescriptors = ReflectUtils.getPropertyDescriptors(sqlRequest.entityClass);
    if(propertyDescriptors == null) {
      throw new RuntimeException("无法获取PropertyDescriptor " + sqlRequest.entityClass.getName());
    }
    
    List<ColField> colFields = new ArrayList<ColField>(propertyDescriptors.length);
    
    for(PropertyDescriptor propertyDescriptor : propertyDescriptors) {
      if(propertyDescriptor == null) {
        continue;
      }
      //Read方法
      Method readMethod = propertyDescriptor.getReadMethod();
      if(readMethod == null) {
        continue;
      }
      //如果标注为Transient,则忽略
      if(readMethod.getAnnotation(Transient.class) != null || readMethod.getAnnotation(java.beans.Transient.class) != null) {
        continue;
      }
      //如果是集合类或者数组，则忽略
      if(ClassUtils.isAssignable(propertyDescriptor.getPropertyType(), Collection.class) || propertyDescriptor.getPropertyType().isArray()) {
        continue;
      }
      String fieldname = propertyDescriptor.getName();
      //必须包含
      if(!CollectionUtils.isEmpty(sqlRequest.includes) && !sqlRequest.includes.contains(fieldname)) {
        continue;
      }
      //必须排除
      if(!CollectionUtils.isEmpty(sqlRequest.excludes) && sqlRequest.excludes.contains(fieldname)) {
        continue;
      }
      //列名，根据字段名转换得到，与表生成的规则相同
      String columnName = sqlRequest.namingStrategy.columnName(sqlRequest.entityClass, fieldname); //EntityBean属性名小写作为字段名
      
      //属性名作为别名
      if(sqlRequest.usingAlias && forSelect) {
        columnName += (" AS " + fieldname);
      }
      
      ColField colField = new ColField(columnName, fieldname, readMethod.getAnnotation(Id.class) != null);
      colFields.add(colField);
    }
    
    return colFields;
  }
  
  /**
   * 用于存放生成SQL所需的参数
   * @author catstiger
   *
   */
  public static final class SQLRequest {
    
    private Class<?> entityClass;
    private BaseEntity entity;
    private List<String> includes;
    private List<String> excludes;
    private boolean usingAlias = false;
    private NamingStrategy namingStrategy;
    
    public SQLRequest(Class<?> entityClass) {
      this.entity = null;
      this.entityClass = entityClass;
      includes = Collections.emptyList();
      excludes = Collections.emptyList();
      usingAlias = false;
      namingStrategy = DEFAULT_NAME_STRATEGY;
    }
    
    public SQLRequest(BaseEntity entity) {
      if(entity == null) {
        throw new RuntimeException("实体不可为null.");
      }
      this.entity = entity;
      this.entityClass = entity.getClass();
      includes = Collections.emptyList();
      excludes = Collections.emptyList();
      usingAlias = false;
      namingStrategy = DEFAULT_NAME_STRATEGY;
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
      this.entity = entity;
      return this;
    }
    /**
     * 设置必须包含的属性名
     * @param includes 属性名
     * @return  支持链式操作
     */
    public SQLRequest includes(String... includes) {
      if(includes != null && includes.length > 0) {
        this.includes = Arrays.asList(includes);
      } else {
        this.includes = Collections.emptyList();
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
        this.excludes = Arrays.asList(excludes);
      } else {
        this.excludes = Collections.emptyList();
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
      
      buf.append(namingStrategy.getClass().getSimpleName());
      
      return buf.toString();
    }

  }
  /**
   * 用于存放生成的SQL，以及对应的参数
   */
  public static final class SQLReady {
    private String sql;
    private Object[] args = new Object[]{};
    
    public SQLReady() {
      
    }
    
    public SQLReady(String sql, Object[] args) {
      this.sql = sql;
      this.args = args;
    }
    
    public String getSql() {
      return sql;
    }
    
    public void setSql(String sql) {
      this.sql = sql;
    }
    
    public Object[] getArgs() {
      return args;
    }
    
    public void setArgs(Object[] args) {
      this.args = args;
    }
  }
  
  /**
   * 用于装载数据库字段col, 和实体类属性field的对应关系
   * @author leesam
   *
   */
  public static final class ColField implements java.io.Serializable {
    private String col;
    private String field;
    private boolean isPrimary = false;
    
    public ColField() {
      
    }
    
    public ColField(String col, String field, boolean isPrimary) {
      this.col = col;
      this.field = field;
      this.isPrimary = isPrimary;
    }

    public String getCol() {
      return col;
    }

    public String getField() {
      return field;
    }

    public boolean isPrimary() {
      return isPrimary;
    }

    public void setCol(String col) {
      this.col = col;
    }

    public void setField(String field) {
      this.field = field;
    }

    public void setPrimary(boolean isPrimary) {
      this.isPrimary = isPrimary;
    }
  }

  
}
