package com.github.catstiger.auth.cache;

import java.io.Serializable;
import java.util.Date;

public class TestModel implements Serializable {
  private Long id;
  private String name;
  private Double score;
  private Date birth;

  public TestModel() {
    
  }
  public TestModel(Long id, String name, Double score, Date birth) {
    this.id = id;
    this.name = name;
    this.score = score;
    this.birth = birth;
  }

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

  public Double getScore() {
    return score;
  }

  public void setScore(Double score) {
    this.score = score;
  }

  public Date getBirth() {
    return birth;
  }

  public void setBirth(Date birth) {
    this.birth = birth;
  }

  @Override
  public String toString() {
    return "TestModel [id=" + id + ", name=" + name + ", score=" + score + ", birth=" + birth + "]";
  }
}
