package com.github.catstiger.core.db.mapper;

import java.beans.PropertyDescriptor;
import java.lang.reflect.Method;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.Table;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.RowMapper;

import com.github.catstiger.core.db.NamingStrategy;
import com.github.catstiger.core.db.SQLRequest;
import com.github.catstiger.utils.ReflectUtils;
/**
 * 一个{@link RowMapper}的实现类，可以将ResultSet中的数据装入一个JavaBean。他具有如下feature:
 * <ul>
 *     <li>支持将ResultSet中的数据装入普通JavaBean</li>
 *     <li>在多表join查询的情况下，会将不同的数据装入相应的实体对象中，前提条件是，这些实体对象必须隶属于某一根对象,即构造函数中传入的{@link #beanClass}</li>
 *     <li>在单表查询的情况下，外键会自动装入引用的实体类的id属性中，要求引用的实体类必须用Entity标注，并且必须有可访问的<code>Long getId()</code>方法。例如：<br>
 *         SELECT u.dept_id from user u,对应的主实体类为User,外键dept_id对应User的属性dept的id，此时，dept_id会自动装入user.getDept().setId(Long id)中。
 *     </li>
 *     <li><b>不</b>支持一对多的情况，也就是说，属性是一个Collection的情况不能装入。</li>
 *     <li><b>不支持</b>重复引用的情况，例如：User中有多个属性指向Dept，则不能正确装入。</li>
 *     <li>支持常用的类型：Integer,Long,Double,Float,String(VARCHAR,CLOB,TEXT), Date, Boolean,后续更多的类型持续加入</li>
 *     <li>扩展点：传入一个{@link RowHandler}的实现，对数据进行更多操作！</li>
 * </ul>
 * @author catstiger
 *
 * @param <T>
 */
public class BeanRowMapper<T> implements RowMapper<T> {
  private static Logger logger = LoggerFactory.getLogger(BeanRowMapper.class);
  
  private Class<T> beanClass;
  private Map<String, ColumnTypesIndex> columnIndexMap = new HashMap<String, ColumnTypesIndex>(20);
  private RowHandler<T> rowHandler;
  private NamingStrategy namingStrategy = SQLRequest.DEFAULT_NAME_STRATEGY;

  /**
   * 构造一个BeanRowMapper的实例，传入要处理的Bean的类型
   * @param beanClass 给出要处理的Bean的类型
   */
  public BeanRowMapper(Class<T> beanClass) {
    this.beanClass = beanClass;
    this.rowHandler = null;
  }
  
  /**
   * 构造一个BeanRowMapper的实例，传入要处理的Bean的类型
   * @param beanClass 给出要处理的Bean的类型
   */
  public BeanRowMapper(Class<T> beanClass, NamingStrategy namingStrategy) {
    this.beanClass = beanClass;
    this.rowHandler = null;
    this.namingStrategy = namingStrategy;
  }
  /**
   * 构造一个BeanRowMapper的实例，使用泛型参数指出要处理的Bean的类型，指出RowHandler扩展点
   * @param rowHandler RowHandler的实例，在处理完一条记录后，可以做更多的操作 
   * @param beanClass 对应的实体类
   */
  public BeanRowMapper(Class<T> beanClass, RowHandler<T> rowHandler) {
    this.beanClass = beanClass;
    this.rowHandler = rowHandler;
  }
  
  public BeanRowMapper(Class<T> beanClass, RowHandler<T> rowHandler, NamingStrategy namingStrategy) {
    this.beanClass = beanClass;
    this.rowHandler = rowHandler;
    this.namingStrategy = namingStrategy;
  }
  
  /**
   * 使用某种命名策略，缺省的，使用驼峰命名法
   * @param namingStrategy NamingStrategy的实例
   */
  public BeanRowMapper<T> withNamingStrategy(NamingStrategy namingStrategy) {
    this.namingStrategy = namingStrategy;
    return this;
  }
  
  /**
   * 设置对应的实体类
   */
  public BeanRowMapper<T> withBeanClass(Class<T> beanClass) {
    this.beanClass = beanClass;
    return this;
  }
  
