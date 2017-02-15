package com.github.catstiger.core.redis;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;

import org.redisson.api.RedissonClient;
import org.redisson.spring.cache.CacheConfig;
import org.redisson.spring.cache.RedissonSpringCacheManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.cache.CacheManager;
import org.springframework.core.io.Resource;

import com.github.catstiger.utils.StringUtils;
import com.google.common.base.Splitter;

public class RedissonCacheManagerFactoryBean implements FactoryBean<CacheManager> {
  private static Logger logger = LoggerFactory.getLogger(RedissonCacheManagerFactoryBean.class);
  public static final String DEFAULT_CACHENAME_PREFIX = "expiry";
  
  private RedissonClient redissonClient;
  private Resource cacheConfig;
  private String cacheNamePrefx = DEFAULT_CACHENAME_PREFIX;

  @Override
  public CacheManager getObject() throws Exception {
    RedissonSpringCacheManager rscm = new RedissonSpringCacheManager(redissonClient);
    //加载Cache配置（不同name的cache的过期时间）
    Map<String, CacheConfig> configs = new HashMap<String, CacheConfig>();
    if(cacheConfig == null) {
      configs.put("common", new CacheConfig(120000, 60000));
    } else {
      //加载配置文件
      Properties props = new Properties();
      InputStream in = cacheConfig.getInputStream();
      try {
        props.load(cacheConfig.getInputStream());
      } finally {
        in.close();
      }
      //读取加载项
      for(Iterator<Object> itr = props.keySet().iterator(); itr.hasNext();) {
        String key = (String) itr.next();
        if(!key.startsWith(cacheNamePrefx + ".")) {
          continue;
        }
        String value = props.getProperty(key);
        if(StringUtils.isBlank(value)) {
          continue;
        }
        long ttl = 0L;
        long maxIdleTime = 0L;
        if(value.contains(",")) {
          String[] cfg = Splitter.on(",").trimResults().splitToList(value).toArray(new String[]{});
          if(StringUtils.isNumber(cfg[0])) {
            ttl = Long.valueOf(cfg[0]);
          }
          if(StringUtils.isNumber(cfg[1])) {
            maxIdleTime = Long.valueOf(cfg[1]);
          }
        } else {
          if(StringUtils.isNumber(value)) {
            ttl = Long.valueOf(value);
          }
        }
        CacheConfig cacheConfig = new CacheConfig(ttl, maxIdleTime);
        String name = key.substring(cacheNamePrefx.length() + 1);
        logger.debug("Add cacheConfig for {}, {}, {}", name, cacheConfig.getTTL(), cacheConfig.getMaxIdleTime());
        configs.put(name, cacheConfig);
      }
      
    }
    
    rscm.setConfig(configs);
    
    return rscm;
  }

  @Override
  public Class<?> getObjectType() {
    return CacheManager.class;
  }

  @Override
  public boolean isSingleton() {
    return true;
  }

  public void setRedissonClient(RedissonClient redissonClient) {
    this.redissonClient = redissonClient;
  }


  public void setCacheConfig(Resource cacheConfig) {
    this.cacheConfig = cacheConfig;
  }

  public void setCacheNamePrefx(String cacheNamePrefx) {
    this.cacheNamePrefx = cacheNamePrefx;
  }


}
