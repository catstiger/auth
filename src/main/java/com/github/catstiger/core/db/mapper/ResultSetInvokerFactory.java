package com.github.catstiger.core.db.mapper;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.github.catstiger.core.db.mapper.rsi.BigDecimalRSInvoker;
import com.github.catstiger.core.db.mapper.rsi.BooleanRSInvoker;
import com.github.catstiger.core.db.mapper.rsi.DateRSInvoker;
import com.github.catstiger.core.db.mapper.rsi.DoubleRSInvoker;
import com.github.catstiger.core.db.mapper.rsi.FloatRSInvoker;
import com.github.catstiger.core.db.mapper.rsi.IntegerRSInvoker;
import com.github.catstiger.core.db.mapper.rsi.LongRSInvoker;
import com.github.catstiger.core.db.mapper.rsi.StringRSInvoker;
import com.github.catstiger.core.db.mapper.rsi.TimestampRSInvoker;

abstract class ResultSetInvokerFactory {
  @SuppressWarnings("rawtypes")
  private static Map<Class<?>, ResultSetInvoker> invokers = new ConcurrentHashMap<Class<?>, ResultSetInvoker>();
  
  static {
    invokers.put(Integer.class, new IntegerRSInvoker());
    invokers.put(Double.class, new DoubleRSInvoker());
    invokers.put(Float.class, new FloatRSInvoker());
    invokers.put(Long.class, new LongRSInvoker());
    invokers.put(int.class, new IntegerRSInvoker());
    invokers.put(double.class, new DoubleRSInvoker());
    invokers.put(float.class, new FloatRSInvoker());
    invokers.put(long.class, new LongRSInvoker());
    
    invokers.put(Date.class, new DateRSInvoker());
    invokers.put(Timestamp.class, new TimestampRSInvoker());
    invokers.put(BigDecimal.class, new BigDecimalRSInvoker());
    invokers.put(Boolean.class, new BooleanRSInvoker());
    
    invokers.put(String.class, new StringRSInvoker());
  }
  
  /**
   * 根据给定的类型，返回合适的RSInvoker的实例，如果没有，则返回null
   * @param type 给出类型
   * @return
   */
  @SuppressWarnings("unchecked")
  static <T> ResultSetInvoker<T> getRSInvoker(Class<T> type) {
    if(invokers.containsKey(type)) {
      return invokers.get(type);
    }
    return null;
  }
}
