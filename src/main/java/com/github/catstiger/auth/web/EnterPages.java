package com.github.catstiger.auth.web;

import com.github.catstiger.mvc.annotation.API;
import com.github.catstiger.mvc.annotation.Domain;

@Domain("/")
public class EnterPages {
  @API("index")
  public String index() {
    return "a";
  }
}
