package com.github.catstiger.core.model;

import java.io.Serializable;

import javax.persistence.Id;

import org.beetl.sql.core.annotatoin.AssignID;

public abstract class AbstractModel implements Serializable {
  protected Long id;

  @Id
  @AssignID("workers")
  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }
  
  
}
