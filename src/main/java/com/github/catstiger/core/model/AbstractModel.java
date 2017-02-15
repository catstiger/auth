package com.github.catstiger.core.model;

import java.io.Serializable;

import javax.persistence.Id;

import org.beetl.sql.core.annotatoin.AssignID;

import com.github.catstiger.core.db.id.IDAutoGenFactoryBean;

public abstract class AbstractModel implements Serializable {
  protected Long id;

  @Id
  @AssignID(IDAutoGenFactoryBean.ID_GENERATOR_NAME)
  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }
  
  
}
