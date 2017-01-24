package com.github.catstiger.auth.cache;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Random;

import javax.annotation.Resource;

import org.junit.Test;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.JdkSerializationRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.util.Assert;

import com.github.catstiger.auth.test.SpringTestCase;
import com.github.catstiger.cache.GenericsJsonRedisSerializer;
import com.github.catstiger.cache.JsonRedisSerializer;
import com.github.catstiger.utils.StringUtils;

@SuppressWarnings("unchecked")
public class RedisCacheTest extends SpringTestCase{
  @Resource
  private RedisTemplate<String, Object> redisTemplate;
  
  @Test
  public void testA() {
    final TestModel model = new TestModel(0L, "惊奇先生", 342.0D, new Date());
    Boolean successed = (Boolean) redisTemplate.execute(new RedisCallback<Boolean>() {
      @Override
      public Boolean doInRedis(RedisConnection connection) throws DataAccessException {   
        final RedisSerializer<String> keySerializer = ((RedisSerializer<String>) redisTemplate.getKeySerializer());
        final RedisSerializer<Object> valueSerializer = (RedisSerializer<Object>) redisTemplate.getValueSerializer();
        final byte[] key = keySerializer.serialize("test.catstiger4");
        byte[] value = valueSerializer.serialize((model));    
        
        connection.setNX(key, value);
        return true;
      }}, true, true);
    
    Assert.isTrue(successed);
    TestModel modelInRedis = (TestModel) redisTemplate.execute(new RedisCallback<TestModel>() {
      @Override
      public TestModel doInRedis(RedisConnection connection) throws DataAccessException {
        final RedisSerializer<String> keySerializer = ((RedisSerializer<String>) redisTemplate.getKeySerializer());
        final RedisSerializer<Object> valueSerializer = (RedisSerializer<Object>) redisTemplate.getValueSerializer();
        final byte[] key = keySerializer.serialize("test.catstiger4");
        byte[] value = connection.get(key);
        Object obj = valueSerializer.deserialize(value);
        System.out.println(obj);
        return (TestModel) obj;
      }
      
    });
    
    Assert.notNull(modelInRedis);
  }
  
  @Test
  public void testB() {
    List<TestModel> list = new ArrayList<TestModel>(200000);
    Random rnd = new Random();
    Date date = new Date();
    for(int i = 0; i < 200000; i++) {
      TestModel model = new TestModel(rnd.nextLong(), StringUtils.random(10) + "中文测试", rnd.nextDouble(), date);
      list.add(model);
    }
    //Jdk Serializer
    JdkSerializationRedisSerializer serializer = new JdkSerializationRedisSerializer();
    long start = System.currentTimeMillis();
    
    for(int i = 0; i < 20 * 10000; i++) {
      TestModel model = list.get(i);
      byte[] value = serializer.serialize(model);
      TestModel tm = (TestModel) serializer.deserialize(value);
      Assert.isTrue(tm != null && tm.getId().equals(model.getId()));
    }
    long end = System.currentTimeMillis();
    
    System.out.println("JDK Serialize: " + Duration.ofMillis(end - start).toString());
    
    //FastJson
    JsonRedisSerializer jsonSerializer = new JsonRedisSerializer();
    start = System.currentTimeMillis();
    
    for(int i = 0; i < 20 * 10000; i++) {
      TestModel model = list.get(i);
      byte[] value = jsonSerializer.serialize(model);
      TestModel tm = (TestModel) jsonSerializer.deserialize(value);
      Assert.isTrue(tm != null && tm.getId().equals(model.getId()));
    }
    end = System.currentTimeMillis();
    
    System.out.println("FastJSON Serialize: " + Duration.ofMillis(end - start).toString());
    
    GenericsJsonRedisSerializer<TestModel> jsSerializer = new GenericsJsonRedisSerializer<TestModel>(TestModel.class);
    start = System.currentTimeMillis();
    
    for(int i = 0; i < 20 * 10000; i++) {
      TestModel model = list.get(i);
      byte[] value = jsSerializer.serialize(model);
      TestModel tm = (TestModel) jsSerializer.deserialize(value);
      Assert.isTrue(tm != null && tm.getId().equals(model.getId()));
    }
    end = System.currentTimeMillis();
    
    System.out.println("FastJSON2 Serialize: " + Duration.ofMillis(end - start).toString());
    
    //Jackson JSON
    Jackson2JsonRedisSerializer<TestModel> jacksonSerializer = new Jackson2JsonRedisSerializer<TestModel>(TestModel.class);
    start = System.currentTimeMillis();
    
    for(int i = 0; i < 20 * 10000; i++) {
      TestModel model = list.get(i);
      byte[] value = jacksonSerializer.serialize(model);
      TestModel tm = jacksonSerializer.deserialize(value);
      Assert.isTrue(tm != null && tm.getId().equals(model.getId()));
    }
    end = System.currentTimeMillis();
    
    System.out.println("Jackson Serialize: " + Duration.ofMillis(end - start).toString());
    
  }
  
  
}
