package com.github.catstiger.auth.service;

import javax.annotation.Resource;

import org.junit.Test;

import com.github.catstiger.auth.service.impl.ali.AlidayuSmsService;
import com.github.catstiger.auth.test.SpringTestCase;

public class AlidayuSmsServiceTest extends SpringTestCase {
  @Resource
  private AlidayuSmsService svr;
  
  @Test
  public void testSendVerify() {
    svr.sendVerifyCode("13930119546");
  }
}
