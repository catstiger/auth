package com.github.catstiger.core.db;

import java.beans.PropertyDescriptor;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.persistence.Id;
import javax.persistence.Transient;

import org.springframework.util.Assert;
import org.springframework.util.DigestUtils;

import com.github.catstiger.core.db.annotation.FullMatches;
import com.github.catstiger.core.db.annotation.FullText;
import com.github.catstiger.core.db.annotation.RangeQuery;
import com.github.catstiger.core.db.id.IdGen;
import com.github.catstiger.core.db.id.SnowflakeIDWorker;
import com.github.catstiger.core.db.limit.LimitSql;
import com.github.catstiger.core.entity.BaseEntity;
import com.github.catstiger.utils.ClassUtils;
import com.github.catstiger.utils.CollectionUtils;
import com.github.catstiger.utils.ReflectUtils;
import com.github.catstiger.utils.StringUtils;
import com.google.common.base.Joiner;


public final class SQLFactory {
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
  public SQLReady select(SQLRequest sqlRequest) {
    String key = sqlKey(sqlRequest, "select");
    //从缓存中取得SQL
    String sqlObj = sqlCache.get(key);
    if(sqlObj != null) {
      return new SQLReady(sqlObj, new Object[]{}, sqlRequest.limitSql); 
    }
    Collection<ColField> colFields = columns(sqlRequest);
    
    String alias = null;
    String tablename = sqlRequest.namingStrategy.tablename(sqlRequest.entityClass);
    //表名别名
    if(sqlRequest.usingAlias) {
      alias = sqlRequest.namingStrategy.tableAlias(sqlRequest.entityClass);
      tablename += (" " + alias);
    }
    
    final StringBuilder sqlBuf = new StringBuilder(1000).append("SELECT ");
    for(Iterator<ColField> itr = colFields.iterator(); itr.hasNext();) {
      ColField colField = itr.next();
      
      //属性名作为别名
      if(sqlRequest.usingAlias) {
        sqlBuf.append(alias).append(".").append(colField.col)
        .append(" AS ").append(sqlRequest.namingStrategy.columnLabel(colField.fieldname));
      } else { //不使用别名
        sqlBuf.append(colField.col);
      }
      if(itr.hasNext()) {
        sqlBuf.append(",\n");
      }
    }
    sqlBuf.append(" FROM ").append(tablename);
    
    if(sqlRequest.byId) {
      ColField primary = findPrimary(colFields);
      String idCol = (primary != null ? primary.col : "id");
      sqlBuf.append(" WHERE ").append(sqlRequest.usingAlias ? alias + "." :  "").append(idCol).append("=");
      
      if(sqlRequest.namedParams) {
        String idField = (primary != null ? primary.fieldname : "id");
        sqlBuf.append(":").append(idField);
      } else {
        sqlBuf.append("?");
      }
    }
    sqlCache.put(key, sqlBuf.toString()); //装入缓存
    
    return new SQLReady(sqlBuf.toString(), new Object[]{}, sqlRequest.limitSql); 
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
   * @param sqlRequest 查询请求，必须有一个非空的实体对象
   * @param fullMatches 字符串查询使用全匹配，即%string%的形式，这种形式不会利用索引！
   * @return
   */
  public SQLReady conditions(SQLRequest sqlRequest) {
    if(sqlRequest.entity == null) {
      throw new java.lang.IllegalArgumentException("给出的实体类不可为空。");
    }
    ORMHelper ormHelper = ORMHelper.getInstance(sqlRequest.namingStrategy);
    Class<?> entityClass = sqlRequest.entityClass;
    
    Collection<ColField> colFields = columns(sqlRequest);
    List<String> sqls = new ArrayList<>(colFields.size()); //存SQL片段
    List<Object> args = new ArrayList<>(colFields.size()); //存参数值
    Map<String, Object> namedParams = new HashMap<>(colFields.size()); //存参数值
    
    for(Iterator<ColField> itr = colFields.iterator(); itr.hasNext();) {
      ColField colField = itr.next();
      
      Field field = ReflectUtils.findField(entityClass, colField.getFieldname());
      Object value = getField(field, sqlRequest.entity);
      if(value == null) {
        continue;
      }
      
      //处理主键
      if(ormHelper.isPrimaryKey(field)) {
        doPrimaryKey(sqlRequest, field, value, sqls, args, namedParams);
        continue;
      }
      //处理字符串
      if(ClassUtils.isAssignable(field.getType(), String.class)) {
        FullText ftAnn = getAnnotation(field, FullText.class); //全文检索
        if(ftAnn != null) {
          doFullText(sqlRequest, field, value, ftAnn, sqls, args, namedParams);
          continue;
        }
        FullMatches fmAnn = getAnnotation(field, FullMatches.class); //全匹配，代替LIKE %%
        if(fmAnn != null) {
          doFullMatches(sqlRequest, field, value, ftAnn, sqls, args, namedParams);
          continue;
        }
        //Like查询
        if(fmAnn == null && ftAnn == null) {
          doLike(sqlRequest, field, value, sqls, args, namedParams);
          continue;
        }
      }
      //处理数字
      if(ClassUtils.isAssignable(field.getType(), Number.class)) {
        RangeQuery rqAnn = getAnnotation(field, RangeQuery.class);
        if(rqAnn != null) {
          doRangeQuery(sqlRequest, field, value, rqAnn, sqls, args, namedParams);
          continue;
        }
      }
      //处理日期和时间
      if(ClassUtils.isAssignable(field.getType(), Date.class)) {
        RangeQuery rqAnn = getAnnotation(field, RangeQuery.class);
        if(rqAnn != null) {
          doRangeQuery(sqlRequest, field, value, rqAnn, sqls, args, namedParams);
          continue;
        }
      }
      
      //处理其他情况（前面没有处理的）
      String tableAlias = sqlRequest.namingStrategy.tableAlias(sqlRequest.entityClass);
      String colname = sqlRequest.namingStrategy.columnName(sqlRequest.entityClass, field);
      
      if(sqlRequest.namedParams) { //命名参数
        StringBuilder sql = new StringBuilder(100);
        if(sqlRequest.usingAlias) { //使用表名别名
          sql.append(tableAlias).append(".");
        } 
        sqls.add(sql.append(colname).append("=").append(":").append(field.getName()).toString());
        namedParams.put(field.getName(), value);
      } else {
        StringBuilder sql = new StringBuilder(100);
        if(sqlRequest.usingAlias) { //使用表名别名
          sql.append(tableAlias).append(".");
        } 
        sqls.add(new StringBuilder(100).append(colname).append("=").append("?").toString());
        args.add(value);
      }
    }
    
    String sql = Joiner.on(" AND ").join(sqls);
    if(sqlRequest.namedParams) {
      return new SQLReady(sql, namedParams, sqlRequest.limitSql);
    } else {
      return new SQLReady(sql, args.toArray(new Object[]{}), sqlRequest.limitSql);
    }
  }
  
  public SQLReady conditionsOfParameters (SQLRequest sqlRequest) {
    //TODO
    return null;
  }
  
  /**
   * 根据给定的实体类，构造一个SQL INSERT语句，
   * 如果给出的SQLRequest对象中，namedParams为<code>true</code>，则返回带有参数的SQL，数据使用MAP封装，否则返回带有?的SQL，数据采用数组封装。
   * @param sqlRequest 给定SQLRequest
   * @return SQLReady 包括SQL和参数，如果SQLRequest.namedParams为<code>true</code>, SQL语句使用属性名作为字段别名和占位符，参数采用Map存储，Key为属性名。
   * 否则，SQL语句采用?作为占位符，参数用数组保存。
   */
  public SQLReady insert(SQLRequest sqlRequest) {
    if(sqlRequest.entity == null) {
      throw new java.lang.IllegalArgumentException("给出的实体类不可为空。");
    }
    
    BaseEntity entity = sqlRequest.entity;
    if(entity.getId() == null) {
      entity.setId(DEF_IDGEN.nextId());
    }
    Class<?> entityClass = sqlRequest.entityClass;
    String tablename = sqlRequest.namingStrategy.tablename(entityClass);
    Collection<ColField> colFields = columns(sqlRequest);
    
    List<Object> args = new ArrayList<Object>(colFields.size()); //使用？做占位符
    Map<String, Object> namedParams = new LinkedHashMap<>(colFields.size()); //使用别名做占位符
    //字段列表
    List<String> prevValues = new ArrayList<String>(colFields.size());
    //占位符
    List<String> afterValues = new ArrayList<String>(colFields.size());
    
    for(Iterator<ColField> itr = colFields.iterator(); itr.hasNext();) {
      ColField colField = itr.next();
      Field field = ReflectUtils.findField(entityClass, colField.getFieldname());
      Object arg = getField(field, entity);
      if(arg == null && !sqlRequest.includesNull) { //不包括NULL字段
        continue;
      }
      prevValues.add(colField.col);
      
      if(sqlRequest.namedParams) { //使用别名，参数存放在Map中，别名为KEY
        namedParams.put(colField.fieldname, arg);
        afterValues.add(":" + colField.fieldname);
      } else { //使用？
        args.add(arg);
        afterValues.add("?");
      }
    }
    if(prevValues.isEmpty() || afterValues.isEmpty()) {
      throw new IllegalStateException("无法构造有效的INSERT语句。");
    }
    
    String cols = Joiner.on(",").join(prevValues);
    String values = Joiner.on(",").join(afterValues);
    
    StringBuilder sqlBuf = new StringBuilder(200).append("INSERT INTO ").append(tablename).append(" (\n")
        .append(cols).append(") VALUES (\n").append(values).append(")");
    
    
    if(sqlRequest.namedParams) {
      return new SQLReady(sqlBuf.toString(), namedParams, sqlRequest.limitSql);
    } else {
      return new SQLReady(sqlBuf.toString(), args.toArray(new Object[]{}), sqlRequest.limitSql);
    }
  }
  /**
   * 生成Insert SQL，忽略为<code>null</code>的字段。
   * 如果给出的SQLRequest对象中，namedParams为<code>true</code>，则返回带有参数的SQL，数据使用MAP封装，否则返回带有?的SQL，数据采用数组封装。
   * @param entity 实体类
   * @return
   */
  public SQLReady insertNonNull(SQLRequest sqlRequest) {
    sqlRequest = sqlRequest.includesNull(false);
    return insert(sqlRequest);
  }
 
  /**
   * 根据SQLRequest构造一个SQL UPDATE语句及其对应的参数数组。
   * <ul>
   *     <li>在SET子句中忽略主键字段</li>
   *     <li>根据sqlRequest的设置，可以只处理不为空的字段</li>
   *     <li>根据SQLRequest中的NamingStrategy，构造各个字段的名字</li>
   *     <li>根据sqlRequest的设置，如果byId为true，并且，SQLRequest#entity的主键不为空，则自动追加WHERE id=?子句，并且在参数中加入ID值</li>
   * </ul>
   * @param sqlRequest
   * @return
   */
  public SQLReady update(SQLRequest sqlRequest) {
    ORMHelper ormHelper = ORMHelper.getInstance(sqlRequest.namingStrategy);
    
    if(sqlRequest.entity == null) {
      throw new NullPointerException("给出的实体类不可为空。");
    }
    Class<?> entityClass = sqlRequest.entityClass;
    String tablename = sqlRequest.namingStrategy.tablename(entityClass);
    
    Collection<ColField> colFields = columns(sqlRequest);
    if(colFields == null || colFields.isEmpty()) {
      throw new java.lang.IllegalArgumentException("无法获取实体类的属性。");
    }
    List<Object> args = new ArrayList<>(colFields.size()); //存放SQL对应的参数
    Map<String, Object> namedParams = new LinkedHashMap<>(colFields.size()); //使用别名做占位符
    List<String> sqls = new ArrayList<>(colFields.size()); //存放col=?
    
    for(ColField cf : colFields) {
      Field field = ReflectUtils.findField(entityClass, cf.getFieldname());
      if(ormHelper.isPrimaryKey(field)) { //主键忽略
        continue;
      }
      Object v = getField(field, sqlRequest.entity);
      if(v == null && !sqlRequest.includesNull) {
        continue;
      }
      if(sqlRequest.namedParams) {
        namedParams.put(field.getName(), v);
        sqls.add(sqlRequest.namingStrategy.columnName(sqlRequest.entityClass, field) + "=:" + field.getName());
      } else {
        args.add(v);
        sqls.add(sqlRequest.namingStrategy.columnName(sqlRequest.entityClass, field) + "=?");
      }
    }
    if(sqls.isEmpty()) {
      throw new IllegalStateException("无法构造有效的UPDATE语句。");
    }
    final StringBuilder sqlBuf = new StringBuilder(100).append("UPDATE ").append(tablename).append(" SET ");
    sqlBuf.append(Joiner.on(",").join(sqls));
    //ByID更新
    if(sqlRequest.byId && sqlRequest.entity.getId() != null) {
      ColField primary = findPrimary(colFields);
      String idCol = (primary != null ? primary.col : "id");
      sqlBuf.append(" WHERE ").append(idCol).append("=");
      
      if(sqlRequest.namedParams) {
        String idField = (primary != null ? primary.fieldname : "id");
        sqlBuf.append(":").append(idField);
      } else {
        sqlBuf.append("?");
      }
      args.add(sqlRequest.entity.getId());
    }
    
    if(sqlRequest.namedParams) {
      return new SQLReady(sqlBuf.toString(), namedParams, sqlRequest.limitSql);
    } else {
      return new SQLReady(sqlBuf.toString(), args.toArray(new Object[]{}), sqlRequest.limitSql);
    }
  }
  
  /**
   * 根据SQLRequest构造一个SQL UPDATE语句及其对应的参数数组。
   * <ul>
   *     <li>只处理不为空的字段，并且，在SET子句中忽略主键字段</li>
   *     <li>根据SQLRequest中的NamingStrategy，构造各个字段的名字</li>
   *     <li>如果SQLRequest#entity的主键不为空，则自动追加WHERE id=?子句，并且在参数中加入ID值</li>
   * </ul>
   * @param sqlRequest
   * @param byId
   * @return
   */
  public SQLReady updateById(SQLRequest sqlRequest) {
    sqlRequest.byId(true);
    return this.update(sqlRequest);
  }
  
  /**
   * 根据给定的SQLRequest，获取对应的列名-字段名列表
   * @param sqlRequest 给定实SQLRequest
   * @return List of {@link ColField}
   */
  public Collection<ColField> columns(SQLRequest sqlRequest) {
    String key = colKey(sqlRequest);
    Collection<ColField> colFields = columnCache.get(key);
    if(colFields != null) {
      return colFields;
    }
    
    colFields = getColFields(sqlRequest);
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
    return limitSql(sql, start, limit, null);
  }
  
  /**
   * 返回基于LimitSql对象的Limit SQL
   * @param sql 原始的SQL
   * @param start Start index of the rows, first is 0.
   * @param limit Max results
   * @return SQL with limit
   */
  public String limitSql(String sql, int start, int limit, LimitSql limitSql) {
    if(limitSql == null) {
      limitSql = SQLRequest.DEFAULT_LIMIT_SQL;
    }
    return limitSql.getLimitSql(sql, start, limit);
  }
  
  /**
   * 使用下划线命名法，取得实体类对应的表名
   */
  public String getTablename(Class<?> entityClass) {
    return SQLRequest.DEFAULT_NAME_STRATEGY.tablename(entityClass);
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
  
 
  
  private List<ColField> getColFields(SQLRequest sqlRequest) {
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
      Field field = ReflectUtils.findField(sqlRequest.entityClass, propertyDescriptor.getName());
      if(readMethod == null || field == null) {
        continue;
      }
      //如果标注为Transient,则忽略
      if(getAnnotation(field, Transient.class) != null) {
        continue;
      }
      if(getAnnotation(field, java.beans.Transient.class) != null) {
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
      
      ColField colField = new ColField(columnName, fieldname, readMethod.getAnnotation(Id.class) != null);
      colFields.add(colField);
    }
    
    colFields.sort(new Comparator<ColField>() {
      @Override
      public int compare(ColField cf1, ColField cf2) {
        if(cf1 == null && cf2 == null) {
          return 0;
        }
        
        if(cf1 == null && cf2 != null) {
          return -1;
        }
        
        if(cf1 != null && cf2 == null) {
          return 1;
        }
        
        if("id".equalsIgnoreCase(cf1.col)) {
          return -1;
        }
        
        if(cf1.col != null) {
          return cf1.col.compareToIgnoreCase(cf2.col);
        }
        
        throw new NullPointerException("字段名不可为空。");
      }
    });
    
    return colFields;
  }
  

  private Object getField(Field field, Object entity) {
    Method getter = ORMHelper.getInstance().getAccessMethod(field);
    Object value;
    if(getter != null) {
      value = ReflectUtils.invokeMethod(getter, entity);
    } else {
      value = getField(field, entity);
    }
    
    return value;
  }
  
  private void doRangeQuery(SQLRequest sqlRequest, Field field, Object value, RangeQuery rqAnn, List<String> sqls, List<Object> args, Map<String, Object> namedParams) {
    String tableAlias = sqlRequest.namingStrategy.tableAlias(sqlRequest.entityClass);
    String colname = sqlRequest.namingStrategy.columnName(sqlRequest.entityClass, field);
    //处理START
    String startFieldName = rqAnn.start();
    StringBuilder sql = new StringBuilder(100);
    if(sqlRequest.usingAlias) {
      sql.append(tableAlias).append(".");
    } 
    
    if(StringUtils.isNotBlank(startFieldName)) {
      Field startField = ReflectUtils.findField(field.getDeclaringClass(), startFieldName);
      if(startField == null) {
        throw new java.lang.RuntimeException("属性不存在 " + startFieldName);
      }
      Object startValue = getField(startField, sqlRequest.entity); //开始值
      if(startValue != null) {
        sql.append(colname).append(rqAnn.greatAndEquals() ? ">=" : ">");
        if(sqlRequest.namedParams) {
          sql.append(":").append(startFieldName);
          namedParams.put(startFieldName, startValue);
        } else {
          sql.append("?");
          args.add(startValue);
        }
        sqls.add(sql.toString());
      }
    }
    
    //处理END
    String endFieldName = rqAnn.end();
    StringBuilder sql2 = new StringBuilder(100);
    if(sqlRequest.usingAlias) {
      sql2.append(tableAlias).append(".");
    } 
    
    if(StringUtils.isNotBlank(endFieldName)) {
      Field endField = ReflectUtils.findField(field.getDeclaringClass(), endFieldName);
      if(endField == null) {
        throw new java.lang.RuntimeException("属性不存在 " + startFieldName);
      }
      Object endValue = getField(endField, sqlRequest.entity); //结束值
      if(endValue != null) {
        sql2.append(colname).append(rqAnn.lessAndEquals() ? "<=" : "<");
        if(sqlRequest.namedParams) {
          sql2.append(":").append(endFieldName);
          namedParams.put(endFieldName, endValue);
        } else {
          sql2.append("?");
          args.add(endValue);
        }
        sqls.add(sql2.toString());
      }
    }
  }
  
  private void doPrimaryKey(SQLRequest sqlRequest, Field field, Object value, List<String> sqls, List<Object> args, Map<String, Object> namedParams) {
    String colname = sqlRequest.namingStrategy.columnName(sqlRequest.entityClass, field);
    String tableAlias = sqlRequest.namingStrategy.tableAlias(sqlRequest.entityClass);
    
    if(sqlRequest.namedParams) {
      sqls.add((sqlRequest.usingAlias ? tableAlias + "." : "") + colname + "=:id");
      namedParams.put("id", value);
    } else {
      sqls.add((sqlRequest.usingAlias ? tableAlias + "." : "") + colname + "=?");
      args.add(value);
    }
  }
  
  private void doFullText(SQLRequest sqlRequest, Field field, Object value, FullText ftAnn, List<String> sqls, List<Object> args, Map<String, Object> namedParams) {
    String colname = ftAnn.relativeColumn(); //取得FullText设定的列
    if(StringUtils.isBlank(colname)) {
      colname = sqlRequest.namingStrategy.columnName(sqlRequest.entityClass, field);
    }
    String tableAlias = sqlRequest.namingStrategy.tableAlias(sqlRequest.entityClass);
    
    StringBuilder sql = new StringBuilder(100).append(" MATCH(")
        .append((sqlRequest.usingAlias ? tableAlias + "." : ""))
        .append(colname).append(") AGAINST (");
    if(sqlRequest.namedParams) {
      sql.append(":").append(field.getName());
      namedParams.put(field.getName(), value);
    } else {
      sql.append("?");
      args.add(value);
    }
    sql.append(" IN BOOLEAN MODE)");
    
    sqls.add(sql.toString());
  }
  
  private void doLike(SQLRequest sqlRequest, Field field, Object value, List<String> sqls, List<Object> args, Map<String, Object> namedParams) {
    String colname = sqlRequest.namingStrategy.columnName(sqlRequest.entityClass, field);
    String tableAlias = sqlRequest.namingStrategy.tableAlias(sqlRequest.entityClass);
    
    StringBuilder sql = new StringBuilder(100).append((sqlRequest.usingAlias ? tableAlias + "." : "")).append(colname).append(" LIKE ");
    if(sqlRequest.namedParams) {
      sql.append(":").append(field.getName());
      namedParams.put(field.getName(), value.toString() + "%");
    } else {
      sql.append("?");
      args.add(value.toString() + "%");
    }
    sqls.add(sql.toString());
  }
  
  private void doFullMatches(SQLRequest sqlRequest, Field field, Object value, FullText ftAnn, List<String> sqls, List<Object> args, Map<String, Object> namedParams) {
    String colname = sqlRequest.namingStrategy.columnName(sqlRequest.entityClass, field);
    String tableAlias = sqlRequest.namingStrategy.tableAlias(sqlRequest.entityClass);
    //LOCATE(?, c.name) > 0
    
    StringBuilder sql = new StringBuilder(100).append(" LOCATE(");
    if(sqlRequest.namedParams) {
      sql.append(":")
      .append(field.getName())
      .append(",")
      .append((sqlRequest.usingAlias ? tableAlias + "." : ""))
      .append(colname).append(") > 0 ");
      namedParams.put(field.getName(), value);
    } else {
      sql.append("?")
      .append(",")
      .append((sqlRequest.usingAlias ? tableAlias + "." : ""))
      .append(colname).append(") > 0");
      args.add(value);
    }
    
    sqls.add(sql.toString());
  }
  
  private <T extends Annotation> T getAnnotation(Field field, Class<T> annotationClass) {
    Assert.notNull(field);
    ORMHelper ormHelper = ORMHelper.getInstance();
    
    T ann = field.getAnnotation(annotationClass);
    if(ann == null) {
      Method getter = ormHelper.getAccessMethod(field);
      ann = getter.getAnnotation(annotationClass);
    }
    
    return ann;
  }
  
  private ColField findPrimary(Collection<ColField> colFields) {
    if(colFields == null || colFields.isEmpty()) {
      return null;
    }
    for(Iterator<ColField> itr = colFields.iterator(); itr.hasNext();) {
      ColField cf = itr.next();
      if(cf.isPrimary) {
        return cf;
      }
    }
    return null;
  }
 
  
  /**
   * 用于装载数据库字段col, 和实体类属性field的对应关系
   * @author leesam
   *
   */
  public static final class ColField implements java.io.Serializable {
    private String col;
    private String fieldname;
    private Field field;
    private boolean isPrimary = false;
    
    public ColField() {
      
    }
    
    public ColField(String col, String fieldname, boolean isPrimary) {
      this.col = col;
      this.fieldname = fieldname;
      this.isPrimary = isPrimary;
    }
    
    public ColField(String col, Field field, boolean isPrimary) {
      this.col = col;
      this.field = field;
      if(field != null) {
        this.fieldname = field.getName();
      }
      this.isPrimary = isPrimary;
    }

    public String getCol() {
      return col;
    }

    public String getFieldname() {
      return fieldname;
    }

    public boolean isPrimary() {
      return isPrimary;
    }

    public void setCol(String col) {
      this.col = col;
    }

    public void setFieldname(String fieldname) {
      this.fieldname = fieldname;
    }

    public void setPrimary(boolean isPrimary) {
      this.isPrimary = isPrimary;
    }

    public Field getField() {
      return field;
    }

    public void setField(Field field) {
      this.field = field;
    }
  }

  
}
