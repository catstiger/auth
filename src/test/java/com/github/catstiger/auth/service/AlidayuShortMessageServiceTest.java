package com.github.catstiger.auth.service;

import javax.annotation.Resource;

import org.junit.Test;

import com.github.catstiger.auth.service.impl.ali.AlidayuShortMessageService;
import com.github.catstiger.auth.test.SpringTestCase;

public class AlidayuShortMessageServiceTest extends SpringTestCase {
  @Resource
  private AlidayuShortMessageService svr;
  
  @Test
  public void testSendVerify() {
    svr.sendVerifyCode("13930119546");
  }
}
