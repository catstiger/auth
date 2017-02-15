package com.github.catstiger.auth.service.impl.ali;

import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.alibaba.fastjson.JSON;
import com.github.catstiger.auth.model.SmsRecord;
import com.github.catstiger.auth.service.SmsSender;
import com.github.catstiger.mvc.exception.Exceptions;
import com.taobao.api.ApiException;
import com.taobao.api.DefaultTaobaoClient;
import com.taobao.api.TaobaoClient;
import com.taobao.api.request.AlibabaAliqinFcSmsNumSendRequest;
import com.taobao.api.response.AlibabaAliqinFcSmsNumSendResponse;

@Service
public class AlidayuSmsSender implements SmsSender {
  @Value("${alidayu.url}")
  private String url;
  
  @Value("${alidayu.appKey}")
  private String appKey;
  
  @Value("${alidayu.appSecret}")
  private String appSecret;
  
  @Override
  public SmsRecord send(String mobile, String template, String title, Map<String, Object> params) {
    TaobaoClient client = new DefaultTaobaoClient(url, appKey, appSecret);
    AlibabaAliqinFcSmsNumSendRequest req = new AlibabaAliqinFcSmsNumSendRequest();
    req.setExtend("verify_code");
    req.setSmsType("normal");
    req.setSmsFreeSignName(title);
    req.setRecNum(mobile);
    if(params != null && !params.isEmpty()) {
      String smsParam = JSON.toJSONString(params);
      req.setSmsParamString(smsParam);
    }
    
    req.setSmsTemplateCode(template);
    try {
      AlibabaAliqinFcSmsNumSendResponse rsp = client.execute(req);
      System.out.println(rsp.getBody());
    } catch (ApiException e) {
      e.printStackTrace();
      throw Exceptions.readable("短信发送失败！");
    }
    return null;
  }

}
