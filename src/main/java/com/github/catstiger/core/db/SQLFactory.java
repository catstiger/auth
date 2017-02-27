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

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
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
   * @param supportsJoin 是否包括外键字段JOIN查询，可以处理一级外键。
   * @return SQL
   */
  public SQLReady select(SQLRequest sqlRequest, boolean supportsJoin) {
    String key = sqlKey(sqlRequest, "select", supportsJoin);
    //从缓存中取得SQL
    String sqlObj = sqlCache.get(key);
    if(sqlObj != null) {
      return new SQLReady(sqlObj, new Object[]{}, sqlRequest.limitSql); 
    }
    Collection<ColField> colFields = columns(sqlRequest, supportsJoin);
    String tablename = sqlRequest.namingStrategy.tablename(sqlRequest.entityClass);
    
    final StringBuilder sqlBuf = new StringBuilder(1000).append("SELECT ");
    
    List<String> sqlSegs = new ArrayList<String>(colFields.size()); //一个一个SQL片段，最后join为完整的字段列表
    //SELECT后的字段列表
    for(Iterator<ColField> itr = colFields.iterator(); itr.hasNext();) {
      ColField colField = itr.next();
      if(colField.isForeign) {
        continue;
      }
      StringBuilder sqlSeg = new StringBuilder(100);
      //属性名作为别名
      if(sqlRequest.usingAlias) {
        String alias = sqlRequest.namingStrategy.tableAlias(colField.ownerClass);  //字段所在表别名
        sqlSeg.append(alias).append(".").append(colField.col)
        .append(" AS ").append(sqlRequest.namingStrategy.columnLabel(colField.fieldname));
      } else { //不使用别名
        sqlSeg.append(colField.col);
      }
      sqlSegs.add(sqlSeg.toString());
    }
    sqlBuf.append(Joiner.on(",\n").join(sqlSegs));
    
    //FROM后面的主表
    sqlBuf.append(" \n FROM ").append(tablename);
    String mainAlias = sqlRequest.namingStrategy.tableAlias(sqlRequest.entityClass);  //字段所在表别名
    sqlBuf.append(" ").append(mainAlias).append(" \n");
    
    ColField primary = findPrimary(colFields);
    
    //关联查询
    if(supportsJoin) {
      for(Iterator<ColField> itr = colFields.iterator(); itr.hasNext();) {
        ColField colField = itr.next();
        if(!colField.isForeign) {
          continue;
        }
        sqlBuf.append(" JOIN \n")
        .append(colField.tablename)
        .append(" ")
        .append(colField.alias)
        .append(" on (").append(colField.alias).append(".").append(colField.fkPrimaryColumn)
        .append("=")
        .append(mainAlias).append(".").append(colField.col).append(") \n");
      }
    }
    
    if(sqlRequest.byId) {
      String idCol = (primary != null ? primary.col : "id");
      
      sqlBuf.append(" WHERE ").append(sqlRequest.usingAlias ? mainAlias + "." :  "").append(idCol).append("=");
      if(sqlRequest.namedParams) {
        String idField = (primary != null ? mainAlias + "." + primary.fieldname : mainAlias + ".id");
        sqlBuf.append(":").append(idField);
      } else {
        sqlBuf.append("?");
      }
    }
    sqlBuf.append("\n");
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
  public SQLReady conditions(SQLRequest sqlRequest, boolean supportsJoin) {
    if(sqlRequest.entity == null) {
      throw new java.lang.IllegalArgumentException("给出的实体类不可为空。");
    }
    ORMHelper ormHelper = ORMHelper.getInstance(sqlRequest.namingStrategy);
    
    Collection<ColField> colFields = columns(sqlRequest, supportsJoin);
    List<String> sqls = new ArrayList<>(colFields.size()); //存SQL片段
    List<Object> args = new ArrayList<>(colFields.size()); //存参数值
    Map<String, Object> namedParams = new HashMap<>(colFields.size()); //存参数值
    
    for(Iterator<ColField> itr = colFields.iterator(); itr.hasNext();) {
      ColField colField = itr.next();
      if(colField.isForeign) {
        continue;
      }
      
      Field field = colField.getField();
      Object value = null;
      if(colField.ownerValue != null) {
        value = getField(field, colField.ownerValue);
      }
      if(value == null) {
        continue;
      }
      
      //处理主键
      if(ormHelper.isPrimaryKey(field)) {
        doPrimaryKey(sqlRequest, colField, value, sqls, args, namedParams);
        continue;
      }
      //处理字符串
      if(ClassUtils.isAssignable(field.getType(), String.class)) {
        FullText ftAnn = getAnnotation(field, FullText.class); //全文检索
        if(ftAnn != null) {
          doFullText(sqlRequest, colField, value, ftAnn, sqls, args, namedParams);
          continue;
        }
        FullMatches fmAnn = getAnnotation(field, FullMatches.class); //全匹配，代替LIKE %%
        if(fmAnn != null) {
          doFullMatches(sqlRequest, colField, value, ftAnn, sqls, args, namedParams);
          continue;
        }
        //Like查询
        if(fmAnn == null && ftAnn == null) {
          doLike(sqlRequest, colField, value, sqls, args, namedParams);
          continue;
        }
      }
      //处理数字
      if(ClassUtils.isAssignable(field.getType(), Number.class)) {
        RangeQuery rqAnn = getAnnotation(field, RangeQuery.class);
        if(rqAnn != null) {
          doRangeQuery(sqlRequest, colField, value, rqAnn, sqls, args, namedParams);
          continue;
        }
      }
      //处理日期和时间
      if(ClassUtils.isAssignable(field.getType(), Date.class)) {
        RangeQuery rqAnn = getAnnotation(field, RangeQuery.class);
        if(rqAnn != null) {
          doRangeQuery(sqlRequest, colField, value, rqAnn, sqls, args, namedParams);
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
    Collection<ColField> colFields = columns(sqlRequest, false);
    
    List<Object> args = new ArrayList<Object>(colFields.size()); //使用？做占位符
    Map<String, Object> namedParams = new LinkedHashMap<>(colFields.size()); //使用别名做占位符
    //字段列表
    List<String> prevValues = new ArrayList<String>(colFields.size());
    //占位符
    List<String> afterValues = new ArrayList<String>(colFields.size());
    
    for(Iterator<ColField> itr = colFields.iterator(); itr.hasNext();) {
      ColField colField = itr.next();
      Field field = colField.getField();
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
    
    Collection<ColField> colFields = columns(sqlRequest, false);
    if(colFields == null || colFields.isEmpty()) {
      throw new java.lang.IllegalArgumentException("无法获取实体类的属性。");
    }
    List<Object> args = new ArrayList<>(colFields.size()); //存放SQL对应的参数
    Map<String, Object> namedParams = new LinkedHashMap<>(colFields.size()); //使用别名做占位符
    List<String> sqls = new ArrayList<>(colFields.size()); //存放col=?
    
    for(ColField cf : colFields) {
      Field field = cf.getField();
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
  public Collection<ColField> columns(SQLRequest sqlRequest, boolean supportsJoin) {
    Collection<ColField> colFields = getColFields(sqlRequest, supportsJoin);
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
  private String sqlKey(SQLRequest sqlRequest, String type, boolean supportsJoin) {
    StringBuilder keyBuilder = new StringBuilder(200).append(type).append(sqlRequest.toString()).append(supportsJoin);
    return DigestUtils.md5DigestAsHex(keyBuilder.toString().getBytes(Charset.forName("UTF-8")));
  }
  
  private List<ColField> getColFields(SQLRequest sqlRequest, boolean supportsJoin) {
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
      String columnName = sqlRequest.namingStrategy.columnName(sqlRequest.entityClass, fieldname);      
      boolean isPrimary = (getAnnotation(field, Id.class) != null);
      Class<?> type = field.getType();
      //Class<?> ownerClass = sqlRequest.entityClass;
      boolean isForeign = (getAnnotation(field, JoinColumn.class) != null && type.getAnnotation(Entity.class) != null);
      //处理外键中的字段
      if(isForeign && supportsJoin) {
        ColField colField = new ColField(columnName, field, isPrimary, isForeign, type, sqlRequest.entityClass);
        colField.tablename = sqlRequest.namingStrategy.tablename(colField.type);
        String alias = sqlRequest.namingStrategy.tableAlias(colField.type);
        int aliasCount = 0;
        //检查有无重复的别名
        for(ColField cf : colFields) {
          if(cf.alias != null && cf.isForeign && cf.alias.startsWith(alias + "_")) {
            aliasCount++;
          }
        }
        colField.alias = alias + "_" + aliasCount; //外键的表的别名，要加下标，因为可能重复引用同一个表
        //外键需要加载对应的entity
        Object fkValue = null;
        if(sqlRequest.entity != null) {
          fkValue = this.getField(field, sqlRequest.entity);
          if(fkValue == null) {
            fkValue = ReflectUtils.instantiate(colField.type);
            colField.ownerValue = sqlRequest.entity;
          }
        }
        colFields.add(colField);
        //关联表中的数据
        SQLRequest sr = new SQLRequest(type)
            .includes(sqlRequest.includes.toArray(new String[]{}))
            .excludes(sqlRequest.excludes.toArray(new String[]{}))
            .usingAlias(sqlRequest.usingAlias)
            .includesNull(sqlRequest.includesNull);
        
        List<ColField> fkFields = getColFields(sr, false);
        final Object foreignValue = fkValue;
        fkFields.forEach(e -> {
          e.ownerValue = foreignValue;
        });
        colFields.addAll(fkFields);

        //外键关联字段，要保存对应表的主键
        for(ColField fkcf : fkFields) {
          if(fkcf.isPrimary) {
            colField.fkPrimaryColumn = fkcf.col;
            break;
          }
        }
      } else {
        ColField colField = new ColField(columnName, field, isPrimary, isForeign, type, sqlRequest.entityClass);
        colField.tablename = sqlRequest.namingStrategy.tablename(colField.ownerClass);
        colField.alias = sqlRequest.namingStrategy.tableAlias(colField.ownerClass);
        
        if(sqlRequest.entity != null) {
          colField.ownerValue = sqlRequest.entity;
        }
        colFields.add(colField);
      }
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
  
  private void doRangeQuery(SQLRequest sqlRequest, ColField colField, Object value, RangeQuery rqAnn, List<String> sqls, List<Object> args, Map<String, Object> namedParams) {
    //处理START
    String startFieldName = rqAnn.start();
    StringBuilder sql = new StringBuilder(100);
    if(sqlRequest.usingAlias) {
      sql.append(colField.alias).append(".");
    } 
    
    if(StringUtils.isNotBlank(startFieldName)) {
      Field startField = ReflectUtils.findField(colField.ownerClass, startFieldName);
      if(startField == null) {
        throw new java.lang.RuntimeException("属性不存在 " + startFieldName);
      }
      Object startValue = getField(startField, sqlRequest.entity); //开始值
      if(startValue != null) {
        sql.append(colField.col).append(rqAnn.greatAndEquals() ? ">=" : ">");
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
      sql2.append(colField.alias).append(".");
    } 
    
    if(StringUtils.isNotBlank(endFieldName)) {
      Field endField = ReflectUtils.findField(colField.ownerClass, endFieldName);
      if(endField == null) {
        throw new java.lang.RuntimeException("属性不存在 " + startFieldName);
      }
      Object endValue = getField(endField, sqlRequest.entity); //结束值
      if(endValue != null) {
        sql2.append(colField.col).append(rqAnn.lessAndEquals() ? "<=" : "<");
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
  
  private void doPrimaryKey(SQLRequest sqlRequest, ColField colField, Object value, List<String> sqls, List<Object> args, Map<String, Object> namedParams) {
    if(sqlRequest.namedParams) {
      sqls.add((sqlRequest.usingAlias ? colField.alias + "." : "") + colField.col + "=:id");
      namedParams.put("id", value);
    } else {
      sqls.add((sqlRequest.usingAlias ? colField.alias + "." : "") + colField.col + "=?");
      args.add(value);
    }
  }
  
  private void doFullText(SQLRequest sqlRequest, ColField colField, Object value, FullText ftAnn, List<String> sqls, List<Object> args, Map<String, Object> namedParams) {
    String colname = ftAnn.relativeColumn(); //取得FullText设定的列
    if(StringUtils.isBlank(colname)) {
      colname = colField.col;
    }
    
    StringBuilder sql = new StringBuilder(100).append(" MATCH(")
        .append((sqlRequest.usingAlias ? colField.alias + "." : ""))
        .append(colname).append(") AGAINST (");
    if(sqlRequest.namedParams) {
      sql.append(":").append(colField.field.getName());
      namedParams.put(colField.field.getName(), value);
    } else {
      sql.append("?");
      args.add(value);
    }
    sql.append(" IN BOOLEAN MODE)");
    
    sqls.add(sql.toString());
  }
  
  private void doLike(SQLRequest sqlRequest, ColField colField, Object value, List<String> sqls, List<Object> args, Map<String, Object> namedParams) {
    StringBuilder sql = new StringBuilder(100).append((sqlRequest.usingAlias ? colField.alias + "." : "")).append(colField.col).append(" LIKE ");
    if(sqlRequest.namedParams) {
      sql.append(":").append(colField.field.getName());
      namedParams.put(colField.field.getName(), value.toString() + "%");
    } else {
      sql.append("?");
      args.add(value.toString() + "%");
    }
    sqls.add(sql.toString());
  }
  
  private void doFullMatches(SQLRequest sqlRequest, ColField colField, Object value, FullText ftAnn, List<String> sqls, List<Object> args, Map<String, Object> namedParams) {
    StringBuilder sql = new StringBuilder(100).append(" LOCATE(");
    if(sqlRequest.namedParams) {
      sql.append(":")
      .append(colField.field.getName())
      .append(",")
      .append((sqlRequest.usingAlias ? colField.alias + "." : ""))
      .append(colField.col).append(") > 0 ");
      namedParams.put(colField.field.getName(), value);
    } else {
      sql.append("?")
      .append(",")
      .append((sqlRequest.usingAlias ? colField.alias + "." : ""))
      .append(colField.col).append(") > 0");
      args.add(value);
    }
    
    sqls.add(sql.toString());
  }
  
  private static <T extends Annotation> T getAnnotation(Field field, Class<T> annotationClass) {
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
    public Object ownerValue;
    public String fkPrimaryColumn;
    private String col;
    private String fieldname;
    private Field field;
    private boolean isPrimary = false;
    private boolean isForeign = false;
    private Class<?> type;
    private Class<?> ownerClass;
    private String tablename;
    private String alias;
    
    public ColField() {
      
    }
    
    public ColField(String col, String fieldname, boolean isPrimary, boolean isForeign, Class<?> type, Class<?> ownerClass) {
      this.col = col;
      this.fieldname = fieldname;
      this.isPrimary = isPrimary;
      this.isForeign = isForeign;
      this.type = type;
      this.ownerClass = ownerClass;
      if(ownerClass != null) {
        field = ReflectUtils.findField(ownerClass, fieldname);
      }
      if(type == null && field != null) {
        type = field.getType();
      }
    }
    
    public ColField(String col, Field field, boolean isPrimary, boolean isForeign, Class<?> type, Class<?> ownerClass) {
      this.col = col;
      this.field = field;
      this.isForeign = isForeign;
      this.type = type;
      this.ownerClass = ownerClass;
      if(field != null) {
        this.fieldname = field.getName();
        if(ownerClass == null) {
          ownerClass = field.getDeclaringClass();
        }
      }
      if(type == null && field != null) {
        type = field.getType();
      }
      this.isPrimary = isPrimary;
    }
    
    public Field getField() {
      if(this.field == null && this.ownerClass != null) {
        this.field = ReflectUtils.findField(this.ownerClass, this.fieldname);
      }
      return field;
    }
   
  }

  
}
