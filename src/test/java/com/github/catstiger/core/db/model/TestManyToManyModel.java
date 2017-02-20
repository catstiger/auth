package com.github.catstiger.core.db.model;

import java.util.ArrayList;
import java.util.List;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.ManyToMany;
import javax.persistence.Table;

import com.github.catstiger.core.db.sync.annotation.AutoId;
import com.github.catstiger.core.db.sync.annotation.SyncIgnore;

@Entity
@Table(name = "test_many_to_many_model")
@SyncIgnore
public class TestManyToManyModel {
  private Long id;
  private String name;
  private List<TestDbModel> testDbs = new ArrayList<>();
  
  @Id @AutoId
  public Long getId() {
    return id;
  }
  public void setId(Long id) {
    this.id = id;
  }
  public String getName() {
    return name;
  }
  public void setName(String name) {
    this.name = name;
  }
  
  @ManyToMany(targetEntity = TestManyToManyModel.class)
  public List<TestDbModel> getTestDbs() {
    return testDbs;
  }
  public void setTestDbs(List<TestDbModel> testDbs) {
    this.testDbs = testDbs;
  } 
}
