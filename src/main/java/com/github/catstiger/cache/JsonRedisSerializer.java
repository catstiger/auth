package com.github.catstiger.cache;

import java.nio.charset.Charset;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.data.redis.serializer.SerializationException;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.util.ThreadLocalCache;
import com.github.catstiger.utils.StringUtils;
import com.google.common.base.Strings;

public class JsonRedisSerializer implements RedisSerializer<Object> {
  private static int MAX_LENGTH_CLASSNAME = 120;
  private static int LENGTH_CLASSNAME_SIZE = 4;
  
  private static Map<String, Class<?>> classCache = new ConcurrentHashMap<>(32);
  private Charset charset = Charset.forName("UTF-8");
  
    
  @Override
  public byte[] serialize(Object t) throws SerializationException {
    Assert.notNull(t);
    
    String json = JSON.toJSONString(t);
    return serialize(t.getClass(), json);
  }

  @Override
  public Object deserialize(byte[] bytes) throws SerializationException {
    return parse(bytes);
  }
  
  private Class<?> loadClass(String classname) {
    if(classCache.containsKey(classname)) {
      return classCache.get(classname);
    }
    
    try {
      Class<?> clazz = ClassUtils.forName(classname, getClass().getClassLoader());
      classCache.put(classname, clazz);
      return clazz;
    } catch (ClassNotFoundException e) {
      e.printStackTrace();
      throw new SerializationException(e.getMessage(), e);
    } catch (LinkageError e) {
      e.printStackTrace();
      throw new SerializationException(e.getMessage(), e);
    }
  }
  
  private byte[] serialize(Class<?> clazz, String json) {
    Assert.notNull(clazz);
    
    StringBuilder strBuilder = new StringBuilder(json.length() + MAX_LENGTH_CLASSNAME);
    String classname = clazz.getName();
    strBuilder.append(Strings.padEnd(String.valueOf(classname.length()), LENGTH_CLASSNAME_SIZE, ' '));
    strBuilder.append(classname);
    strBuilder.append(json);
    
    return strBuilder.toString().getBytes(charset);
  }
  
  private Object parse(byte[] bytes) {
    if(bytes == null || bytes.length == 0) {
      return null;
    }
    
    int clznameLen = Integer.parseInt(StringUtils.trim(new String(bytes, 0, LENGTH_CLASSNAME_SIZE, charset)));
    String classname = new String(bytes, LENGTH_CLASSNAME_SIZE, clznameLen, charset);
    Class<?> clazz = loadClass(classname);
    return JSON.parseObject(bytes, LENGTH_CLASSNAME_SIZE + clznameLen, bytes.length - LENGTH_CLASSNAME_SIZE - clznameLen, ThreadLocalCache.getUTF8Decoder(), clazz);
  }
  
  public static void main(String[]args) {
    String str = JsonRedisSerializer.class.getName() + "        ";
    System.out.println(str.length() + "  " + str.getBytes(Charset.forName("UTF-8")).length);
  }
  
}
