package com.github.catstiger.core.db;

import java.beans.PropertyDescriptor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.Table;
import javax.persistence.Transient;

import org.springframework.util.Assert;
import org.springframework.util.DigestUtils;

import com.github.catstiger.core.db.id.IdGen;
import com.github.catstiger.core.db.id.SnowflakeIDWorker;
import com.github.catstiger.core.entity.BaseEntity;
import com.github.catstiger.utils.ClassUtils;
import com.github.catstiger.utils.CollectionUtils;
import com.github.catstiger.utils.ReflectUtils;
import com.github.catstiger.utils.StringUtils;
import com.google.common.base.Joiner;
import com.google.common.base.Predicate;
import com.google.common.base.Splitter;
import com.google.common.collect.Collections2;


public final class SQLFactory {
  private static final String SQL_SELECT = "SELECT_";
  private static final String SQL_INSERT = "INSERT_";
  private static final String SQL_UPDATE = "UPDATE_";
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
   * 相当于调用select(entityClass, null, null, false);
   * @see #select(Class, List, List, Boolean)
   */
  public String select(Class<?> entityClass) {
    return select(entityClass, null, null, false);
  }
  
  /**
   * 相当于调用select(entityClass, null, null, fieldAsAlias);
   * @see #select(Class, List, List, Boolean)
   */
  public String select(Class<?> entityClass, boolean fieldAsAlias) {
    return select(entityClass, null, null, fieldAsAlias);
  }
  
  /**
   * 相当于调用select(entityClass, includeFields, null, fieldAsAlias);
   * @see #select(Class, List, List, Boolean)
   */
  public String selectIncludes(Class<?> entityClass, List<String> includeFields, boolean fieldAsAlias) {
    return select(entityClass, includeFields, null, fieldAsAlias);
  }
  
  /**
   * 相当于调用select(entityClass, includeFields, null, false);
   * @see #select(Class, List, List, Boolean)
   */
  public String selectIncludes(Class<?> entityClass, List<String> includeFields) {
    return select(entityClass, includeFields, null, false);
  }
  
  /**
   * 相当于调用select(entityClass, null, excludeFields, fieldAsAlias);
   * @see #select(Class, List, List, Boolean)
   */
  public String selectExcludes(Class<?> entityClass, List<String> excludeFields, boolean fieldAsAlias) {
    return select(entityClass, null, excludeFields, fieldAsAlias);
  }
  
