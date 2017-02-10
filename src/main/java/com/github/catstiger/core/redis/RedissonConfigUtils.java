package com.github.catstiger.core.redis;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.net.URL;
import java.util.Iterator;
import java.util.Properties;

import org.redisson.client.codec.Codec;
import org.redisson.codec.SnappyCodec;
import org.redisson.config.Config;
import org.redisson.config.SingleServerConfig;
import org.redisson.misc.URLBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cglib.core.ReflectUtils;
import org.springframework.util.ClassUtils;
import org.springframework.util.ReflectionUtils;

import com.github.catstiger.utils.StringUtils;


public final class RedissonConfigUtils {
  private static Logger logger = LoggerFactory.getLogger(RedissonConfigUtils.class);
  
  public static final String KEY_PREFIX_SPRING = "redisson.spring";
  public static final String KEY_PREFIX_HIBERNATE = "redisson.hibernate";
  public static final String DEFAULT_CODEC_CLASS = SnappyCodec.class.getName();
  /**
   * 从Properties文件中加载Config
   * @param in InputStream of properties.
   * @return Instance of {@link Config}
   */
  public static Config loadConfig(InputStream in, String prefix) {
    Properties props = new Properties();
    try {
      props.load(in);
    } catch (IOException e) {
      e.printStackTrace();
    } finally {
      try {
        in.close();
      } catch (IOException e) {
        e.printStackTrace();
      }
    }
    Config config = new Config();
    //设置codec
    Codec codecObject = null;
    if(props.containsKey(prefix + ".codec")) {
      String codec = props.getProperty(prefix + ".codec", DEFAULT_CODEC_CLASS);
      try {
        codecObject = (Codec) ReflectUtils.newInstance(ClassUtils.forName(codec, RedissonConfigUtils.class.getClassLoader()));
      } catch (Exception e) {
        e.printStackTrace();
        throw new RuntimeException(e);
      } 
    }
    if(codecObject != null) {
      config.setCodec(codecObject);
    } else {
      config.setCodec(new SnappyCodec());
    }
    SingleServerConfig ssc = config.useSingleServer();
    //加载其他属性
    for(Iterator<Object> keyIter = props.keySet().iterator(); keyIter.hasNext();) {
      String key = (String) keyIter.next();
      if(!key.startsWith(prefix + ".")) {
        continue;
      }
      String value = props.getProperty(key, "");
      if(StringUtils.isNotBlank(value)) {
        String field = key.substring(prefix.length() + 1);
        //String setter = "set" + StringUtils.upperFirst(field);
        Field property = ReflectionUtils.findField(SingleServerConfig.class, field);
       
        if(property != null) {
          property.setAccessible(true);
          logger.debug("Setting configuration arguments {}, {}", field, value);
          
          Class<?> paramType = property.getType();
          Object v = null;
          if((paramType == long.class || paramType == Long.class) && StringUtils.isNumber(value)) {
            v = Long.valueOf(value);
          } else if((paramType == int.class || paramType == Integer.class) && StringUtils.isNumber(value)) {
            v = Integer.valueOf(value);
          } else if(paramType == String.class) {
            v = value;
          } else if (paramType == URL.class) {
            v = URLBuilder.create(value);
          }
          try {
            property.set(ssc, v);
          } catch (IllegalArgumentException e) {
            e.printStackTrace();
          } catch (IllegalAccessException e) {
            e.printStackTrace();
          }
        }
      }
    }
    
    return config;
  }
  
  private RedissonConfigUtils() {
    
  }
}
