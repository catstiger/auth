package com.github.catstiger.auth.dao;

import javax.annotation.Resource;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.github.catstiger.auth.model.SmsRecord;
import com.github.catstiger.core.db.SQLFactory;
import com.github.catstiger.core.db.SQLFactory.SQLReady;
import com.github.catstiger.core.db.SQLFactory.SQLRequest;
import com.github.catstiger.core.db.id.IdGen;

import reactor.core.support.Assert;

@Repository
public class SmsRecordDao {
  @Resource
  private JdbcTemplate jdbcTemplate;
  @Resource
  private IdGen idGen;
  
  
  /**
   * 将发送给某个手机的验证短信全部设置为失效
   * @param mobile 给出手机号
   */
  @Transactional
  public void makeUnavailable(String mobile) {
    Assert.notNull(mobile, "手机号不可为空。");
    
    jdbcTemplate.update("update sms_record set is_available=false where mobile=?", mobile);
  }
  
  @Transactional
  public SmsRecord insert(SmsRecord entity) {
    entity.setId(idGen.nextId());
    SQLReady sqlReady = SQLFactory.getInstance().insertDynamic(new SQLRequest(entity).usingAlias(false));
    jdbcTemplate.update(sqlReady.getSql(), sqlReady.getArgs());
    
    return entity;
  }
}
