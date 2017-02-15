package com.github.catstiger.core.db.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToMany;
import javax.persistence.Table;

import com.github.catstiger.core.db.annotation.Index;

import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "test_db_model")
public class TestDbModel implements Serializable {

  @Getter(onMethod = @__({@Id, @org.beetl.sql.core.annotatoin.AutoID})) @Setter
  private Long id;
  
  @Getter @Setter
  @Index
  private String username;
  
  @Getter @Setter
  private Date lastModified;
  
  @Getter @Setter
  private Double price;
  
  private Float radis;
  
  @Column(length = 100)
  @Index
  private String descn;
  
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