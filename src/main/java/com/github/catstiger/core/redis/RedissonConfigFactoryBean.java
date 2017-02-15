package com.github.catstiger.core.redis;

import org.redisson.config.Config;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;

public class RedissonConfigFactoryBean implements FactoryBean<Config> {
  private Resource configLocation;
  private Resource yamlLocation;
  private String prefix;

  @Override
  public Config getObject() throws Exception {
    Config config;
    
    if(yamlLocation != null) {
      config = Config.fromYAML(yamlLocation.getInputStream());
    } else if (configLocation != null) {
      config = RedissonConfigUtils.loadConfig(configLocation.getInputStream(), prefix);
    } else {
      config = Config.fromYAML(new ClassPathResource("redisson.yaml").getInputStream());
    }
    
    return config;
  }

  @Override
  public Class<?> getObjectType() {
    return Config.class;
  }

  @Override
  public boolean isSingleton() {
    return false;
  }
  
  public void setConfigLocation(Resource configLocation) {
    this.configLocation = configLocation;
  }

  public void setYamlLocation(Resource yamlLocation) {
    this.yamlLocation = yamlLocation;
  }

  public void setPrefix(String prefix) {
    this.prefix = prefix;
  }


}
