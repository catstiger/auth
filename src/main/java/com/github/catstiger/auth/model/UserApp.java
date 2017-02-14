package com.github.catstiger.auth.model;

import java.util.ArrayList;
import java.util.List;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToMany;
import javax.persistence.Table;

import com.github.catstiger.core.model.AbstractModel;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Entity
@Table(name = "user_app")
@ToString @EqualsAndHashCode(callSuper = true)
@Data
public class UserApp extends AbstractModel {
  @Column(nullable = false)
  private String name;
  
  @Column(length = 500)
  private String descn;
  
  private Boolean isAvailable = true;
  
  @JoinColumn(name = "owner_id")
  private SysUser owner;
  
  
  @ManyToMany(targetEntity = SysUser.class, mappedBy = "userApps")
  private List<SysUser> managers = new ArrayList<>(0);
  
}
