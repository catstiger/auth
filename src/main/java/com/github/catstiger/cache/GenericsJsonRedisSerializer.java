package com.github.catstiger.cache;

import java.nio.charset.Charset;

import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.data.redis.serializer.SerializationException;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.util.ThreadLocalCache;

public class GenericsJsonRedisSerializer<T> implements RedisSerializer<T> {

  public static final Charset DEFAULT_CHARSET = Charset.forName("UTF-8");
  
  static final byte[] EMPTY_ARRAY = new byte[0];

  private final Class<T> type;

  /**
   * Creates a new {@link GenericsJsonRedisSerializer} for the given target {@link Class}.
   * 
   * @param type
   */
  public GenericsJsonRedisSerializer(Class<T> type) {
    this.type = type;
  }

  public T deserialize(byte[] bytes) throws SerializationException {
    if (bytes == null || bytes.length == 0) {
      return null;
    }
    return JSON.parseObject(bytes, 0, bytes.length, ThreadLocalCache.getUTF8Decoder(), type);
  }

  public byte[] serialize(Object t) throws SerializationException {
    if (t == null) {
      return EMPTY_ARRAY;
    }
    return JSON.toJSONBytes(t);
  } 
}
