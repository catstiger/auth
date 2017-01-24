package com.github.catstiger.cache;

import java.nio.charset.Charset;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.data.redis.serializer.SerializationException;
import org.springframework.util.ClassUtils;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.util.ThreadLocalCache;
import com.github.catstiger.utils.StringUtils;
import com.google.common.base.Strings;

public class JsonRedisSerializer implements RedisSerializer<Object> {
  private static int LENGTH_CLASSNAME_SIZE = 4;
  
  private static Map<String, Class<?>> classCache = new ConcurrentHashMap<>(32);
  private Charset charset = Charset.forName("UTF-8");
  
    
  @Override
  public byte[] serialize(Object t) throws SerializationException {
    if(t == null) {
      return GenericsJsonRedisSerializer.EMPTY_ARRAY;
    }
    String json = JSON.toJSONString(t);
    
    String classname = t.getClass().getName();
    StringBuilder strBuilder = new StringBuilder(json.length() + LENGTH_CLASSNAME_SIZE + classname.length());
    strBuilder.append(Strings.padEnd(String.valueOf(classname.length()), LENGTH_CLASSNAME_SIZE, ' '));
    strBuilder.append(classname);
    strBuilder.append(json);
    
    return strBuilder.toString().getBytes(charset);
  }

  @Override
  public Object deserialize(byte[] bytes) throws SerializationException {
    if(bytes == null || bytes.length == 0) {
      return null;
    }
    
    int clznameLen = Integer.parseInt(StringUtils.trim(new String(bytes, 0, LENGTH_CLASSNAME_SIZE, charset)));
    String classname = new String(bytes, LENGTH_CLASSNAME_SIZE, clznameLen, charset);
    Class<?> clazz = loadClass(classname);
    return JSON.parseObject(bytes, LENGTH_CLASSNAME_SIZE + clznameLen, bytes.length - LENGTH_CLASSNAME_SIZE - clznameLen, ThreadLocalCache.getUTF8Decoder(), clazz);
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
  
 
}
