package com.github.catstiger.auth.model;

import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.ManyToMany;
import javax.persistence.Table;

import com.github.catstiger.core.db.annotation.Index;
import com.github.catstiger.core.entity.BaseEntity;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Entity
@Table(name = "sys_user")
@ToString @EqualsAndHashCode(callSuper = true)
@Data
public class SysUser extends BaseEntity {
  
  @Column(length = 32, unique = true, nullable = false)
  @Index(unique = true)
  private String username;
  
  @Column(length = 128)
  private String password;
  
  @Column(length = 32, unique = true)
  @Index(unique = true)
  private String mobile;
  
  @Column(length = 64, unique = true)
  private String email;
  
  private String company;
  
  @Column(name = "real_name", length = 64)
  private String realName;
  
  @Index
  private Boolean isAvailable = true;
  
  @Column(name = "regist_time")
  private Date registTime;
  
  @ManyToMany(targetEntity = SysRole.class)
  private Set<SysRole> roles = new HashSet<SysRole>(0);

}


