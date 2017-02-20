package com.github.catstiger.core.db.id;

import javax.annotation.Resource;

import org.redisson.api.RQueue;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

@Component @Slf4j
public class SnowflakeIdGen implements IdGen {
  @Resource
  private RedissonClient redis;
  
  private long workerIdBits = 5L;
  //31
  private long maxWorkerId = -1L ^ (-1L << workerIdBits);
  private long datacenterId = 0L;
  private SnowflakeIDWorker worker = null;
  
  
  @Override
  public Long nextId() {
    if(worker == null) {
      long workerId = getWorkderId(datacenterId);
      worker = new SnowflakeIDWorker(workerId, datacenterId);
    }
    return worker.nextId();
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
