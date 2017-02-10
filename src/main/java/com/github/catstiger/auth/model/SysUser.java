package com.github.catstiger.auth.model;

import java.io.Serializable;
import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;

import org.beetl.sql.core.annotatoin.AssignID;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Entity
@Table(name = "sys_user")
@ToString @EqualsAndHashCode()
public class SysUser implements Serializable {
  @Getter(onMethod = @__({@AssignID("simple")}))  @Setter
  private Long id;
  
  @Getter  @Setter @Column(length = 32, unique = true, nullable = false)
  private String username;
  
  @Getter  @Setter
  @Column(length = 128)
  private String password;
  
  @Getter  @Setter
  @Column(length = 32, unique = true)
  private String mobile;
  
  @Getter  @Setter
  @Column(length = 64, unique = true)
  private String email;
  
  @Getter  @Setter
  private String company;
  
  @Getter @Setter
  @Column(name = "real_name", length = 64)
  private String realName;
  
  @Getter @Setter
  private Boolean isAvailable = true;
  
  @Getter @Setter
  @Column(name = "regist_time")
  private Date registTime;
  
}