  /**
   * 相当于调用<code>select(entityClass, null, excludeFields, false);</code>
   * @see #select(Class, List, List, Boolean)
   */
  public String selectExcludes(Class<?> entityClass, List<String> excludeFields) {
    return select(entityClass, null, excludeFields, false);
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
   * @param entityClass 实体类
   * @param includeFields 必须包含的字段名，不属于其中的，不会包含
   * @param excludeFields 必须不包含的字段名，属于其中的，不会包含
   * @param fieldAsAlias 是否需要将属性名作为字段名别名, 缺省为false
   * @return SQL
   */
  public String select(Class<?> entityClass, List<String> includeFields, List<String> excludeFields, boolean fieldAsAlias) {
    String key = sqlKey(entityClass, includeFields, excludeFields, fieldAsAlias, SQL_SELECT);
    //从缓存中取得SQL
    String sqlObj = sqlCache.get(key);
    if(sqlObj != null) {
      return sqlObj; 
    }
    Collection<ColField> colFields = columns(entityClass, includeFields, excludeFields, fieldAsAlias);
    
    final String alias = getTableAlias(entityClass);
    String tablename = getTablename(entityClass, alias);
    
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
   * 相当于调用insert(entityClass, null, null, false);
   * @see {@link #insert(Class, List, List, Boolean)}
   */
  public String insert(Class<?> entityClass) {
    return insert(entityClass, null, null, false);
  }
  
  /**
   * 相当于调用insert(entityClass, null, null, namedParams);
   * @see {@link #insert(Class, List, List, Boolean)}
   */
  public String insert(Class<?> entityClass, boolean namedParams) {
    return insert(entityClass, null, null, namedParams);
  }
  
  /**
   * 相当于调用insert(entityClass, includeFields, null, namedParams);
   * @see {@link #insert(Class, List, List, Boolean)}
   */
  public String insertIncludes(Class<?> entityClass, List<String> includeFields, boolean namedParams) {
    return insert(entityClass, includeFields, null, namedParams);
  }
  
  /**
   * 相当于调用insert(entityClass, includeFields, null, false);
   * @see {@link #insert(Class, List, List, Boolean)}
   */
  public String insertIncludes(Class<?> entityClass, List<String> includeFields) {
    return insert(entityClass, includeFields, null, false);
  }
  
  /**
   * 相当于调用insert(entityClass, null, excludeFields, namedParams);
   * @see {@link #insert(Class, List, List, Boolean)}
   */
  public String insertExcludes(Class<?> entityClass, List<String> excludeFields, boolean namedParams) {
    return insert(entityClass, null, excludeFields, namedParams);
  }
  
  /**
   * 相当于调用insert(entityClass, null, excludeFields, false);
   * @see {@link #insert(Class, List, List, Boolean)}
   */
  public String insertExcludes(Class<?> entityClass, List<String> excludeFields) {
    return insert(entityClass, null, excludeFields, false);
  }
  
  /**
   * 根据给定的实体类，构造一个SQL INSERT语句
   * @param entityClass 给定实体类，必须用Entity标注
   * @param includeFields 必须包含的字段名
   * @param excludeFields 必须不包含的字段名
   * @param namedParams 是否为参数命名，如果为false，所有参数都用?代替，否则，用实体类属性名称代替，例如:INSERT INTO users(id,user_name) VALUES (:id,:userName)
   * @return SQL
   */
  public String insert(Class<?> entityClass, List<String> includeFields, List<String> excludeFields, boolean namedParams) {
    String key = sqlKey(entityClass, includeFields, excludeFields, namedParams, SQL_INSERT);
    //从缓存中取得SQL
    String sqlObj = sqlCache.get(key);
    if(sqlObj != null) {
      return sqlObj; 
    }
    String tablename = getTablename(entityClass);
    
    Collection<ColField> colFields = columns(entityClass, includeFields, excludeFields, false);
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
      
      afterValues.append(namedParams ? (":" + colField.field) : "?");
      if(itr.hasNext()) {
        afterValues.append(namedParams ? ",\n" : "," );
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
  public SQLReady insert(BaseEntity entity, boolean namedParams) {
    if(entity == null) {
      throw new NullPointerException("给出的实体类不可为空。");
    }
    if(entity.getId() == null) {
      entity.setId(DEF_IDGEN.nextId());
    }
    Class<?> entityClass = entity.getClass();
    
    String tablename = getTablename(entityClass);
    Collection<ColField> colFields = columns(entityClass, false);
    
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
      
      afterValues.append(namedParams ? (":" + colField.field) : "?");
      if(itr.hasNext()) {
        afterValues.append(namedParams ? ",\n" : "," );
      } 
    }
    StringBuilder sqlBuf = new StringBuilder(200).append("INSERT INTO ").append(tablename).append(" (\n")
        .append(prevValues.toString()).append(") VALUES (\n").append(afterValues.toString()).append(")");
    
    return new SQLReady(sqlBuf.toString(), args.toArray(new Object[]{}));
  }
  
  /**
   * @see #update(Class, List, List, Boolean)
   */
  public String update(Class<?> entityClass) {
    return this.update(entityClass, null, null, false);
  }
  
  /**
   * @see #update(Class, List, List, Boolean)
   */
  public String update(Class<?> entityClass, boolean namedParams) {
    return this.update(entityClass, null, null, namedParams);
  }
  
  /**
   * @see #update(Class, List, List, Boolean)
   */
  public String updateIncludes(Class<?> entityClass, List<String> includeFields, boolean namedParams) {
    return this.update(entityClass, includeFields, null, namedParams);
  }
  
  /**
   * @see #update(Class, List, List, Boolean)
   */
  public String updateIncludes(Class<?> entityClass, List<String> includeFields) {
    return this.update(entityClass, includeFields, null, false);
  }
  
  /**
   * @see #update(Class, List, List, Boolean)
   */
  public String updateExcludes(Class<?> entityClass, List<String> excludeFields, boolean namedParams) {
    return update(entityClass, null, excludeFields, namedParams);
  }
  
  /**
   * @see #update(Class, List, List, Boolean)
   */
  public String updateExcludes(Class<?> entityClass, List<String> excludeFields) {
    return update(entityClass, null, excludeFields, false);
  }
  
  /**
   * 根据给定的实体类，生成update语句
   * @param entityClass 实体类，必须用@Entity标注
   * @param includeFields 必须包含的字段
   * @param excludeFields 必须不包含的字段
   * @param byId 如果true,并且字段列表中包括主键，则添加WHERE PK=?
   * @param namedParams 是否使用命名参数
   * @return
   */
  public String update(Class<?> entityClass, List<String> includeFields, List<String> excludeFields, final boolean namedParams) {
    String key = sqlKey(entityClass, includeFields, excludeFields, namedParams, SQL_UPDATE);
    //从缓存中取得SQL
    String sqlObj = sqlCache.get(key);
    if(sqlObj != null) {
      return sqlObj; 
    }
    String tablename = getTablename(entityClass);
    
    Collection<ColField> colFields = columns(entityClass, includeFields, excludeFields, false);
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
      sqlBuf.append(colField.col).append(" = ").append(namedParams ? (":" + colField.field) : "?");
      if(itr.hasNext()) {
        sqlBuf.append(",\n");
      }
    }
    
    sqlCache.put(key, sqlBuf.toString()); //装入缓存
    return sqlBuf.toString();
  }
  
  public Collection<ColField> columns(Class<?> entityClass) {
    return this.columns(entityClass, null, null, false);
  }
  
  public Collection<ColField> columns(Class<?> entityClass, boolean fieldAsAlias) {
    return this.columns(entityClass, null, null, fieldAsAlias);
  }
  
  public Collection<ColField> columnsInclude(Class<?> entityClass, List<String> includeFields) {
    return this.columns(entityClass, includeFields, null, false);
  }
  
  public Collection<ColField> columnsInclude(Class<?> entityClass, List<String> includeFields, boolean fieldAsAlias) {
    return this.columns(entityClass, includeFields, null, fieldAsAlias);
  }
  
  public Collection<ColField> columnsExclude(Class<?> entityClass, List<String> excludeFields) {
    return this.columns(entityClass, null, excludeFields, false);
  }
  
  public Collection<ColField> columnsExclude(Class<?> entityClass, List<String> excludeFields, boolean fieldAsAlias) {
    return this.columns(entityClass, null, excludeFields, fieldAsAlias);
  }
  
  /**
   * 根据给定的实体类，获取对应的列名-字段名列表
   * @param entityClass 给定实体类
   * @param includeFields 必须包含的字段名，不在其内的，不会列出
   * @param excludeFields 不得包含的字段名，在其内的，不会列出
   * @param fieldAsAlias 是否将字段名作为列名的别名
   * @return List of {@link ColField}
   */
  public Collection<ColField> columns(Class<?> entityClass, List<String> includeFields, List<String> excludeFields, boolean fieldAsAlias) {
    String key = colKey(entityClass, includeFields, excludeFields, fieldAsAlias);
    Collection<ColField> colFields = columnCache.get(key);
    if(colFields != null) {
      return colFields;
    }
    
    colFields = getColFields(entityClass, includeFields, excludeFields, fieldAsAlias);
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
   * 获取SQL语句的缓存键值
   * @param entityClass 实体类
   * @param includeFields 包含的字段名
   * @param excludeFields 排除的字段名
   * @param aliasOrNamed 是否需要将属性名作为字段别名（SELECT）, 或者，是否需要命名参数（INSERT or UPDATE）
   * @param type SQL类型
   * @return 缓存Key
   */
  private String sqlKey(Class<?> entityClass, List<String> includeFields, List<String> excludeFields, boolean aliasOrNamed, String type) {
    StringBuilder keyBuilder = new StringBuilder(200).append(type).append(entityClass.getName());
   
    if(!CollectionUtils.isEmpty(includeFields)) {
      keyBuilder.append(Joiner.on("_").join(includeFields));
    }
    if(!CollectionUtils.isEmpty(excludeFields)) {
      keyBuilder.append(Joiner.on("_").join(includeFields));
    }
    keyBuilder.append(aliasOrNamed);
    return DigestUtils.md5DigestAsHex(keyBuilder.toString().getBytes(Charset.forName("UTF-8")));
  }
  
  
  private String colKey(Class<?> entityClass, List<String> includeFields, List<String> excludeFields, boolean fieldAsAlias) {
    StringBuilder keyBuilder = new StringBuilder(200).append(entityClass.getName());
    
    if(!CollectionUtils.isEmpty(includeFields)) {
      keyBuilder.append(Joiner.on("_").join(includeFields));
    }
    if(!CollectionUtils.isEmpty(excludeFields)) {
      keyBuilder.append(Joiner.on("_").join(includeFields));
    }
    keyBuilder.append(fieldAsAlias);
    
    return DigestUtils.md5DigestAsHex(keyBuilder.toString().getBytes(Charset.forName("UTF-8")));
  }
  /**
   * 根据给定的实体类，得到对应的表名
   * @param entityClass 给出实体类
   * @param alias 别名
   * @return
   */
  private String getTablename(Class<?> entityClass, String alias) {
    Entity entity = entityClass.getAnnotation(Entity.class);
    if(entity == null) {
      throw new RuntimeException("实体类必须用@Entity标注");  
    }
    
    String tablename = StringUtils.toSnakeCase(entityClass.getSimpleName());
    Table table = entityClass.getAnnotation(Table.class);
    if(table != null && StringUtils.isNotBlank(table.name())) {
      tablename = table.name();
    }
    
    if(alias != null) {
      tablename += (" " + alias);
    }
    
    return tablename;
  }
  
  public String getTablename(Class<?> entityClass) {
    return getTablename(entityClass, null);
  }
  
  private List<ColField> getColFields(Class<?> entityClass, List<String> includeFields, List<String> excludeFields, boolean fieldAsAlias) {
    PropertyDescriptor[] propertyDescriptors = ReflectUtils.getPropertyDescriptors(entityClass);
    if(propertyDescriptors == null) {
      throw new RuntimeException("无法获取PropertyDescriptor " + entityClass.getName());
    }
    //includeFields 全部转为小写
    if(!CollectionUtils.isEmpty(includeFields)) {
      includeFields.forEach(new Consumer<String>() {
        @Override
        public void accept(String t) {
          if(t != null) t = t.toLowerCase();
        }});
    }
    //excludeFields 全部转为小写
    if(!CollectionUtils.isEmpty(excludeFields)) {
      excludeFields.forEach(new Consumer<String>() {
        @Override
        public void accept(String t) {
          if(t != null) t = t.toLowerCase();
        }});
    }
    
    List<ColField> colFields = new ArrayList<ColField>(propertyDescriptors.length);
    
    for(PropertyDescriptor propertyDescriptor : propertyDescriptors) {
      if(propertyDescriptor == null) {
        continue;
      }
      Method readMethod = propertyDescriptor.getReadMethod();
      if(readMethod == null) {
        continue;
      }
      if(readMethod.getAnnotation(Transient.class) != null || readMethod.getAnnotation(java.beans.Transient.class) != null) {
        continue;
      }
      if(ClassUtils.isAssignable(propertyDescriptor.getPropertyType(), Collection.class)) {
        continue;
      }
      
      String columnName = StringUtils.toSnakeCase(propertyDescriptor.getName()); //EntityBean属性名小写作为字段名
      
      if(!CollectionUtils.isEmpty(includeFields) && !includeFields.contains(columnName)) {
        continue;
      }
      if(!CollectionUtils.isEmpty(excludeFields) && excludeFields.contains(columnName)) {
        continue;
      }
      //有@Column标记
      Column column = readMethod.getAnnotation(Column.class);
      if(column != null && StringUtils.isNotBlank(column.name())) {
        columnName = column.name().toLowerCase();
      }
      //有JoinColumn标记
      JoinColumn joinColumn = readMethod.getAnnotation(JoinColumn.class);
      if(joinColumn != null && StringUtils.isNotBlank(joinColumn.name())) {
        columnName = joinColumn.name().toLowerCase();
      }
      //属性名作为别名
      if(fieldAsAlias) {
        if(joinColumn != null) {
          columnName += (" AS " + propertyDescriptor.getName() + "Id");
        } else {
          columnName += (" AS " + propertyDescriptor.getName());
        }
        
      }
      
      ColField colField = new ColField(columnName, propertyDescriptor.getName(), readMethod.getAnnotation(Id.class) != null);
      colFields.add(colField);
    }
    
    return colFields;
  }
  
  /**
   * 根据类名获取表名别名
   * @param entityClass 给出实体类
   * @return 返回表名别名，别名为实体类类名各个单词首字母小写
   */
  private String getTableAlias(Class<?> entityClass) {
    Iterable<String> iterable = Splitter.on("_").split(StringUtils.toSnakeCase(entityClass.getSimpleName()));
    StringBuilder alias = new StringBuilder(10);
    for(Iterator<String> itr = iterable.iterator(); itr.hasNext();) {
      alias.append(itr.next().charAt(0));
    }
    
    return alias.toString().toLowerCase();
  }
  
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
