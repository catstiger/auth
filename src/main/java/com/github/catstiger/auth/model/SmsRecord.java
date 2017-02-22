package com.github.catstiger.auth.model;

import java.util.Date;

import javax.persistence.Entity;
import javax.persistence.Table;

import com.github.catstiger.core.db.annotation.Index;
import com.github.catstiger.core.entity.BaseEntity;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Entity
@Table(name = "sms_record")
@Data @ToString @EqualsAndHashCode(callSuper = true)
public class SmsRecord extends BaseEntity {
  public static final String SHORT_MSG_VERIFY_CODE = "VERIFY_CODE";
  /**
   * 用户ID
   */
  @Index
  private Long userId;
  /**
   * 用户名
   */
  @Index
  private String username;
  /**
   * 手机
   */
  @Index
  private String mobile;
  /**
   * 模板ID
   */
  private String templateId;
  /**
   * 验证码，仅验证码短信有效
   */
  private String code;
  /**
   * 短信内容
   */
  private String body;
  /**
   * 短信类型
   */
  private String type;
  /**
   * 发送时间
   */
  private Date sendTime;
  /**
   * 返回码，由服务商提供
   */
  private String returnCode;
  /**
   * 验证码是否有效
   */
  @Index
  private Boolean isAvailable = true;
  /**
   * 是否发送成功
   */
  private Boolean sendSuccess = true;
}
