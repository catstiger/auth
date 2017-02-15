package com.github.catstiger.auth.service;

import java.util.Map;

import com.github.catstiger.auth.model.SmsRecord;

public interface SmsSender {
  /**
   * 发送短信，并返回短信发送的结果
   * @param mobile 目标手机号
   * @param template 短信模板，或者模板ID
   * @param title 短信标题，或者SIGN什么的
   * @param params 短信参数
   * @return Instance of SmsRecord
   */
  public SmsRecord send(String mobile, String template, String title, Map<String, Object> params);
  
}
