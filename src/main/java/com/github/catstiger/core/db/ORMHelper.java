package com.github.catstiger.core.db;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.ManyToMany;
import javax.persistence.Transient;

import org.springframework.util.ReflectionUtils;

import com.github.catstiger.core.db.sync.annotation.SyncIgnore;
import com.github.catstiger.utils.ClassUtils;
import com.github.catstiger.utils.ReflectUtils;
import com.github.catstiger.utils.StringUtils;
import com.google.common.base.Splitter;

import lombok.NonNull;

public final class ORMHelper {
  private static Map<String, ORMHelper> instances = new ConcurrentHashMap<>(5);
  
  private NamingStrategy namingStrategy;
  
  private ORMHelper() {
    namingStrategy = SQLFactory.DEFAULT_NAME_STRATEGY;
  }
  
  private ORMHelper(NamingStrategy namingStrategy) {
    this.namingStrategy = namingStrategy;
  }
  
  public static ORMHelper getInstance() {
    String key = SQLFactory.DEFAULT_NAME_STRATEGY.getClass().getSimpleName();
    if(!instances.containsKey(key)) {
      ORMHelper instance = new ORMHelper();
      instances.put(key, instance);
    }
    
    return instances.get(key);
  }
  
  public static ORMHelper getInstance(NamingStrategy namingStrategy) {
    if(namingStrategy == null) {
      return getInstance();
    }
    String key = namingStrategy.getClass().getSimpleName();
    if(!instances.containsKey(key)) {
      ORMHelper instance = new ORMHelper(namingStrategy);
      instances.put(key, instance);
    }
    
    return instances.get(key);
  }
  
  
  /**
   * 判断给定的Class是否是一个实体类
   */
  public Boolean isEntity(Class<?> entityClass) {
    if(entityClass == null) {
      return false;
    }
    
    return (entityClass.getAnnotation(Entity.class) != null) && (entityClass.getAnnotation(SyncIgnore.class) == null);
  }
  
  /**
   * 判断一个实体类是否被数据同步忽略，被SyncIgnore标注的类会被忽略
   */
  public Boolean isEntityIgnore(Class<?> entityClass) {
    if(entityClass == null) {
      return true;
    }
    
    return entityClass.getAnnotation(SyncIgnore.class) != null;
  }
  
  /**
   * 根据实体类，得到对应的表名：
   * <ul>
   *     <li>如果实体类被@javax.persistence.Table标注，并且进行命名，则取@Table命名</li>
   *     <li>如果实体类被@org.beetl.sql.core.annotatoin.Table标注，并且进行命名，则取@Table的命名</li>
   *     <li>否则，取类名小写，单词之间用下划线分割作为表名</li>
   * </ul>
   * @param entityClass 给出实体类
   * @return 表名
   */
  public String tableNameByEntity(@NonNull Class<?> entityClass) {
    return namingStrategy.tablename(entityClass);
  }
  
  /**
   * 根据实体类的属性，获取字段名：
   * <ul>
   *     <li>如果属性被@javax.persistence.Column或者@javax.persistence.JoinColumn标注，并且进行命名，则取其作为字段名</li>
   *     <li>否则，取字段名小写，单词之间用下划线分割作为字段名</li>
   * </ul>
   * @param entityClass 实体类
   * @param fieldName 属性名
   * @return 字段名
   */
  public String columnNameByField(@NonNull Class<?> entityClass, @NonNull String fieldName) {
    return namingStrategy.columnName(entityClass, fieldName);
  }
  /**
   * 根据实体类，和field，获取对应的GETTER方法。被如下Annotation标注的字段或者对应的Getter方法，会被忽略
   * <ul>
   *     <li>javax.persistence.Transient</li>
   *     <li>java.beans.Transient</li>
   *     <li>javax.persistence.ManyToMany</li>
   * </ul>
   * @param entityClass 实体类
   * @param fieldName 属性名
   * @return Getter Meth
   */
  public Method getAccessMethod(@NonNull Class<?> entityClass, @NonNull String fieldName) {
    Method getter = ReflectionUtils.findMethod(entityClass, "get" + StringUtils.upperFirst(fieldName));
    return getter;
  }
  
