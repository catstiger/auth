package com.github.catstiger.auth.service;

import com.github.catstiger.auth.model.SmsRecord;
/**
 * 短信服务，负责发送短信，验证短信
 * @author leesam
 *
 */
public interface SmsService {
  /**
   * 向某个手机发送一条验证短信
   * @param mobile 手机
   */
  SmsRecord sendVerifyCode(String mobile);
  
  /**
   * 返回最后一条有效的验证码短信
   */
  SmsRecord lastAvailable(String mobile);
  
  /**
   * 验证码是否正确
   * @param mobile 手机
   * @param code 验证码
   */
  void isCorrect(String mobile, String code);
  
  /**
   * 检查所有过期的验证码短信，标记为已经过期
   */
  void checkAll();
}
