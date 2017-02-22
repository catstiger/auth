package com.github.catstiger.core.db.model;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToMany;
import javax.persistence.Table;
import javax.persistence.Transient;

import com.github.catstiger.core.db.sync.annotation.FullMatches;
import com.github.catstiger.core.db.sync.annotation.FullText;
import com.github.catstiger.core.db.sync.annotation.Index;
import com.github.catstiger.core.db.sync.annotation.RangeQuery;
import com.github.catstiger.core.db.sync.annotation.SyncIgnore;
import com.github.catstiger.core.entity.BaseEntity;

import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "test_db_model")
@SyncIgnore
public class TestDbModel extends BaseEntity {

  @Getter @Setter
  @Index @FullMatches
  private String username;
  
  @Getter @Setter @RangeQuery(end = "lastModifiedEnd")
  private Date lastModified;
  
  @Transient @Getter @Setter
  private Date lastModifiedEnd;
  
  @Getter @Setter @RangeQuery(end = "priceEnd", start = "priceStart")
  private Double price;
  
  @Transient @Getter @Setter
  private Double priceStart;
  
  @Transient @Getter @Setter
  private Double priceEnd;
  
  private Float radis;
  
  @Column(length = 100)  @Getter @Setter
  @Index @FullText(relativeColumn = "desc_ft")
  private String descn;
  
  @Transient @Getter @Setter
  private String descFt;
  
  @Getter @Setter
  private String realName;
  
  private TestRefModel refModel;
  
  private List<TestManyToManyModel> m2mModel = new ArrayList<>(); 
  
  @JoinColumn
  public TestRefModel getRefModel() {
    return refModel;
  }

  public void setRefModel(TestRefModel refModel) {
    this.refModel = refModel;
  }

  @Column(precision = 7, scale = 3)
  public Float getRadis() {
    return radis;
  }

  public void setRadis(Float radis) {
    this.radis = radis;
  }

  @ManyToMany(targetEntity = TestManyToManyModel.class, mappedBy = "testDbs")
  public List<TestManyToManyModel> getM2mModel() {
    return m2mModel;
  }

  public void setM2mModel(List<TestManyToManyModel> m2mModel) {
    this.m2mModel = m2mModel;
  }
}
