package com.github.catstiger.auth.service;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import javax.annotation.Resource;

import org.redisson.api.RMapCache;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.github.catstiger.auth.model.SmsRecord;
import com.github.catstiger.mvc.annotation.API;
import com.github.catstiger.mvc.annotation.Domain;
import com.github.catstiger.mvc.annotation.Param;
import com.github.catstiger.mvc.exception.Exceptions;
import com.github.catstiger.utils.StringUtils;

import lombok.extern.slf4j.Slf4j;

@Service
@Domain("/sms") @Slf4j
public class SmsService {
  public static final String VERIFY_CODE_CACHE_NAME = "verify_code_map_cache";
  /**
   * 验证短信过期时间
   */
  public static final long VERIFY_CODE_TTL = 10L * 60L * 1000L;
  /**
   * 验证短信缓存空闲时间
   */
  public static final long VERIFY_CODE_MAX_IDLE = 10L * 60L * 1000L;
  
  @Value("${alidayu.sign}")
  private String sign;
  
  @Value("${alidayu.verifyCode.templateId}")
  private String verifyTemplateId;
  
  @Resource
  private SmsSender smsSender;
  @Resource
  private RedissonClient redis;
  
  private long verifyCodeTTL = VERIFY_CODE_TTL;
  private long verifyCodeMaxIdle = VERIFY_CODE_MAX_IDLE;
  
  /**
   * 向手机发送一条验证短信，验证码过期时间为10分钟
   * @param mobile 
   * @return
   */
  @API
  public SmsRecord sendVerifyCode(@Param("mobile") String mobile) {
    if(StringUtils.isBlank(mobile)) {
      throw Exceptions.readable("手机号是必须的！");
    }
    
    Map<String, Object> params = new HashMap<>();
    params.put("code", StringUtils.randomNumeric(4));
    SmsRecord record = smsSender.send(mobile, verifyTemplateId, sign, params, SmsSender.SMS_TYPE_VERIFY);
    //如果发送成功，记录验证码
    if(record.getSendSuccess() != null && record.getSendSuccess()) {
      RMapCache<Object, Object> cache = redis.getMapCache(VERIFY_CODE_CACHE_NAME);
      log.debug("缓存验证码 {} {} {}", mobile, params.get("code"), getVerifyCodeTTL());
      cache.put(mobile, params.get("code"), getVerifyCodeTTL(), TimeUnit.MILLISECONDS, getVerifyCodeMaxIdle(), TimeUnit.MILLISECONDS);
    }
    return record;
  }

  /**
   * 如果验证码正确，返回true，并删除验证码， 否则返回False
   * @param mobile 给定手机
   * @param code 给定验证码
   */
  @API
  public Boolean isCorrect(@Param("mobile") String mobile, @Param("code") String code) {
    if(StringUtils.isBlank(mobile)) {
      throw Exceptions.readable("手机号不可为空!");
    }
    
    if(StringUtils.isBlank(code)) {
      throw Exceptions.readable("验证码是必须的！");
    }
    
    RMapCache<Object, Object> cache = redis.getMapCache(VERIFY_CODE_CACHE_NAME);
    Object verifyCode = cache.get(mobile);
    
    return code.equals(verifyCode);
  }

  /**
   * 验证码失效时间，毫秒
   */
  public long getVerifyCodeTTL() {
    return verifyCodeTTL;
  }

  public void setVerifyCodeTTL(long verifyCodeTTL) {
    this.verifyCodeTTL = verifyCodeTTL;
  }
  /**
   * 验证码空闲时间，毫秒
   */
  public long getVerifyCodeMaxIdle() {
    return verifyCodeMaxIdle;
  }

  public void setVerifyCodeMaxIdle(long verifyCodeMaxIdle) {
    this.verifyCodeMaxIdle = verifyCodeMaxIdle;
  }


}