  public Method getAccessMethod(@NonNull Field field) {
    return getAccessMethod(field.getDeclaringClass(), field.getName());
  }
  
  /**
   * 字段是否被忽略
   * @param field
   * @return
   */
  public Boolean isFieldIgnore(Field field) {
    javax.persistence.Transient transientAnn = field.getAnnotation(javax.persistence.Transient.class);
    if(transientAnn != null) {
      return true;
    }
    
    java.beans.Transient trAnn = field.getAnnotation(java.beans.Transient.class);
    if(trAnn != null) {
      return true;
    }
    
    Method getter = getAccessMethod(field.getDeclaringClass(), field.getName());
    if(getter == null) {
      return true;
    }
    transientAnn = getter.getAnnotation(javax.persistence.Transient.class);
    if(transientAnn != null) {
      return true;
    }
    
    trAnn = getter.getAnnotation(java.beans.Transient.class);
    if(trAnn != null) {
      return true;
    }
    
    ManyToMany m2m = field.getAnnotation(ManyToMany.class);
    if(m2m != null) {
      return true;
    }
    m2m = getter.getAnnotation(ManyToMany.class);
    if(m2m != null) {
      return true;
    }
    
    return false;
  }
  
  /**
   * 属性是否被忽略
   */
  public Boolean isFieldIgnore(Class<?> entityClass, String fieldName) {
    try {
      Field field = ReflectUtils.findField(entityClass, fieldName);
      return isFieldIgnore(field);
    } catch (Exception e) {
      e.printStackTrace();
    }
    
    return false;
  }
  
  /**
   * 判断字段是否在UPDATE，INSERT中被忽略，标注为Transient的字段，集合类或者数组
   * @param field
   * @return
   */
  public Boolean isUpdateIgnore(Field field) {
     //如果标注为Transient,则忽略
    if(field.getAnnotation(Transient.class) != null || field.getAnnotation(java.beans.Transient.class) != null) {
      return true;
    }
    //如果是集合类或者数组，则忽略
    if(ClassUtils.isAssignable(field.getType(), Collection.class) || field.getType().isArray()) {
      return true;
    }
    
    Method getter = this.getAccessMethod(field);
    if(getter.getAnnotation(Transient.class) != null || getter.getAnnotation(java.beans.Transient.class) != null) {
      return true;
    }
    return false;
  }
  
  public Boolean isUpdateIgnore(Class<?> entityClass, String fieldName) {
    try {
      Field field = ReflectUtils.findField(entityClass, fieldName);
      return isUpdateIgnore(field);
    } catch (Exception e) {
      e.printStackTrace();
    }
    
    return false;
  }
  
  
  /**
   * 判断一个属性是否是主键
   */
  public Boolean isPrimaryKey(Field field) {
    if(isFieldIgnore(field)) {
      return false;
    }
    
    Id idAnn = field.getAnnotation(Id.class);
    if(idAnn != null) {
      return true;
    }
    
    Method getter = this.getAccessMethod(field);
    idAnn = getter.getAnnotation(Id.class);
    if(idAnn != null) {
      return true;
    }
    
    if("id".equals(field.getName()) && Long.class == field.getType()) {
      return true;
    }
    return false;
  }
  
  
  
  /**
   * 返回缩写
   */
  public String simpleName(@NonNull String name) {
    StringBuilder strBuilder = new StringBuilder();
    Splitter.on("_").split(name).forEach(new Consumer<String>() {
      @Override
      public void accept(String t) {
        if(t != null && t.length() > 1) {
          strBuilder.append(t.charAt(0));
        }
      }
    });
    
    return strBuilder.toString();
  }
}
