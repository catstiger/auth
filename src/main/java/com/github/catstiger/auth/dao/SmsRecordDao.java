package com.github.catstiger.auth.dao;

import org.beetl.sql.core.annotatoin.Param;
import org.beetl.sql.core.mapper.BaseMapper;

import com.github.catstiger.auth.model.SmsRecord;

public interface SmsRecordDao extends BaseMapper<SmsRecord> {
  /**
   * 将发送给某个手机的验证短信全部设置为失效
   * @param mobile 给出手机号
   */
  public void makeUnavailable(@Param("mobile") String mobile);
}
