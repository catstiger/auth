package com.github.catstiger.auth.model;

import java.util.HashSet;
import java.util.Set;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.ManyToMany;
import javax.persistence.Table;

import com.github.catstiger.core.model.AbstractModel;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Entity
@Table(name = "sys_role")
@ToString @EqualsAndHashCode(callSuper = true)
@Data
public class SysRole extends AbstractModel {
  @Column(nullable = false, unique = true)
  private String name;
  private String descn;
  
  @ManyToMany(targetEntity = SysUser.class, mappedBy = "roles")
  private Set<SysUser> users = new HashSet<SysUser>(0);
}
