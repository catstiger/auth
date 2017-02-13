package com.github.catstiger.core.db.model;

import java.io.Serializable;
import java.util.Date;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

import org.beetl.sql.core.annotatoin.AutoID;

import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "test_db_model")
public class TestDbModel implements Serializable {
  @Getter(onMethod = @__({@Id, @AutoID})) @Setter
  private Long id;
  
  @Getter @Setter
  private String username;
  
  @Getter @Setter
  private Date lastModified;
}
