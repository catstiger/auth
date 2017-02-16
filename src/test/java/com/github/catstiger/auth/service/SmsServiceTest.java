package com.github.catstiger.auth.service;

import javax.annotation.Resource;

import org.junit.Test;

import com.github.catstiger.auth.model.SmsRecord;
import com.github.catstiger.auth.test.SpringTestCase;

import reactor.core.support.Assert;

public class SmsServiceTest extends SpringTestCase {
  @Resource
  private SmsService svr;
  
  @Test
  public void testSendVerify() {
    svr.setVerifyCodeTTL(10 * 1000);
    SmsRecord smsRec = svr.sendVerifyCode("13930119546");
    
    
    Assert.isTrue(smsRec.getSendSuccess() != null && smsRec.getSendSuccess());
    Assert.isTrue(svr.isCorrect(smsRec.getMobile(), smsRec.getCode()));
    Assert.isTrue(!svr.isCorrect(smsRec.getMobile(), smsRec.getCode()));
    
  }
  
  @Test
  public void testSendVerify2() throws Exception {
    svr.setVerifyCodeTTL(10L * 1000L);
    svr.setVerifyCodeTTL(10L * 1000L);
    SmsRecord smsRec = svr.sendVerifyCode("13930119546");
    
    Assert.isTrue(smsRec.getSendSuccess() != null && smsRec.getSendSuccess());
    Thread.sleep(12 * 1000);
    Assert.isTrue(!svr.isCorrect(smsRec.getMobile(), smsRec.getCode()));
    
  }
}
