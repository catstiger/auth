package com.github.catstiger.core.redis;

import javax.annotation.Resource;

import org.junit.Test;
import org.redisson.api.RQueue;
import org.redisson.api.RedissonClient;

import com.github.catstiger.auth.test.SpringTestCase;

import reactor.core.support.Assert;

public class RedissonTest extends SpringTestCase {
  @Resource
  RedissonClient redis;
  
  @Test
  public void testQueue() {
    System.out.println(-1L ^ (-1L << 5L));
    RQueue<Object> q = redis.getQueue("MY_QUEUE_4");
    Assert.isTrue(!q.isExists());
    
    boolean isSet = q.expireAt(System.currentTimeMillis() + 10 * 1000L);
    System.out.println(isSet);
    for(int i = 0; i < 100; i++) {
      q.add(new Integer(i));
    }
    Assert.isTrue(q.isExists());
    
    Assert.isTrue(q.poll().equals(new Integer(0)));
    for(int i = 1; i < 99; i++) {
      Assert.isTrue(q.poll().equals(new Integer(i)));
    }
    
    Assert.isTrue(q.size() == 1);
    q.poll();
    Assert.isTrue(!q.isExists());
    Assert.isTrue(q.isEmpty());
  }
}
