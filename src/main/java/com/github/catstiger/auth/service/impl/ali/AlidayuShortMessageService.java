package com.github.catstiger.auth.service.impl.ali;

import java.util.HashMap;
import java.util.Map;

import javax.annotation.Resource;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.github.catstiger.auth.model.SmsRecord;
import com.github.catstiger.auth.service.ShortMessageService;
import com.github.catstiger.auth.service.SmsSender;
import com.github.catstiger.mvc.annotation.API;
import com.github.catstiger.mvc.annotation.Domain;
import com.github.catstiger.mvc.annotation.Param;
import com.github.catstiger.mvc.exception.Exceptions;
import com.github.catstiger.utils.StringUtils;

@Service
@Domain("/sms")
public class AlidayuShortMessageService implements ShortMessageService {
  
  @Value("${alidayu.sign}")
  private String sign;
  
  @Value("${alidayu.verifyCode.templateId}")
  private String verifyTemplateId;
  
  @Resource
  private SmsSender smsSender;
  
  @Override
  @API
  public SmsRecord sendVerifyCode(@Param("mobile") String mobile) {
    if(StringUtils.isBlank(mobile)) {
      throw Exceptions.readable("手机号是必须的！");
    }
    
    Map<String, Object> params = new HashMap<>();
    params.put("code", StringUtils.randomNumeric(4));
    SmsRecord record = smsSender.send(mobile, verifyTemplateId, sign, params);
    
    return record;
  }

  @Override
  public SmsRecord lastAvailable(String mobile) {
    
    return null;
  }

  @Override
  @API
  public void isCorrect(@Param("mobile") String mobile, @Param("code") String code) {
    

  }

  @Override
  public void checkAll() {
    

  }

}
