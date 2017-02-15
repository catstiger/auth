package com.github.catstiger.auth.service.impl.ali;

import java.util.Date;
import java.util.Map;

import javax.annotation.Resource;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.alibaba.fastjson.JSON;
import com.github.catstiger.auth.dao.SmsRecordDao;
import com.github.catstiger.auth.dao.SysUserDao;
import com.github.catstiger.auth.model.SmsRecord;
import com.github.catstiger.auth.model.SysUser;
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
  
  @Resource
  private SysUserDao sysUserDao;
  @Resource
  private SmsRecordDao smsRecDao;
  
  @Override
  @Transactional
  public SmsRecord send(String mobile, String template, String title, Map<String, Object> params) {
    TaobaoClient client = new DefaultTaobaoClient(url, appKey, appSecret);
    //准备发送短信请求对象
    AlibabaAliqinFcSmsNumSendRequest req = new AlibabaAliqinFcSmsNumSendRequest();
    req.setExtend("verify_code");
    req.setSmsType("normal");
    req.setSmsFreeSignName(title); //大于签名
    req.setRecNum(mobile);
    //短信参数
    String smsParam = null;
    if(params != null && !params.isEmpty()) {
      smsParam = JSON.toJSONString(params);
      req.setSmsParamString(smsParam);
    }
    req.setSmsTemplateCode(template);
    //创建短信记录
    SmsRecord smsRec = new SmsRecord();
    SysUser sender = sysUserDao.byMobile(mobile);
    if(sender != null) {
      smsRec.setUserId(sender.getId());
      smsRec.setUsername(sender.getUsername());
    }
    if(params != null && params.containsKey("code")) {
      smsRec.setCode((String) params.get("code"));
    }
    smsRec.setSendTime(new Date());
    
    try {
      AlibabaAliqinFcSmsNumSendResponse rsp = client.execute(req);

      smsRec.setIsAvailable(true);
      smsRec.setBody(rsp.getBody());
      smsRec.setSendSuccess(rsp.isSuccess());
      smsRec.setReturnCode(rsp.getErrorCode());
      
    } catch (ApiException e) {
      e.printStackTrace();
      smsRec.setIsAvailable(false);
      smsRec.setReturnCode(e.getErrCode());
      smsRec.setSendSuccess(false);
      smsRec.setBody(e.getErrMsg());
      throw Exceptions.readable("短信发送失败！");
    }
    smsRecDao.insertTemplate(smsRec);
    return smsRec;
  }

}