  /**
   * 设置RowHandler的实例，用于在处理完毕后执行更多的自定义操作。
   */
  public BeanRowMapper<T> withRowHandler(RowHandler<T> rowHandler) {
    this.rowHandler = rowHandler;
    return this;
  }
  
  
  /**
   * 根据{@link beanClass}和ResultSet中的数据，填充Bean
   * @see {@link org.springframework.jdbc.core.RowMapper#mapRow(ResultSet, int)}
   */
  @Override
  public T mapRow(final ResultSet rs, int index) throws SQLException {
    extractColumnIndexMap(rs); //提取字段名称和字段顺序
    
    T bean = ReflectUtils.instantiate(beanClass);
    EntityBeanWalker walker = new EntityBeanWalker();
    walker.walk(bean, new PropertyValueWriter() {
      @Override
      public Object writeProperty(Object owner, PropertyDescriptor propertyDescriptor) {
        String tablename = getTablename(owner.getClass()); //表名
       
        //所有可能的列名
        List<String> alias = new ArrayList<String>(3);
        //属性名或者属性名单词用下划线分割
        alias.add(tablename + "." + namingStrategy.columnLabel(propertyDescriptor));
        //作为外键的列名
        JoinColumn joinColumn = propertyDescriptor.getReadMethod().getAnnotation(JoinColumn.class);
        //根据列名，找到columnIndex
        int columnIndex = 0;
        for(String col : alias) {
          if(columnIndexMap.containsKey(col.toLowerCase())) {
            columnIndex = columnIndexMap.get(col.toLowerCase()).index;
            break;
          }
        }
        
        Object value = null;
        if(columnIndex != 0) {
          if(joinColumn == null) { //写入一般属性
            //调用set方法，写入对应的数据
            value = writeCommonProperty(owner, propertyDescriptor, rs, columnIndex);
          } else { //写入ManyToOne
            value = writeReferenceProperty(owner, propertyDescriptor, rs, columnIndex);
          }
        }
        
        return value;
      }
    });
    //小小的扩展点
    if(rowHandler != null) {
      rowHandler.handle(bean, rs, index);
    }
    
    return bean;
  }
  
  private Object writeReferenceProperty(Object owner, PropertyDescriptor propertyDescriptor, ResultSet rs, int columnIndex) {
    Class<?> propClass = propertyDescriptor.getPropertyType();
    //ManyToOne
    if(propClass.getAnnotation(Table.class) != null || propClass.getAnnotation(Entity.class) != null) { //属性对应一个实体类
      Object refInstance = ReflectUtils.invokeMethod(propertyDescriptor.getReadMethod(), owner); //get这个实体类的实例
      if(refInstance == null) { //为空则创建
        refInstance = ReflectUtils.instantiate(propClass);
      }
      //因为引用的数据总是引用ID字段，因此直接调用setId即可
      try {
        Method setId = propClass.getMethod("setId", Long.class);
        Long id = rs.getLong(columnIndex);
        ReflectUtils.invokeMethod(setId, refInstance, id);
      } catch (Exception e) {
        e.printStackTrace();
      } 
      ReflectUtils.invokeMethod(propertyDescriptor.getWriteMethod(), owner, refInstance);
      
      return refInstance;
    }
    
    return null;
  }
  
  private Object writeCommonProperty(Object owner, PropertyDescriptor propertyDescriptor, ResultSet rs, int columnIndex) {
    Object value = null;
    try {
      
      ResultSetInvoker<?> rsInvoker = ResultSetInvokerFactory.getRSInvoker(propertyDescriptor.getPropertyType());
      if(rsInvoker == null) {
        logger.warn("没有找到合适的RSInvoker实例 {}", propertyDescriptor.getPropertyType());
        return null;
      }
      value = rsInvoker.get(rs, columnIndex);
    } catch (SQLException e) {
      e.printStackTrace();
      return null;
    }
    Method writer = propertyDescriptor.getWriteMethod();
    ReflectUtils.invokeMethod(writer, owner, value);
    
    return value;
  }
  
  /**
   * 根据类名获取表名，表名来自于javax.persistence.Table或者org.hibernate.annotaions.Table标注，如果没有标注，则取自简单类名小写下划线分割。
   * @param beanClass 给出对应的类
   * @return
   */
  private String getTablename(Class<?> beanClass) {
    return namingStrategy.tablename(beanClass);
  }
  
  private void extractColumnIndexMap(ResultSet rs) throws SQLException{
    if(columnIndexMap.isEmpty()) {
      ResultSetMetaData rsMetaData = rs.getMetaData();
     
      int count = rsMetaData.getColumnCount();
      for(int  index = 1; index <= count; index++) {
        //int index = i + 1;
        String table = rsMetaData.getTableName(index).toLowerCase();
        //列名(Label)
        String label = table + "." + namingStrategy.columnLabel(rs, index);
        int columnType = rsMetaData.getColumnType(index);
        columnIndexMap.put(label, new ColumnTypesIndex(label, index, columnType));
      }
    }
  }
  
  /**
   * 字段名，字段类型，字段顺序的对应关系
   * @author catstiger
   *
   */
  @SuppressWarnings("unused")
  private static class ColumnTypesIndex {
    private String column;
    private Integer index;
    private Integer type;
   
    public ColumnTypesIndex(String column, Integer index, Integer type) {
      this.column = column;
      this.index = index;
      this.type = type;
    }

  }

}
