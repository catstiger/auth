package com.github.catstiger.core.db.id;

import javax.annotation.Resource;

import org.beetl.sql.core.IDAutoGen;
import org.beetl.sql.ext.SnowflakeIDWorker;
import org.redisson.api.RQueue;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.FactoryBean;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class IDAutoGenFactoryBean implements FactoryBean<IDAutoGen<Long>> {
  public static final String ID_GENERATOR_NAME = "workers";
  
  @Resource
  private RedissonClient redis;
  
  private long workerIdBits = 5L;
  //31
  private long maxWorkerId = -1L ^ (-1L << workerIdBits);
  private long datacenterId = 0L;

  @Override
  public IDAutoGen<Long> getObject() throws Exception {
    long workerId = getWorkderId(datacenterId);
    
    return new IDAutoGen<Long>() {
      SnowflakeIDWorker worker = new SnowflakeIDWorker(workerId, datacenterId);
      @Override
      public Long nextID(String params) {
        return worker.nextId();
      }
      
    };
  }

  @Override
  public Class<?> getObjectType() {
    return IDAutoGen.class;
  }

  @Override
  public boolean isSingleton() {
    return true;
  }
  
  private long getWorkderId(long datacenterId) {
    String key = "auto_id_worker_" + datacenterId;
    RQueue<Object> queue = redis.getQueue(key);
    
    if(!queue.isExists() || queue.isEmpty()) {
      for(long i = 1L; i <= maxWorkerId; i++) {
        queue.add(new Long(i));
      }
    }
    
    Long workerId = (Long) queue.poll();
    log.info("Redis生成WorkerID {}", workerId);
    return workerId;
  }

  public void setDatacenterId(long datacenterId) {
    this.datacenterId = datacenterId;
  }

}
