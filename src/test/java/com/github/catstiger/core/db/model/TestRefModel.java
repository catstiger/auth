package com.github.catstiger.core.db.model;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Lob;
import javax.persistence.Table;
import javax.persistence.Transient;

import org.beetl.sql.core.annotatoin.AutoID;

import com.github.catstiger.core.db.annotation.Index;
import com.github.catstiger.core.db.annotation.SyncIgnore;

@Entity
@Table(name = "test_ref_model")
@SyncIgnore
public class TestRefModel {
  
  private Long id;
  private String title;
  private String descn;
  private String ig;
  
  
  @Id @AutoID
  public Long getId() {
    return id;
  }
  public void setId(Long id) {
    this.id = id;
  }
  
  @Column(length = 50)
  @Index(name = "idx_test_ref_model_title_a", unique = true)
  public String getTitle() {
    return title;
  }
  public void setTitle(String title) {
    this.title = title;
  }
  
  @Lob
  public String getDescn() {
    return descn;
  }
  
  public void setDescn(String descn) {
    this.descn = descn;
  }
  
  @Transient
  public String getIg() {
    return ig;
  }
  
  public void setIg(String ig) {
    this.ig = ig;
  }
  
}
